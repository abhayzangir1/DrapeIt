export type VtoProviderId = "clothes" | "scarf";
export type ApiFeature = "facial-colors" | "try-on";

export interface Env {
  YOUCAM_API_KEY: string;
  JUDGE_ACCESS_CODE?: string;
  SESSION_SECRET?: string;
  ALLOWED_ORIGINS?: string;
  SESSION_TTL_SECONDS?: string;
  CREDIT_FLOOR?: string;
  UNIT_BUDGET?: string;
  UNIT_BUDGET_ID?: string;
  VTO_PROVIDER?: string;
  SCARF_DEFAULT_GENDER?: string;
  SCARF_ALLOWED_IMAGE_HOSTS?: string;
  MEDIA_TTL_SECONDS?: string;
  MEDIA_READ_TTL_SECONDS?: string;
  UPLOAD_TICKET_TTL_SECONDS?: string;
  RATE_LIMIT_SESSION?: string;
  RATE_LIMIT_UPLOAD?: string;
  RATE_LIMIT_TASK?: string;
  RATE_LIMIT_POLL?: string;
  MAX_TASKS_PER_DAY?: string;
  DRAPEPROOF_STATE?: KVNamespace;
  IMAGE_STORE?: R2Bucket;
  PAID_TASK_LEDGER?: DurableObjectNamespace;
}

export interface SessionClaims {
  v: 1;
  iat: number;
  exp: number;
  jti: string;
  aud: "drapeproof-api";
}

export interface UploadFileInput {
  contentType: "image/jpeg" | "image/jpg" | "image/png";
  fileName: string;
  fileSize: number;
}

export interface RateLimitResult {
  allowed: boolean;
  limit: number;
  remaining: number;
  resetAt: number;
}

export type Fetcher = (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>;

export interface YouCamTaskState {
  taskStatus: "running" | "success" | "error";
  error?: string;
  errorMessage?: string;
  results?: unknown;
}
