import { bearerToken, issueSessionToken, verifyJudgeCode, verifySessionToken } from "./auth";
import { getConfig, hasSecureJudgeCode, hasUsableApiKey } from "./config";
import { ApiError, errorResponse } from "./errors";
import {
  allowedOrigin,
  clientKey,
  corsHeaders,
  json,
  readJson,
  requestIdFor,
  withSecurityHeaders
} from "./http";
import {
  createLocalUploadTickets,
  serveLocalMedia,
  signedMediaUrl,
  storeLocalUpload
} from "./image-store";
import {
  acceptPaidOperation,
  admitPaidOperation,
  fingerprintPaidRequest,
  markPaidOperationUnknown,
  paidBudgetStatus,
  paidOperationStatus,
  rejectPaidOperation,
  validateOperationId,
  type PaidOperationResult
} from "./paid-operations";
import { enforceRateLimit } from "./rate-limit";
import type { ApiFeature, Env, Fetcher } from "./types";
import {
  facialTaskSchema,
  parseWith,
  sessionSchema,
  uploadSchema,
  validateTaskId,
  clothesVtoTaskSchema,
  scarfVtoTaskSchema,
  validateAllowedImageUrl
} from "./validation";
import { normalizeTaskResult, YouCamClient } from "./youcam";

const API_VERSION = "0.2.0";

export function createHandler(upstreamFetch: Fetcher = fetch) {
  return async function handle(request: Request, env: Env): Promise<Response> {
    const requestId = requestIdFor(request);
    const config = getConfig(env);
    let origin: string | null = null;

    try {
      origin = allowedOrigin(request, config.allowedOrigins);
      if (request.method === "OPTIONS") {
        return withSecurityHeaders(new Response(null, { status: 204, headers: corsHeaders(origin) }), requestId, origin);
      }

      const url = new URL(request.url);
      const client = new YouCamClient(env, upstreamFetch);

      const publicMediaMatch = /^\/media\/([a-f0-9]{32})$/.exec(url.pathname);
      if (request.method === "GET" && publicMediaMatch?.[1]) {
        return withSecurityHeaders(
          await serveLocalMedia(request, publicMediaMatch[1], env),
          requestId,
          origin
        );
      }

      const uploadMediaMatch = /^\/v1\/media\/([a-f0-9]{32})$/.exec(url.pathname);
      if (request.method === "PUT" && uploadMediaMatch?.[1]) {
        return withSecurityHeaders(
          await storeLocalUpload(request, uploadMediaMatch[1], env, config.mediaTtlSeconds),
          requestId,
          origin
        );
      }

      if (request.method === "GET" && url.pathname === "/healthz") {
        const accessGateConfigured = hasSecureJudgeCode(env);
        const stateStoreConfigured = Boolean(env.DRAPEPROOF_STATE);
        const paidLedgerConfigured = Boolean(env.PAID_TASK_LEDGER);
        const configured =
          hasUsableApiKey(env) &&
          config.providerConfigured &&
          accessGateConfigured &&
          stateStoreConfigured &&
          paidLedgerConfigured;
        return withSecurityHeaders(
          json({
            status: configured ? "ok" : "degraded",
            service: "drapeproof-api",
            version: API_VERSION,
            vtoProvider: config.provider,
            vtoProviderConfigured: config.providerConfigured,
            accessGateConfigured,
            stateStoreConfigured,
            paidLedgerConfigured
          }, configured ? 200 : 503),
          requestId,
          origin
        );
      }

      if (request.method === "POST" && url.pathname === "/v1/session") {
        if (!hasUsableApiKey(env)) {
          throw new ApiError(503, "service_not_configured", "The YouCam integration is not configured.");
        }
        if (!hasSecureJudgeCode(env)) {
          throw new ApiError(
            503,
            "access_gate_not_configured",
            "Public access is disabled until a secure judge code is configured."
          );
        }
        if (!env.DRAPEPROOF_STATE) {
          throw new ApiError(
            503,
            "state_store_not_configured",
            "Session issuance requires the persistent state store."
          );
        }
        await enforceRateLimit(env, "session", clientKey(request), config.rateLimits.session);
        const body = parseWith(sessionSchema, await readJson(request));
        if (!(await verifyJudgeCode(body.accessCode, env))) {
          throw new ApiError(401, "invalid_access_code", "The access code is invalid.");
        }
        const session = await issueSessionToken(env, config.sessionTtlSeconds);
        return withSecurityHeaders(
          json({
            token: session.token,
            tokenType: "Bearer",
            expiresAt: new Date(session.expiresAt * 1_000).toISOString(),
            expiresInSeconds: config.sessionTtlSeconds
          }, 201),
          requestId,
          origin
        );
      }

      if (!url.pathname.startsWith("/v1/")) {
        throw new ApiError(404, "not_found", "The requested route does not exist.");
      }

      if (!hasSecureJudgeCode(env)) {
        throw new ApiError(
          503,
          "access_gate_not_configured",
          "Public access is disabled until a secure judge code is configured."
        );
      }

      await verifySessionToken(bearerToken(request), env);
      const identity = clientKey(request);

      if (request.method === "GET" && url.pathname === "/v1/credits") {
        assertPaidLedgerConfigured(env);
        await enforceRateLimit(env, "credits", identity, config.rateLimits.poll);
        const [facialColorCost, tryOnCost, status] = await Promise.all([
          client.featureCost("facial-colors"),
          client.featureCost("try-on"),
          paidBudgetStatus(env, config.unitBudgetId, config.unitBudget, config.creditFloor)
        ]);
        return withSecurityHeaders(
          json({
            ...status,
            provider: config.provider,
            costs: { facialColors: facialColorCost, tryOn: tryOnCost },
            balanceSource: "protected-local-ledger"
          }),
          requestId,
          origin
        );
      }

      if (request.method === "POST" && url.pathname === "/v1/uploads") {
        await enforceRateLimit(env, "upload", identity, config.rateLimits.upload);
        const body = parseWith(uploadSchema, await readJson(request));
        const tickets =
          body.feature === "try-on" && config.provider === "scarf"
            ? await createLocalUploadTickets(request, body.files, env, config.uploadTicketTtlSeconds)
            : await client.createUploadTickets(body.feature, body.files);
        return withSecurityHeaders(json({ feature: body.feature, files: tickets }, 201), requestId, origin);
      }

      if (request.method === "POST" && url.pathname === "/v1/tasks/facial-colors") {
        assertPaidStoresConfigured(env);
        await enforceRateLimit(env, "task", identity, config.rateLimits.task);
        const body = parseWith(facialTaskSchema, await readJson(request));
        const taskCost = await client.featureCost("facial-colors");
        const fingerprint = await fingerprintPaidRequest("facial-colors", {
          sourceFileId: body.sourceFileId,
          faceAngleStrictness: body.faceAngleStrictness
        });
        const result = await runPaidTask(
          env,
          config.unitBudgetId,
          "facial-colors",
          body.operationId,
          fingerprint,
          config.unitBudget,
          config.creditFloor,
          config.maxTasksPerDay,
          taskCost,
          () => client.createFacialTask(body.sourceFileId, body.faceAngleStrictness)
        );
        return withSecurityHeaders(paidTaskResponse(result), requestId, origin);
      }

      if (request.method === "POST" && url.pathname === "/v1/tasks/try-on") {
        assertPaidStoresConfigured(env);
        await enforceRateLimit(env, "task", identity, config.rateLimits.task);
        const rawBody = await readJson(request);
        if (!config.providerConfigured) {
          throw new ApiError(
            503,
            "vto_provider_not_configured",
            "The private image store required by the Scarf provider is not configured."
          );
        }
        let providerInput: Parameters<YouCamClient["createVtoTask"]>[0];
        let operationId: string;
        let fingerprintPayload: unknown;
        if (config.provider === "scarf") {
          const body = parseWith(scarfVtoTaskSchema, rawBody);
          operationId = body.operationId;
          const gender = body.gender ?? config.scarfDefaultGender;
          if (!gender) {
            throw new ApiError(400, "gender_required", "Scarf try-on requires gender female or male.");
          }
          let sourceImageUrl: string;
          let referenceImageUrl: string;
          if (body.sourceFileId && body.referenceFileId) {
            sourceImageUrl = await signedMediaUrl(
              body.sourceFileId,
              url.origin,
              env,
              config.mediaReadTtlSeconds
            );
            referenceImageUrl = await signedMediaUrl(
              body.referenceFileId,
              url.origin,
              env,
              config.mediaReadTtlSeconds
            );
          } else if (body.sourceImageUrl && body.referenceImageUrl) {
            sourceImageUrl = validateAllowedImageUrl(body.sourceImageUrl, config.scarfAllowedImageHosts);
            referenceImageUrl = validateAllowedImageUrl(
              body.referenceImageUrl,
              config.scarfAllowedImageHosts
            );
          } else {
            throw new ApiError(400, "validation_error", "A complete Scarf image pair is required.");
          }
          providerInput = {
            provider: "scarf",
            sourceImageUrl,
            referenceImageUrl,
            gender,
            style: body.style
          };
          fingerprintPayload = {
            sourceFileId: body.sourceFileId,
            referenceFileId: body.referenceFileId,
            sourceImageUrl: body.sourceImageUrl,
            referenceImageUrl: body.referenceImageUrl,
            gender,
            style: body.style,
            provider: "scarf"
          };
        } else {
          const body = parseWith(clothesVtoTaskSchema, rawBody);
          operationId = body.operationId;
          providerInput = {
            provider: "clothes",
            sourceFileId: body.sourceFileId,
            referenceFileId: body.referenceFileId,
            templateId: body.templateId,
            garmentCategory: body.garmentCategory
          };
          fingerprintPayload = {
            sourceFileId: body.sourceFileId,
            referenceFileId: body.referenceFileId,
            templateId: body.templateId,
            garmentCategory: body.garmentCategory,
            provider: "clothes"
          };
        }
        const taskCost = await client.featureCost("try-on");
        const fingerprint = await fingerprintPaidRequest("try-on", fingerprintPayload);
        const result = await runPaidTask(
          env,
          config.unitBudgetId,
          "try-on",
          operationId,
          fingerprint,
          config.unitBudget,
          config.creditFloor,
          config.maxTasksPerDay,
          taskCost,
          () => client.createVtoTask(providerInput)
        );
        return withSecurityHeaders(paidTaskResponse(result, config.provider), requestId, origin);
      }

      const operationMatch = /^\/v1\/operations\/([^/]+)$/.exec(url.pathname);
      if (request.method === "GET" && operationMatch?.[1]) {
        assertPaidLedgerConfigured(env);
        await enforceRateLimit(env, "poll", identity, config.rateLimits.poll);
        const result = await paidOperationStatus(
          env,
          config.unitBudgetId,
          validateOperationId(operationMatch[1])
        );
        return withSecurityHeaders(operationStatusResponse(result, config.provider), requestId, origin);
      }

      const pollMatch = /^\/v1\/tasks\/(facial-colors|try-on)\/([^/]+)$/.exec(url.pathname);
      if (request.method === "GET" && pollMatch?.[1] && pollMatch[2]) {
        await enforceRateLimit(env, "poll", identity, config.rateLimits.poll);
        const feature = pollMatch[1] as ApiFeature;
        const taskId = validateTaskId(pollMatch[2]);
        const state = await client.pollTask(feature, taskId);
        return withSecurityHeaders(json(normalizeTaskResult(feature, taskId, state)), requestId, origin);
      }

      const knownPath =
        url.pathname === "/v1/credits" ||
        url.pathname === "/v1/uploads" ||
        url.pathname === "/v1/tasks/facial-colors" ||
        url.pathname === "/v1/tasks/try-on" ||
        Boolean(operationMatch) ||
        Boolean(pollMatch) ||
        Boolean(publicMediaMatch) ||
        Boolean(uploadMediaMatch);
      if (knownPath) throw new ApiError(405, "method_not_allowed", "This method is not allowed.");
      throw new ApiError(404, "not_found", "The requested route does not exist.");
    } catch (error) {
      return withSecurityHeaders(errorResponse(error, requestId), requestId, origin);
    }
  };
}

async function runPaidTask(
  env: Env,
  budgetId: string,
  feature: ApiFeature,
  operationId: string,
  fingerprint: string,
  budget: number,
  protectedFloor: number,
  maxTasksPerDay: number,
  taskCost: number,
  createTask: () => Promise<string>
): Promise<PaidOperationResult> {
  const admission = await admitPaidOperation(env, budgetId, {
    operationId,
    feature,
    fingerprint,
    unitCost: taskCost,
    budget,
    protectedFloor,
    maxTasksPerDay
  });
  if (admission.state !== "admitted") return admission;

  let taskId: string;
  try {
    taskId = await createTask();
  } catch (error) {
    if (error instanceof ApiError && error.code === "youcam_rejected") {
      await rejectPaidOperation(env, budgetId, {
        operationId,
        feature,
        fingerprint,
        rejectionCode: error.code,
        rejectionMessage: error.message
      });
      throw error;
    }
    try {
      await markPaidOperationUnknown(env, budgetId, { operationId, feature, fingerprint });
    } catch {
      // The admission remains fail-closed and will become UNKNOWN_RECONCILE after its stale window.
    }
    throw new ApiError(
      409,
      "operation_outcome_unknown",
      "YouCam may have accepted this task. Do not create another operation; reconcile this operation ID.",
      { operationId, state: "UNKNOWN_RECONCILE", reservedUnitCost: taskCost }
    );
  }

  try {
    return await acceptPaidOperation(env, budgetId, {
      operationId,
      feature,
      fingerprint,
      taskId
    });
  } catch {
    throw new ApiError(
      503,
      "operation_commit_unavailable",
      "YouCam accepted the task, but the operation ledger could not confirm it. Save these IDs for reconciliation.",
      { operationId, taskId, state: "UNKNOWN_RECONCILE", reservedUnitCost: taskCost }
    );
  }
}

function paidTaskResponse(result: PaidOperationResult, provider?: string): Response {
  const common = {
    operationId: result.operationId,
    feature: result.feature,
    ...(result.feature === "try-on" && provider ? { provider } : {}),
    reservedUnitCost: result.reservedUnitCost
  };
  if (result.state === "accepted") {
    return json({
      ...common,
      taskId: result.taskId,
      status: "accepted",
      pollAfterSeconds: 2,
      replayed: result.replayed
    }, 202);
  }
  if (result.state === "in_progress") {
    return json({
      ...common,
      status: "operation_pending",
      retryAfterSeconds: result.retryAfterSeconds,
      replayed: true
    }, 202, { "retry-after": String(result.retryAfterSeconds) });
  }
  if (result.state === "unknown_reconcile") {
    throw new ApiError(
      409,
      "operation_outcome_unknown",
      "This operation has an indeterminate provider outcome. Do not create another operation until it is reconciled.",
      { ...common, state: "UNKNOWN_RECONCILE" }
    );
  }
  if (result.state === "rejected") {
    throw new ApiError(
      409,
      "operation_rejected",
      "This operation was rejected previously. Use a new operation ID only after correcting the input.",
      { ...common, rejectionCode: result.rejectionCode }
    );
  }
  throw new ApiError(503, "ledger_invariant_failed", "The paid-task ledger returned an invalid state.");
}

function operationStatusResponse(result: PaidOperationResult, provider?: string): Response {
  const common = {
    operationId: result.operationId,
    feature: result.feature,
    ...(result.feature === "try-on" && provider ? { provider } : {}),
    reservedUnitCost: result.reservedUnitCost,
    replayed: true
  };
  if (result.state === "accepted") {
    return json({ ...common, status: "accepted", taskId: result.taskId, pollAfterSeconds: 2 });
  }
  if (result.state === "in_progress") {
    return json({
      ...common,
      status: "operation_pending",
      retryAfterSeconds: result.retryAfterSeconds
    });
  }
  if (result.state === "unknown_reconcile") {
    return json({
      ...common,
      status: "unknown_reconcile",
      message: "Do not create a replacement task until this operation is reconciled."
    });
  }
  if (result.state === "rejected") {
    return json({
      ...common,
      status: "rejected",
      rejectionCode: result.rejectionCode,
      message: result.rejectionMessage
    });
  }
  throw new ApiError(503, "ledger_invariant_failed", "The paid-task ledger returned an invalid state.");
}

function assertPaidLedgerConfigured(env: Env): void {
  if (!env.PAID_TASK_LEDGER) {
    throw new ApiError(
      503,
      "paid_ledger_not_configured",
      "Paid tasks are disabled until the atomic operation ledger is configured."
    );
  }
}

function assertPaidStoresConfigured(env: Env): void {
  if (!env.DRAPEPROOF_STATE) {
    throw new ApiError(503, "state_store_not_configured", "Paid tasks require the persistent state store.");
  }
  assertPaidLedgerConfigured(env);
}

const handle = createHandler();

export { PaidTaskLedger } from "./paid-operations";

export default {
  fetch(request: Request, env: Env): Promise<Response> {
    return handle(request, env);
  }
};
