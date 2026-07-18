import { ApiError } from "./errors";
import type { Env, SessionClaims } from "./types";

const encoder = new TextEncoder();
const decoder = new TextDecoder();

function bytesToBase64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function base64UrlToBytes(value: string): ArrayBuffer {
  if (!/^[A-Za-z0-9_-]+$/.test(value) || value.length % 4 === 1) {
    throw new Error("Invalid base64url");
  }
  const padding = "=".repeat((4 - (value.length % 4)) % 4);
  const binary = atob(value.replace(/-/g, "+").replace(/_/g, "/") + padding);
  const buffer = new ArrayBuffer(binary.length);
  const bytes = new Uint8Array(buffer);
  for (let index = 0; index < binary.length; index += 1) bytes[index] = binary.charCodeAt(index);
  if (bytesToBase64Url(bytes) !== value) throw new Error("Non-canonical base64url");
  return buffer;
}

async function signingKey(env: Env): Promise<CryptoKey> {
  const source = env.SESSION_SECRET?.trim() || env.YOUCAM_API_KEY;
  const derived = await crypto.subtle.digest(
    "SHA-256",
    encoder.encode(`drapeproof/session/v1\0${source}`)
  );
  return crypto.subtle.importKey(
    "raw",
    derived,
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign", "verify"]
  );
}

export async function issueSessionToken(
  env: Env,
  ttlSeconds: number,
  nowSeconds = Math.floor(Date.now() / 1_000)
): Promise<{ token: string; expiresAt: number }> {
  const claims: SessionClaims = {
    v: 1,
    iat: nowSeconds,
    exp: nowSeconds + ttlSeconds,
    jti: crypto.randomUUID(),
    aud: "drapeproof-api"
  };
  const payload = bytesToBase64Url(encoder.encode(JSON.stringify(claims)));
  const signature = await crypto.subtle.sign("HMAC", await signingKey(env), encoder.encode(payload));
  return { token: `${payload}.${bytesToBase64Url(new Uint8Array(signature))}`, expiresAt: claims.exp };
}

export async function verifySessionToken(
  token: string,
  env: Env,
  nowSeconds = Math.floor(Date.now() / 1_000)
): Promise<SessionClaims> {
  if (token.length > 2_048) throw new ApiError(401, "unauthorized", "A valid session is required.");
  const parts = token.split(".");
  if (parts.length !== 2 || !parts[0] || !parts[1]) {
    throw new ApiError(401, "unauthorized", "A valid session is required.");
  }

  try {
    const valid = await crypto.subtle.verify(
      "HMAC",
      await signingKey(env),
      base64UrlToBytes(parts[1]),
      encoder.encode(parts[0])
    );
    if (!valid) throw new Error("Invalid signature");

    const parsed: unknown = JSON.parse(decoder.decode(base64UrlToBytes(parts[0])));
    if (!isSessionClaims(parsed) || parsed.exp <= nowSeconds || parsed.iat > nowSeconds + 60) {
      throw new Error("Invalid claims");
    }
    return parsed;
  } catch {
    throw new ApiError(401, "unauthorized", "A valid session is required.");
  }
}

function isSessionClaims(value: unknown): value is SessionClaims {
  if (!value || typeof value !== "object") return false;
  const candidate = value as Record<string, unknown>;
  return (
    candidate.v === 1 &&
    candidate.aud === "drapeproof-api" &&
    typeof candidate.iat === "number" &&
    Number.isSafeInteger(candidate.iat) &&
    typeof candidate.exp === "number" &&
    Number.isSafeInteger(candidate.exp) &&
    candidate.exp > candidate.iat &&
    candidate.exp - candidate.iat <= 3_600 &&
    typeof candidate.jti === "string" &&
    /^[0-9a-f-]{36}$/i.test(candidate.jti)
  );
}

export async function verifyJudgeCode(provided: string, env: Env): Promise<boolean> {
  if (!env.JUDGE_ACCESS_CODE) return false;
  if (!provided || provided.length > 512) return false;
  const key = await signingKey(env);
  const expected = await crypto.subtle.sign("HMAC", key, encoder.encode(env.JUDGE_ACCESS_CODE));
  return crypto.subtle.verify("HMAC", key, expected, encoder.encode(provided));
}

export function bearerToken(request: Request): string {
  const header = request.headers.get("authorization") ?? "";
  const match = /^Bearer ([A-Za-z0-9._-]+)$/.exec(header);
  if (!match?.[1]) throw new ApiError(401, "unauthorized", "A valid session is required.");
  return match[1];
}
