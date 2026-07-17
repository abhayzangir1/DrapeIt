import type { Env, VtoProviderId } from "./types";

export const YOUCAM_ORIGIN = "https://yce-api-01.makeupar.com";
export const MAX_JSON_BYTES = 16 * 1024;
export const MAX_IMAGE_BYTES = 10 * 1024 * 1024 - 1;

export const SCARF_STYLES = [
  "style_french_elegance",
  "style_light_luxury",
  "style_cottagecore",
  "style_modern_chic",
  "style_bohemian"
] as const;

function integerEnv(
  raw: string | undefined,
  fallback: number,
  minimum: number,
  maximum: number
): number {
  if (raw === undefined || raw.trim() === "") return fallback;
  if (!/^\d+$/.test(raw)) return fallback;
  const parsed = Number(raw);
  return Number.isSafeInteger(parsed) && parsed >= minimum && parsed <= maximum
    ? parsed
    : fallback;
}

export function getConfig(env: Env) {
  const provider: VtoProviderId = env.VTO_PROVIDER === "clothes" ? "clothes" : "scarf";

  return {
    provider,
    providerConfigured: provider === "clothes" || Boolean(env.IMAGE_STORE),
    allowedOrigins: new Set(
      (env.ALLOWED_ORIGINS ?? "")
        .split(",")
        .map((origin) => origin.trim())
        .filter((origin) => isValidAllowedOrigin(origin))
    ),
    sessionTtlSeconds: integerEnv(env.SESSION_TTL_SECONDS, 1_800, 300, 3_600),
    creditFloor: integerEnv(env.CREDIT_FLOOR, 300, 0, 1_000_000),
    unitBudget: integerEnv(env.UNIT_BUDGET, 1_000, 1, 10_000_000),
    unitBudgetId:
      env.UNIT_BUDGET_ID && /^[A-Za-z0-9_-]{1,64}$/.test(env.UNIT_BUDGET_ID)
        ? env.UNIT_BUDGET_ID
        : "hackathon-2026",
    scarfDefaultGender:
      env.SCARF_DEFAULT_GENDER === "female" || env.SCARF_DEFAULT_GENDER === "male"
        ? env.SCARF_DEFAULT_GENDER
        : undefined,
    scarfAllowedImageHosts: new Set(
      (env.SCARF_ALLOWED_IMAGE_HOSTS ?? "")
        .split(",")
        .map((host) => host.trim().toLowerCase())
        .filter((host) => /^(?:\*\.)?[a-z0-9](?:[a-z0-9.-]{0,251}[a-z0-9])?$/.test(host))
    ),
    mediaTtlSeconds: integerEnv(env.MEDIA_TTL_SECONDS, 86_400, 3_600, 86_400),
    mediaReadTtlSeconds: integerEnv(env.MEDIA_READ_TTL_SECONDS, 900, 60, 3_600),
    uploadTicketTtlSeconds: integerEnv(env.UPLOAD_TICKET_TTL_SECONDS, 600, 60, 1_800),
    rateLimits: {
      session: integerEnv(env.RATE_LIMIT_SESSION, 5, 1, 1_000),
      upload: integerEnv(env.RATE_LIMIT_UPLOAD, 20, 1, 1_000),
      task: integerEnv(env.RATE_LIMIT_TASK, 10, 1, 1_000),
      poll: integerEnv(env.RATE_LIMIT_POLL, 60, 1, 2_000)
    },
    maxTasksPerDay: integerEnv(env.MAX_TASKS_PER_DAY, 40, 1, 10_000)
  } as const;
}

function isValidAllowedOrigin(value: string): boolean {
  try {
    const url = new URL(value);
    if (url.origin !== value || url.username || url.password) return false;
    return url.protocol === "https:" || url.hostname === "localhost" || url.hostname === "127.0.0.1";
  } catch {
    return false;
  }
}

export function hasUsableApiKey(env: Env): boolean {
  return Boolean(
    env.YOUCAM_API_KEY &&
      env.YOUCAM_API_KEY.length >= 8 &&
      env.YOUCAM_API_KEY.length <= 4_096 &&
      !/[\r\n]/.test(env.YOUCAM_API_KEY)
  );
}

export function hasSecureJudgeCode(env: Env): boolean {
  return Boolean(
    env.JUDGE_ACCESS_CODE &&
      env.JUDGE_ACCESS_CODE.length >= 8 &&
      env.JUDGE_ACCESS_CODE.length <= 512 &&
      !/[\r\n]/.test(env.JUDGE_ACCESS_CODE)
  );
}
