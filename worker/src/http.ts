import { MAX_JSON_BYTES } from "./config";
import { ApiError } from "./errors";

const REQUEST_ID = /^[A-Za-z0-9_-]{8,64}$/;

export function requestIdFor(request: Request): string {
  const supplied = request.headers.get("x-request-id");
  return supplied && REQUEST_ID.test(supplied) ? supplied : crypto.randomUUID();
}

export function clientKey(request: Request): string {
  const ip = request.headers.get("cf-connecting-ip")?.trim();
  if (ip && ip.length <= 64 && /^[0-9A-Fa-f:.]+$/.test(ip)) return ip;
  return "unknown";
}

export async function readJson(request: Request): Promise<unknown> {
  const contentType = request.headers.get("content-type")?.split(";", 1)[0]?.trim().toLowerCase();
  if (contentType !== "application/json") {
    throw new ApiError(415, "unsupported_media_type", "Content-Type must be application/json.");
  }

  const declared = Number(request.headers.get("content-length"));
  if (Number.isFinite(declared) && declared > MAX_JSON_BYTES) {
    throw new ApiError(413, "body_too_large", "The JSON request body is too large.");
  }

  const text = await request.text();
  if (new TextEncoder().encode(text).byteLength > MAX_JSON_BYTES) {
    throw new ApiError(413, "body_too_large", "The JSON request body is too large.");
  }
  if (!text.trim()) throw new ApiError(400, "invalid_json", "A JSON request body is required.");

  try {
    return JSON.parse(text) as unknown;
  } catch {
    throw new ApiError(400, "invalid_json", "The request body is not valid JSON.");
  }
}

export function json(data: unknown, status = 200, headers?: HeadersInit): Response {
  const responseHeaders = new Headers(headers);
  responseHeaders.set("content-type", "application/json; charset=utf-8");
  return new Response(JSON.stringify(data), { status, headers: responseHeaders });
}

export function allowedOrigin(request: Request, allowedOrigins: ReadonlySet<string>): string | null {
  const origin = request.headers.get("origin");
  if (!origin) return null;
  if (!allowedOrigins.has(origin)) {
    throw new ApiError(403, "origin_not_allowed", "This web origin is not allowed.");
  }
  return origin;
}

export function corsHeaders(origin: string | null): Headers {
  const headers = new Headers();
  if (origin) {
    headers.set("access-control-allow-origin", origin);
    headers.set("access-control-allow-credentials", "false");
    headers.set("vary", "Origin");
  }
  headers.set("access-control-allow-methods", "GET, POST, PUT, OPTIONS");
  headers.set(
    "access-control-allow-headers",
    "Authorization, Content-Type, Content-Length, X-DrapeProof-Upload-Token, X-Request-Id"
  );
  headers.set("access-control-max-age", "600");
  return headers;
}

export function withSecurityHeaders(response: Response, requestId: string, origin: string | null): Response {
  const headers = new Headers(response.headers);
  for (const [name, value] of corsHeaders(origin)) headers.set(name, value);
  headers.set("cache-control", "no-store");
  headers.set("content-security-policy", "default-src 'none'; frame-ancestors 'none'");
  headers.set("referrer-policy", "no-referrer");
  headers.set("x-content-type-options", "nosniff");
  headers.set("x-frame-options", "DENY");
  headers.set("x-request-id", requestId);
  return new Response(response.body, { status: response.status, statusText: response.statusText, headers });
}
