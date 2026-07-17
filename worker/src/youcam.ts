import { YOUCAM_ORIGIN, hasUsableApiKey } from "./config";
import { ApiError } from "./errors";
import type { ApiFeature, Env, Fetcher, UploadFileInput, VtoProviderId, YouCamTaskState } from "./types";

const FACIAL_FILE_PATH = "/s2s/v2.0/file/skin-tone-analysis";
const FACIAL_TASK_PATH = "/s2s/v2.0/task/skin-tone-analysis";
const CLOTHES_FILE_PATH = "/s2s/v2.0/file/cloth-v3";
const CLOTHES_TASK_PATH = "/s2s/v2.0/task/cloth-v3";
const SCARF_TASK_PATH = "/s2s/v2.0/task/scarf";
const FEATURE_COST_PATH = "/s2s/v2.0/credit/feature-cost";
const MAX_UPSTREAM_BYTES = 512 * 1024;

type JsonObject = Record<string, unknown>;

export type VtoTaskInput =
  | {
      provider: "scarf";
      sourceImageUrl: string;
      referenceImageUrl: string;
      gender: "female" | "male";
      style:
        | "style_french_elegance"
        | "style_light_luxury"
        | "style_cottagecore"
        | "style_modern_chic"
        | "style_bohemian";
    }
  | {
      provider: "clothes";
      sourceFileId: string;
      referenceFileId?: string | undefined;
      templateId?: string | undefined;
      garmentCategory: "auto" | "upper_body" | "lower_body" | "full_body" | "shoes";
    };

export interface UploadTicket {
  fileId: string;
  fileName: string;
  contentType: string;
  upload: {
    method: "PUT";
    url: string;
    headers: Record<string, string>;
  };
}

export class YouCamClient {
  constructor(
    private readonly env: Env,
    private readonly fetcher: Fetcher = fetch
  ) {}

  providerId(): VtoProviderId {
    return this.env.VTO_PROVIDER === "clothes" ? "clothes" : "scarf";
  }

  vtoTaskPath(): string {
    return this.providerId() === "scarf" ? SCARF_TASK_PATH : CLOTHES_TASK_PATH;
  }

  async createUploadTickets(feature: ApiFeature, files: UploadFileInput[]): Promise<UploadTicket[]> {
    if (feature === "try-on" && this.providerId() === "scarf") {
      throw new ApiError(
        500,
        "invalid_upload_adapter",
        "Scarf uploads must use the private image store adapter."
      );
    }
    const path = feature === "facial-colors" ? FACIAL_FILE_PATH : CLOTHES_FILE_PATH;
    const body = {
      files: files.map((file) => ({
        content_type: file.contentType,
        file_name: file.fileName,
        file_size: file.fileSize
      }))
    };
    const response = await this.request(path, { method: "POST", body: JSON.stringify(body) });
    return parseUploadTickets(response, files);
  }

  async createFacialTask(sourceFileId: string, strictness: string): Promise<string> {
    const response = await this.request(FACIAL_TASK_PATH, {
      method: "POST",
      body: JSON.stringify({
        src_file_id: sourceFileId,
        face_angle_strictness_level: strictness
      })
    });
    return parseAcceptedTaskId(response);
  }

  async createVtoTask(input: VtoTaskInput): Promise<string> {
    if (input.provider !== this.providerId()) {
      throw new ApiError(500, "provider_mismatch", "The try-on provider request is invalid.");
    }
    const payload: JsonObject =
      input.provider === "scarf"
        ? {
            src_file_url: input.sourceImageUrl,
            ref_file_url: input.referenceImageUrl,
            gender: input.gender,
            style: input.style
          }
        : {
            src_file_id: input.sourceFileId,
            ...(input.referenceFileId ? { ref_file_id: input.referenceFileId } : {}),
            ...(input.templateId ? { template_id: input.templateId } : {}),
            garment_category: input.garmentCategory
          };
    const response = await this.request(this.vtoTaskPath(), {
      method: "POST",
      body: JSON.stringify(payload)
    });
    return parseAcceptedTaskId(response);
  }

  async pollTask(feature: ApiFeature, taskId: string): Promise<YouCamTaskState> {
    const taskPath = feature === "facial-colors" ? FACIAL_TASK_PATH : this.vtoTaskPath();
    const response = await this.request(`${taskPath}/${encodeURIComponent(taskId)}`, { method: "GET" });
    return parseTaskState(response);
  }

  async featureCost(feature: ApiFeature): Promise<number> {
    const targetPath = feature === "facial-colors" ? FACIAL_TASK_PATH : this.vtoTaskPath();
    let startingToken: string | undefined;
    const seenTokens = new Set<string>();

    for (let page = 0; page < 50; page += 1) {
      const query = new URLSearchParams({ page_size: "20" });
      if (startingToken) query.set("starting_token", startingToken);
      const response = await this.request(`${FEATURE_COST_PATH}?${query.toString()}`, { method: "GET" });
      const result = isObject(response.result) ? response.result : null;
      const skus = result && Array.isArray(result.skus) ? result.skus : null;
      if (!result || !skus) break;

      for (const sku of skus) {
        if (!isObject(sku) || typeof sku.run_task_url !== "string") continue;
        if (normalizeTaskPath(sku.run_task_url) !== targetPath) continue;
        if (typeof sku.amount === "number" && Number.isSafeInteger(sku.amount) && sku.amount > 0) {
          return sku.amount;
        }
        throw featureCostUnavailable();
      }

      const next = result.next_token;
      if (next === null || next === undefined || next === "") break;
      if ((typeof next !== "string" && typeof next !== "number") || String(next).length > 256) break;
      startingToken = String(next);
      if (seenTokens.has(startingToken)) break;
      seenTokens.add(startingToken);
    }
    throw featureCostUnavailable();
  }

  private async request(path: string, init: RequestInit): Promise<JsonObject> {
    if (!hasUsableApiKey(this.env)) {
      throw new ApiError(503, "service_not_configured", "The YouCam integration is not configured.");
    }
    let upstreamUrl: URL;
    try {
      upstreamUrl = new URL(path, YOUCAM_ORIGIN);
    } catch {
      throw new ApiError(500, "invalid_upstream_path", "The configured provider path is invalid.");
    }
    if (
      upstreamUrl.origin !== YOUCAM_ORIGIN ||
      !/^\/s2s\/[A-Za-z0-9/_.-]+$/.test(upstreamUrl.pathname) ||
      upstreamUrl.hash
    ) {
      throw new ApiError(500, "invalid_upstream_path", "The configured provider path is invalid.");
    }

    const headers = new Headers(init.headers);
    headers.set("authorization", `Bearer ${this.env.YOUCAM_API_KEY}`);
    headers.set("accept", "application/json");
    if (init.body) headers.set("content-type", "application/json");

    let response: Response;
    try {
      response = await this.fetcher(upstreamUrl.toString(), {
        ...init,
        headers,
        signal: AbortSignal.timeout(20_000)
      });
    } catch {
      throw new ApiError(502, "youcam_unavailable", "YouCam did not respond in time.");
    }

    const text = await response.text();
    if (new TextEncoder().encode(text).byteLength > MAX_UPSTREAM_BYTES) {
      throw new ApiError(502, "invalid_upstream_response", "YouCam returned an invalid response.");
    }

    let payload: unknown;
    try {
      payload = text ? (JSON.parse(text) as unknown) : {};
    } catch {
      throw new ApiError(502, "invalid_upstream_response", "YouCam returned an invalid response.");
    }
    if (!isObject(payload)) {
      throw new ApiError(502, "invalid_upstream_response", "YouCam returned an invalid response.");
    }

    const upstreamStatus = typeof payload.status === "number" ? payload.status : undefined;
    if (!response.ok || (upstreamStatus !== undefined && upstreamStatus >= 400)) {
      throw mapUpstreamError(response.status, payload, response.headers.get("retry-after"));
    }
    return payload;
  }
}

function featureCostUnavailable(): ApiError {
  return new ApiError(
    503,
    "feature_cost_unavailable",
    "The task unit cost could not be verified. New paid tasks are temporarily disabled."
  );
}

function normalizeTaskPath(value: string): string | null {
  try {
    const url = new URL(value, YOUCAM_ORIGIN);
    if (url.origin !== YOUCAM_ORIGIN || url.search || url.hash) return null;
    return url.pathname.replace(/\/$/, "");
  } catch {
    return null;
  }
}

function isObject(value: unknown): value is JsonObject {
  return Boolean(value && typeof value === "object" && !Array.isArray(value));
}

function dataObject(payload: JsonObject): JsonObject {
  return isObject(payload.data) ? payload.data : payload;
}

function parseAcceptedTaskId(payload: JsonObject): string {
  const data = dataObject(payload);
  if (typeof data.task_id !== "string" || !/^[A-Za-z0-9_-]{16,512}$/.test(data.task_id)) {
    throw new ApiError(502, "invalid_upstream_response", "YouCam did not return a valid task ID.");
  }
  return data.task_id;
}

function parseTaskState(payload: JsonObject): YouCamTaskState {
  const data = dataObject(payload);
  const status = data.task_status;
  if (status !== "running" && status !== "success" && status !== "error") {
    throw new ApiError(502, "invalid_upstream_response", "YouCam returned an unknown task state.");
  }
  return {
    taskStatus: status,
    ...(typeof data.error === "string" ? { error: safeUpstreamCode(data.error) } : {}),
    ...(typeof data.error_message === "string"
      ? { errorMessage: sanitizeUpstreamMessage(data.error_message) }
      : {}),
    ...(data.results === undefined ? {} : { results: data.results })
  };
}

function parseUploadTickets(payload: JsonObject, inputs: UploadFileInput[]): UploadTicket[] {
  const data = dataObject(payload);
  const files = Array.isArray(data.files) ? data.files : Array.isArray(payload.files) ? payload.files : null;
  if (!files || files.length !== inputs.length) {
    throw new ApiError(502, "invalid_upstream_response", "YouCam did not return valid upload tickets.");
  }

  return files.map((entry, index) => {
    if (!isObject(entry) || typeof entry.file_id !== "string") {
      throw new ApiError(502, "invalid_upstream_response", "YouCam returned an invalid upload ticket.");
    }
    if (!/^[A-Za-z0-9_+\-/=]{8,512}$/.test(entry.file_id)) {
      throw new ApiError(502, "invalid_upstream_response", "YouCam returned an invalid file ID.");
    }
    const requests = Array.isArray(entry.requests) ? entry.requests : [];
    const uploadRequest = requests[0];
    if (!isObject(uploadRequest) || uploadRequest.method !== "PUT" || typeof uploadRequest.url !== "string") {
      throw new ApiError(502, "invalid_upstream_response", "YouCam returned an invalid upload destination.");
    }
    const uploadUrl = safeMediaUrl(uploadRequest.url);
    const input = inputs[index];
    if (!input) throw new ApiError(502, "invalid_upstream_response", "Upload ticket order is invalid.");

    return {
      fileId: entry.file_id,
      fileName: input.fileName,
      contentType: input.contentType,
      upload: {
        method: "PUT",
        url: uploadUrl,
        headers: safeUploadHeaders(uploadRequest.headers)
      }
    };
  });
}

function safeUploadHeaders(value: unknown): Record<string, string> {
  if (!isObject(value)) return {};
  const result: Record<string, string> = {};
  for (const [name, raw] of Object.entries(value)) {
    const lower = name.toLowerCase();
    if (
      typeof raw === "string" &&
      raw.length <= 4_096 &&
      (lower === "content-type" ||
        lower === "content-length" ||
        lower.startsWith("x-amz-") ||
        lower.startsWith("x-goog-") ||
        lower.startsWith("x-ms-"))
    ) {
      result[name] = raw;
    }
  }
  return result;
}

export function safeMediaUrl(value: string): string {
  let url: URL;
  try {
    url = new URL(value);
  } catch {
    throw new ApiError(502, "invalid_upstream_response", "YouCam returned an invalid media URL.");
  }
  const allowed = ["amazonaws.com", "makeupar.com", "perfectcorp.com"].some(
    (suffix) => url.hostname === suffix || url.hostname.endsWith(`.${suffix}`)
  );
  if (url.protocol !== "https:" || url.username || url.password || !allowed) {
    throw new ApiError(502, "invalid_upstream_response", "YouCam returned an untrusted media URL.");
  }
  return url.toString();
}

function mapUpstreamError(status: number, payload: JsonObject, retryAfter: string | null): ApiError {
  const data = dataObject(payload);
  const code = safeUpstreamCode(
    typeof data.error === "string"
      ? data.error
      : typeof payload.error === "string"
        ? payload.error
        : "request_failed"
  );
  if (status === 429) {
    return new ApiError(429, "youcam_rate_limited", "YouCam is temporarily rate limited.", { upstreamCode: code },
      retryAfter && /^\d{1,5}$/.test(retryAfter) ? { "retry-after": retryAfter } : undefined);
  }
  if (status === 400 || status === 404 || status === 422) {
    return new ApiError(400, "youcam_rejected", "YouCam rejected the request.", { upstreamCode: code });
  }
  if (status === 401 || status === 403) {
    return new ApiError(502, "youcam_auth_failed", "The YouCam service credentials were rejected.");
  }
  return new ApiError(502, "youcam_error", "YouCam could not complete the request.", { upstreamCode: code });
}

function safeUpstreamCode(value: string): string {
  return /^[A-Za-z0-9_.-]{1,96}$/.test(value) ? value : "request_failed";
}

function sanitizeUpstreamMessage(value: string): string {
  const singleLine = value.replace(/[\r\n\t]+/g, " ").trim();
  return singleLine.length > 200 ? `${singleLine.slice(0, 197)}...` : singleLine;
}

export function normalizeTaskResult(feature: ApiFeature, taskId: string, state: YouCamTaskState): JsonObject {
  if (state.taskStatus === "running") {
    return { taskId, feature, status: "running", retryAfterSeconds: 2 };
  }
  if (state.taskStatus === "error") {
    return {
      taskId,
      feature,
      status: "error",
      error: {
        code: state.error ?? "processing_failed",
        message: state.errorMessage || "YouCam could not process this image."
      }
    };
  }

  if (feature === "facial-colors") {
    const results = isObject(state.results) ? state.results : {};
    const color = isObject(results.color) ? results.color : {};
    const allowedFields = [
      "skin_color",
      "eye_color",
      "eye_color_name",
      "eyebrow_color",
      "lip_color",
      "hair_color",
      "hair_color_name"
    ];
    const colors: JsonObject = {};
    for (const field of allowedFields) {
      const value = color[field];
      if (typeof value !== "string") continue;
      if (field.endsWith("_color") && !/^#[0-9A-Fa-f]{6}$/.test(value)) continue;
      if (value.length <= 64) colors[field] = value;
    }
    if (typeof colors.skin_color !== "string") {
      throw new ApiError(502, "invalid_upstream_response", "YouCam returned incomplete facial colors.");
    }
    return { taskId, feature, status: "success", result: { colors } };
  }

  const results = isObject(state.results) ? state.results : {};
  if (typeof results.url !== "string") {
    throw new ApiError(502, "invalid_upstream_response", "YouCam returned an incomplete try-on result.");
  }
  return {
    taskId,
    feature,
    status: "success",
    result: { imageUrl: safeMediaUrl(results.url) }
  };
}
