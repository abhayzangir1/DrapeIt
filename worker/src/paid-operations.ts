import { ApiError } from "./errors";
import type { ApiFeature, Env } from "./types";

const encoder = new TextEncoder();
const OPERATION_ID = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const FINGERPRINT = /^[a-f0-9]{64}$/;
const TASK_ID = /^[A-Za-z0-9_-]{16,512}$/;
const BUDGET_KEY = "budget:v2";
const DAILY_PREFIX = "daily:v1:";
const OPERATION_PREFIX = "operation:v1:";
const STALE_AFTER_SECONDS = 90;

export interface BudgetStatus {
  budget: number;
  reservedUnits: number;
  remaining: number;
  protectedFloor: number;
  availableForTasks: number;
}

type StoredOperationState = "admitted" | "accepted" | "unknown" | "rejected";

interface StoredBudget {
  v: 2;
  reservedUnits: number;
  updatedAt: number;
}

interface StoredDailyCount {
  v: 1;
  count: number;
  updatedAt: number;
}

interface StoredOperation {
  v: 1;
  operationId: string;
  feature: ApiFeature;
  fingerprint: string;
  unitCost: number;
  state: StoredOperationState;
  createdAt: number;
  updatedAt: number;
  taskId?: string;
  rejectionCode?: string;
  rejectionMessage?: string;
}

export type PaidOperationResult =
  | {
      state: "admitted";
      operationId: string;
      feature: ApiFeature;
      reservedUnitCost: number;
      replayed: false;
    }
  | {
      state: "in_progress";
      operationId: string;
      feature: ApiFeature;
      reservedUnitCost: number;
      replayed: true;
      retryAfterSeconds: number;
    }
  | {
      state: "accepted";
      operationId: string;
      feature: ApiFeature;
      reservedUnitCost: number;
      replayed: true;
      taskId: string;
    }
  | {
      state: "unknown_reconcile";
      operationId: string;
      feature: ApiFeature;
      reservedUnitCost: number;
      replayed: true;
    }
  | {
      state: "rejected";
      operationId: string;
      feature: ApiFeature;
      reservedUnitCost: number;
      replayed: true;
      rejectionCode: string;
      rejectionMessage: string;
    };

interface AdmitInput {
  operationId: string;
  feature: ApiFeature;
  fingerprint: string;
  unitCost: number;
  budget: number;
  protectedFloor: number;
  maxTasksPerDay: number;
}

interface TransitionInput {
  operationId: string;
  feature: ApiFeature;
  fingerprint: string;
  taskId?: string;
  rejectionCode?: string;
  rejectionMessage?: string;
}

/**
 * A single Durable Object instance is selected from UNIT_BUDGET_ID. Its storage transaction is
 * the serialization point for both unit reservations and operation IDs. The Worker never asks
 * YouCam to create a paid task unless this object returns a brand-new `admitted` result.
 */
export class PaidTaskLedger {
  constructor(
    private readonly state: DurableObjectState,
    _env: Env
  ) {}

  async fetch(request: Request): Promise<Response> {
    try {
      const url = new URL(request.url);
      if (request.method === "GET" && url.pathname === "/budget") {
        const budget = requiredInteger(url.searchParams.get("budget"), "budget", 1, 10_000_000);
        const floor = requiredInteger(url.searchParams.get("floor"), "floor", 0, 1_000_000);
        return ledgerJson(await this.budgetStatus(budget, floor));
      }

      const operationMatch = /^\/operations\/([0-9a-f-]{36})$/i.exec(url.pathname);
      if (request.method === "GET" && operationMatch?.[1]) {
        return ledgerJson(await this.operationStatus(validateOperationId(operationMatch[1])));
      }

      if (request.method !== "POST") return ledgerJson({ error: "not_found" }, 404);
      const body = await request.json<unknown>();
      if (url.pathname === "/admit") return ledgerJson(await this.admit(parseAdmitInput(body)));
      if (url.pathname === "/accept") {
        return ledgerJson(await this.transition("accepted", parseTransitionInput(body)));
      }
      if (url.pathname === "/unknown") {
        return ledgerJson(await this.transition("unknown", parseTransitionInput(body)));
      }
      if (url.pathname === "/reject") {
        return ledgerJson(await this.transition("rejected", parseTransitionInput(body)));
      }
      return ledgerJson({ error: "not_found" }, 404);
    } catch (error) {
      const apiError = error instanceof ApiError
        ? error
        : new ApiError(500, "ledger_error", "The paid-task ledger could not complete the request.");
      return ledgerJson(
        { error: { code: apiError.code, message: apiError.message, details: apiError.details } },
        apiError.status
      );
    }
  }

  private async budgetStatus(budget: number, protectedFloor: number): Promise<BudgetStatus> {
    const stored = await this.state.storage.get<StoredBudget>(BUDGET_KEY);
    const reservedUnits = validBudget(stored)?.reservedUnits ?? 0;
    return budgetView(budget, protectedFloor, reservedUnits);
  }

  private async operationStatus(operationId: string): Promise<PaidOperationResult> {
    return this.state.storage.transaction(async (transaction) => {
      const key = `${OPERATION_PREFIX}${operationId}`;
      const operation = validOperation(await transaction.get<StoredOperation>(key));
      if (!operation) {
        throw new ApiError(404, "operation_not_found", "The paid operation was not found.");
      }
      return this.replayOrExpire(transaction, key, operation, nowSeconds());
    });
  }

  private async admit(input: AdmitInput): Promise<PaidOperationResult> {
    return this.state.storage.transaction(async (transaction) => {
      const now = nowSeconds();
      const operationKey = `${OPERATION_PREFIX}${input.operationId}`;
      const existing = validOperation(await transaction.get<StoredOperation>(operationKey));
      if (existing) {
        assertSameOperation(existing, input);
        return this.replayOrExpire(transaction, operationKey, existing, now);
      }

      const budget = validBudget(await transaction.get<StoredBudget>(BUDGET_KEY)) ?? {
        v: 2 as const,
        reservedUnits: 0,
        updatedAt: 0
      };
      const view = budgetView(input.budget, input.protectedFloor, budget.reservedUnits);
      if (view.remaining - input.unitCost < input.protectedFloor) {
        throw new ApiError(
          402,
          "credit_reserve_reached",
          "The protected YouCam unit reserve has been reached.",
          { remaining: view.remaining, floor: input.protectedFloor, taskCost: input.unitCost }
        );
      }

      const day = new Date(now * 1_000).toISOString().slice(0, 10);
      const dailyKey = `${DAILY_PREFIX}${day}`;
      const daily = validDaily(await transaction.get<StoredDailyCount>(dailyKey)) ?? {
        v: 1 as const,
        count: 0,
        updatedAt: 0
      };
      if (daily.count >= input.maxTasksPerDay) {
        throw new ApiError(
          429,
          "daily_task_limit_reached",
          "The demo task limit has been reached for today."
        );
      }

      const operation: StoredOperation = {
        v: 1,
        operationId: input.operationId,
        feature: input.feature,
        fingerprint: input.fingerprint,
        unitCost: input.unitCost,
        state: "admitted",
        createdAt: now,
        updatedAt: now
      };
      await transaction.put({
        [BUDGET_KEY]: {
          v: 2,
          reservedUnits: budget.reservedUnits + input.unitCost,
          updatedAt: now
        } satisfies StoredBudget,
        [dailyKey]: { v: 1, count: daily.count + 1, updatedAt: now } satisfies StoredDailyCount,
        [operationKey]: operation
      });
      return operationResult(operation, false);
    });
  }

  private async transition(
    next: "accepted" | "unknown" | "rejected",
    input: TransitionInput
  ): Promise<PaidOperationResult> {
    return this.state.storage.transaction(async (transaction) => {
      const key = `${OPERATION_PREFIX}${input.operationId}`;
      const operation = validOperation(await transaction.get<StoredOperation>(key));
      if (!operation) throw new ApiError(404, "operation_not_found", "The paid operation was not found.");
      assertSameOperation(operation, input);

      if (operation.state === "accepted") {
        if (next === "accepted" && input.taskId && operation.taskId === input.taskId) {
          return operationResult(operation, true);
        }
        throw new ApiError(409, "operation_already_accepted", "The operation already has a task ID.");
      }
      if (operation.state === "rejected") return operationResult(operation, true);

      const now = nowSeconds();
      if (next === "accepted") {
        if (!input.taskId || !TASK_ID.test(input.taskId)) {
          throw new ApiError(400, "invalid_task_id", "A valid task ID is required.");
        }
        const accepted: StoredOperation = {
          ...operation,
          state: "accepted",
          taskId: input.taskId,
          updatedAt: now
        };
        await transaction.put(key, accepted);
        return operationResult(accepted, true);
      }

      if (next === "unknown") {
        const unknown: StoredOperation = { ...operation, state: "unknown", updatedAt: now };
        await transaction.put(key, unknown);
        return operationResult(unknown, true);
      }

      const budget = validBudget(await transaction.get<StoredBudget>(BUDGET_KEY));
      if (!budget || budget.reservedUnits < operation.unitCost) {
        throw new ApiError(503, "ledger_invariant_failed", "The paid-task ledger is inconsistent.");
      }
      const rejected: StoredOperation = {
        ...operation,
        state: "rejected",
        rejectionCode: safeCode(input.rejectionCode) ?? "upstream_rejected",
        rejectionMessage: safeMessage(input.rejectionMessage) ?? "The provider rejected this operation.",
        updatedAt: now
      };
      await transaction.put({
        [BUDGET_KEY]: {
          v: 2,
          reservedUnits: budget.reservedUnits - operation.unitCost,
          updatedAt: now
        } satisfies StoredBudget,
        [key]: rejected
      });
      return operationResult(rejected, true);
    });
  }

  private async replayOrExpire(
    transaction: DurableObjectTransaction,
    key: string,
    operation: StoredOperation,
    now: number
  ): Promise<PaidOperationResult> {
    if (operation.state === "admitted" && now - operation.updatedAt >= STALE_AFTER_SECONDS) {
      const unknown: StoredOperation = { ...operation, state: "unknown", updatedAt: now };
      await transaction.put(key, unknown);
      return operationResult(unknown, true);
    }
    return operationResult(operation, true);
  }
}

export async function fingerprintPaidRequest(feature: ApiFeature, payload: unknown): Promise<string> {
  const canonical = stableJson({ v: 1, feature, payload });
  const digest = await crypto.subtle.digest("SHA-256", encoder.encode(canonical));
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

export async function paidBudgetStatus(
  env: Env,
  budgetId: string,
  budget: number,
  protectedFloor: number
): Promise<BudgetStatus> {
  const query = new URLSearchParams({ budget: String(budget), floor: String(protectedFloor) });
  return callLedger<BudgetStatus>(env, budgetId, `/budget?${query.toString()}`, { method: "GET" });
}

export async function admitPaidOperation(
  env: Env,
  budgetId: string,
  input: AdmitInput
): Promise<PaidOperationResult> {
  return callLedger<PaidOperationResult>(env, budgetId, "/admit", postJson(input));
}

export async function acceptPaidOperation(
  env: Env,
  budgetId: string,
  input: TransitionInput & { taskId: string }
): Promise<PaidOperationResult> {
  return callLedger<PaidOperationResult>(env, budgetId, "/accept", postJson(input));
}

export async function markPaidOperationUnknown(
  env: Env,
  budgetId: string,
  input: TransitionInput
): Promise<PaidOperationResult> {
  return callLedger<PaidOperationResult>(env, budgetId, "/unknown", postJson(input));
}

export async function rejectPaidOperation(
  env: Env,
  budgetId: string,
  input: TransitionInput & { rejectionCode: string; rejectionMessage: string }
): Promise<PaidOperationResult> {
  return callLedger<PaidOperationResult>(env, budgetId, "/reject", postJson(input));
}

export async function paidOperationStatus(
  env: Env,
  budgetId: string,
  operationId: string
): Promise<PaidOperationResult> {
  return callLedger<PaidOperationResult>(
    env,
    budgetId,
    `/operations/${encodeURIComponent(validateOperationId(operationId))}`,
    { method: "GET" }
  );
}

export function validateOperationId(value: string): string {
  if (!OPERATION_ID.test(value)) {
    throw new ApiError(400, "invalid_operation_id", "The paid operation ID must be a UUID v4.");
  }
  return value.toLowerCase();
}

async function callLedger<T>(
  env: Env,
  budgetId: string,
  path: string,
  init: RequestInit
): Promise<T> {
  if (!env.PAID_TASK_LEDGER) {
    throw new ApiError(
      503,
      "paid_ledger_not_configured",
      "Paid tasks are disabled until the atomic operation ledger is configured."
    );
  }
  const id = env.PAID_TASK_LEDGER.idFromName(budgetId);
  const response = await env.PAID_TASK_LEDGER.get(id).fetch(`https://paid-ledger.internal${path}`, init);
  const body = await response.json<unknown>();
  if (!response.ok) {
    const candidate = isRecord(body) && isRecord(body.error) ? body.error : null;
    throw new ApiError(
      response.status,
      typeof candidate?.code === "string" ? candidate.code : "paid_ledger_unavailable",
      typeof candidate?.message === "string"
        ? candidate.message
        : "The paid-task ledger is unavailable.",
      candidate?.details
    );
  }
  return body as T;
}

function postJson(body: unknown): RequestInit {
  return {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body)
  };
}

function parseAdmitInput(value: unknown): AdmitInput {
  if (!isRecord(value)) throw new ApiError(400, "invalid_ledger_request", "Invalid ledger request.");
  return {
    operationId: validateOperationId(requiredString(value.operationId)),
    feature: requiredFeature(value.feature),
    fingerprint: requiredFingerprint(value.fingerprint),
    unitCost: requiredInteger(value.unitCost, "unitCost", 1, 1_000_000),
    budget: requiredInteger(value.budget, "budget", 1, 10_000_000),
    protectedFloor: requiredInteger(value.protectedFloor, "protectedFloor", 0, 1_000_000),
    maxTasksPerDay: requiredInteger(value.maxTasksPerDay, "maxTasksPerDay", 1, 10_000)
  };
}

function parseTransitionInput(value: unknown): TransitionInput {
  if (!isRecord(value)) throw new ApiError(400, "invalid_ledger_request", "Invalid ledger request.");
  const taskId = typeof value.taskId === "string" ? value.taskId : undefined;
  const rejectionCode = typeof value.rejectionCode === "string" ? value.rejectionCode : undefined;
  const rejectionMessage = typeof value.rejectionMessage === "string" ? value.rejectionMessage : undefined;
  return {
    operationId: validateOperationId(requiredString(value.operationId)),
    feature: requiredFeature(value.feature),
    fingerprint: requiredFingerprint(value.fingerprint),
    ...(taskId ? { taskId } : {}),
    ...(rejectionCode ? { rejectionCode } : {}),
    ...(rejectionMessage ? { rejectionMessage } : {})
  };
}

function assertSameOperation(
  operation: StoredOperation,
  input: Pick<AdmitInput, "operationId" | "feature" | "fingerprint"> & { unitCost?: number }
): void {
  if (
    operation.operationId !== input.operationId ||
    operation.feature !== input.feature ||
    operation.fingerprint !== input.fingerprint ||
    (input.unitCost !== undefined && operation.unitCost !== input.unitCost)
  ) {
    throw new ApiError(
      409,
      "operation_id_conflict",
      "This operation ID is already bound to a different paid request."
    );
  }
}

function operationResult(operation: StoredOperation, replayed: boolean): PaidOperationResult {
  const base = {
    operationId: operation.operationId,
    feature: operation.feature,
    reservedUnitCost: operation.unitCost
  };
  if (operation.state === "admitted") {
    return replayed
      ? { ...base, state: "in_progress", replayed: true, retryAfterSeconds: 2 }
      : { ...base, state: "admitted", replayed: false };
  }
  if (operation.state === "accepted") {
    if (!operation.taskId) throw new ApiError(503, "ledger_invariant_failed", "The paid-task ledger is inconsistent.");
    return { ...base, state: "accepted", replayed: true, taskId: operation.taskId };
  }
  if (operation.state === "unknown") {
    return { ...base, state: "unknown_reconcile", replayed: true };
  }
  return {
    ...base,
    state: "rejected",
    replayed: true,
    rejectionCode: operation.rejectionCode ?? "upstream_rejected",
    rejectionMessage: operation.rejectionMessage ?? "The provider rejected this operation."
  };
}

function validBudget(value: StoredBudget | undefined): StoredBudget | null {
  return value?.v === 2 && Number.isSafeInteger(value.reservedUnits) && value.reservedUnits >= 0 &&
    Number.isSafeInteger(value.updatedAt) ? value : null;
}

function validDaily(value: StoredDailyCount | undefined): StoredDailyCount | null {
  return value?.v === 1 && Number.isSafeInteger(value.count) && value.count >= 0 &&
    Number.isSafeInteger(value.updatedAt) ? value : null;
}

function validOperation(value: StoredOperation | undefined): StoredOperation | null {
  if (!value || value.v !== 1 || !OPERATION_ID.test(value.operationId) ||
    (value.feature !== "facial-colors" && value.feature !== "try-on") ||
    !FINGERPRINT.test(value.fingerprint) || !Number.isSafeInteger(value.unitCost) || value.unitCost <= 0 ||
    !["admitted", "accepted", "unknown", "rejected"].includes(value.state) ||
    !Number.isSafeInteger(value.createdAt) || !Number.isSafeInteger(value.updatedAt)) return null;
  return value;
}

function budgetView(budget: number, protectedFloor: number, reservedUnits: number): BudgetStatus {
  const remaining = Math.max(0, budget - reservedUnits);
  return {
    budget,
    reservedUnits,
    remaining,
    protectedFloor,
    availableForTasks: Math.max(0, remaining - protectedFloor)
  };
}

function stableJson(value: unknown): string {
  if (value === null || typeof value === "string" || typeof value === "boolean") return JSON.stringify(value);
  if (typeof value === "number") {
    if (!Number.isFinite(value)) throw new ApiError(400, "invalid_fingerprint_input", "Invalid fingerprint input.");
    return JSON.stringify(value);
  }
  if (Array.isArray(value)) return `[${value.map(stableJson).join(",")}]`;
  if (isRecord(value)) {
    return `{${Object.keys(value)
      .filter((key) => value[key] !== undefined)
      .sort()
      .map((key) => `${JSON.stringify(key)}:${stableJson(value[key])}`)
      .join(",")}}`;
  }
  throw new ApiError(400, "invalid_fingerprint_input", "Invalid fingerprint input.");
}

function ledgerJson(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" }
  });
}

function requiredInteger(value: unknown, name: string, minimum: number, maximum: number): number {
  const parsed = typeof value === "string" && /^\d+$/.test(value) ? Number(value) : value;
  if (!Number.isSafeInteger(parsed) || (parsed as number) < minimum || (parsed as number) > maximum) {
    throw new ApiError(400, "invalid_ledger_request", `${name} is invalid.`);
  }
  return parsed as number;
}

function requiredString(value: unknown): string {
  if (typeof value !== "string") throw new ApiError(400, "invalid_ledger_request", "Invalid ledger request.");
  return value;
}

function requiredFeature(value: unknown): ApiFeature {
  if (value !== "facial-colors" && value !== "try-on") {
    throw new ApiError(400, "invalid_ledger_request", "Invalid ledger feature.");
  }
  return value;
}

function requiredFingerprint(value: unknown): string {
  if (typeof value !== "string" || !FINGERPRINT.test(value)) {
    throw new ApiError(400, "invalid_ledger_request", "Invalid ledger fingerprint.");
  }
  return value;
}

function safeCode(value: string | undefined): string | null {
  return value && /^[A-Za-z0-9_.-]{1,96}$/.test(value) ? value : null;
}

function safeMessage(value: string | undefined): string | null {
  if (!value) return null;
  const message = value.replace(/[\r\n\t]+/g, " ").trim();
  return message && message.length <= 300 ? message : null;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value && typeof value === "object" && !Array.isArray(value));
}

function nowSeconds(): number {
  return Math.floor(Date.now() / 1_000);
}
