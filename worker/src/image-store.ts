import { MAX_IMAGE_BYTES } from "./config";
import { ApiError } from "./errors";
import type { Env, UploadFileInput } from "./types";

const encoder = new TextEncoder();
const FILE_ID = /^[a-f0-9]{32}$/;
const TOKEN = /^(\d{10})\.([A-Za-z0-9_-]{40,64})$/;

interface MediaMetadata {
  contentType: string;
  fileSize: number;
  createdAt: number;
  expiresAt: number;
}

export interface LocalUploadTicket {
  fileId: string;
  fileName: string;
  contentType: string;
  upload: {
    method: "PUT";
    url: string;
    headers: Record<string, string>;
  };
}

function requireStore(env: Env): R2Bucket {
  if (!env.IMAGE_STORE) {
    throw new ApiError(
      503,
      "image_store_not_configured",
      "Private image upload is not configured for the Scarf provider."
    );
  }
  return env.IMAGE_STORE;
}

async function mediaKey(env: Env): Promise<CryptoKey> {
  const source = env.SESSION_SECRET?.trim() || env.YOUCAM_API_KEY;
  const derived = await crypto.subtle.digest(
    "SHA-256",
    encoder.encode(`drapeproof/media/v1\0${source}`)
  );
  return crypto.subtle.importKey(
    "raw",
    derived,
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign", "verify"]
  );
}

function signatureToBase64Url(signature: ArrayBuffer): string {
  let binary = "";
  for (const byte of new Uint8Array(signature)) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function base64UrlToBuffer(value: string): ArrayBuffer {
  const padding = "=".repeat((4 - (value.length % 4)) % 4);
  const binary = atob(value.replace(/-/g, "+").replace(/_/g, "/") + padding);
  const buffer = new ArrayBuffer(binary.length);
  const bytes = new Uint8Array(buffer);
  for (let index = 0; index < binary.length; index += 1) bytes[index] = binary.charCodeAt(index);
  return buffer;
}

function canonical(
  purpose: "upload" | "read",
  fileId: string,
  expiresAt: number,
  contentType: string,
  fileSize: number
): string {
  return `${purpose}\n${fileId}\n${expiresAt}\n${contentType}\n${fileSize}`;
}

async function issueToken(
  purpose: "upload" | "read",
  fileId: string,
  metadata: Pick<MediaMetadata, "contentType" | "fileSize">,
  expiresAt: number,
  env: Env
): Promise<string> {
  const signature = await crypto.subtle.sign(
    "HMAC",
    await mediaKey(env),
    encoder.encode(canonical(purpose, fileId, expiresAt, metadata.contentType, metadata.fileSize))
  );
  return `${expiresAt}.${signatureToBase64Url(signature)}`;
}

async function verifyToken(
  token: string,
  purpose: "upload" | "read",
  fileId: string,
  metadata: Pick<MediaMetadata, "contentType" | "fileSize">,
  env: Env,
  nowSeconds = Math.floor(Date.now() / 1_000)
): Promise<boolean> {
  const match = TOKEN.exec(token);
  if (!match?.[1] || !match[2]) return false;
  const expiresAt = Number(match[1]);
  if (!Number.isSafeInteger(expiresAt) || expiresAt <= nowSeconds || expiresAt > nowSeconds + 3_600) {
    return false;
  }
  try {
    return crypto.subtle.verify(
      "HMAC",
      await mediaKey(env),
      base64UrlToBuffer(match[2]),
      encoder.encode(canonical(purpose, fileId, expiresAt, metadata.contentType, metadata.fileSize))
    );
  } catch {
    return false;
  }
}

export async function createLocalUploadTickets(
  request: Request,
  files: UploadFileInput[],
  env: Env,
  ticketTtlSeconds: number
): Promise<LocalUploadTicket[]> {
  requireStore(env);
  const origin = new URL(request.url).origin;
  const expiresAt = Math.floor(Date.now() / 1_000) + ticketTtlSeconds;

  return Promise.all(
    files.map(async (file) => {
      const fileId = crypto.randomUUID().replace(/-/g, "");
      const token = await issueToken("upload", fileId, file, expiresAt, env);
      return {
        fileId,
        fileName: file.fileName,
        contentType: file.contentType,
        upload: {
          method: "PUT" as const,
          url: `${origin}/v1/media/${fileId}`,
          headers: {
            "Content-Type": file.contentType,
            "Content-Length": String(file.fileSize),
            "X-DrapeProof-Upload-Token": token
          }
        }
      };
    })
  );
}

export async function storeLocalUpload(
  request: Request,
  fileId: string,
  env: Env,
  mediaTtlSeconds: number
): Promise<Response> {
  const store = requireStore(env);
  if (!FILE_ID.test(fileId)) throw new ApiError(404, "not_found", "The upload ticket is invalid.");
  const contentType = request.headers.get("content-type")?.split(";", 1)[0]?.trim().toLowerCase() ?? "";
  if (!new Set(["image/jpeg", "image/jpg", "image/png"]).has(contentType)) {
    throw new ApiError(415, "unsupported_media_type", "The upload content type is invalid.");
  }
  const fileSize = Number(request.headers.get("content-length"));
  if (!Number.isSafeInteger(fileSize) || fileSize <= 0 || fileSize > MAX_IMAGE_BYTES) {
    throw new ApiError(411, "invalid_content_length", "A valid Content-Length is required.");
  }
  const token = request.headers.get("x-drapeproof-upload-token") ?? "";
  if (!(await verifyToken(token, "upload", fileId, { contentType, fileSize }, env))) {
    throw new ApiError(401, "invalid_upload_ticket", "The upload ticket is invalid or expired.");
  }

  const bytes = await request.arrayBuffer();
  if (bytes.byteLength !== fileSize) {
    throw new ApiError(400, "content_length_mismatch", "The uploaded byte length does not match the ticket.");
  }
  if (!matchesImageSignature(bytes, contentType)) {
    throw new ApiError(400, "invalid_image_content", "The uploaded bytes do not match the image type.");
  }
  const metadata: MediaMetadata = {
    contentType,
    fileSize,
    createdAt: Math.floor(Date.now() / 1_000),
    expiresAt: Math.floor(Date.now() / 1_000) + mediaTtlSeconds
  };
  await store.put(`media/${fileId}`, bytes, {
    httpMetadata: { contentType },
    customMetadata: {
      fileSize: String(fileSize),
      createdAt: String(metadata.createdAt),
      expiresAt: String(metadata.expiresAt)
    }
  });
  return new Response(null, { status: 204 });
}

function matchesImageSignature(bytes: ArrayBuffer, contentType: string): boolean {
  const view = new Uint8Array(bytes);
  if (contentType === "image/jpeg" || contentType === "image/jpg") {
    return view.length >= 3 && view[0] === 0xff && view[1] === 0xd8 && view[2] === 0xff;
  }
  const png = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];
  return view.length >= png.length && png.every((byte, index) => view[index] === byte);
}

function metadataFor(object: R2Object): MediaMetadata | null {
  const contentType = object.httpMetadata?.contentType;
  const fileSize = Number(object.customMetadata?.fileSize);
  const createdAt = Number(object.customMetadata?.createdAt);
  const expiresAt = Number(object.customMetadata?.expiresAt);
  if (
    !contentType ||
    !new Set(["image/jpeg", "image/jpg", "image/png"]).has(contentType) ||
    !Number.isSafeInteger(fileSize) ||
    fileSize <= 0 ||
    object.size !== fileSize ||
    !Number.isSafeInteger(createdAt) ||
    !Number.isSafeInteger(expiresAt) ||
    expiresAt <= createdAt
  ) {
    return null;
  }
  return { contentType, fileSize, createdAt, expiresAt };
}

async function headStoredMedia(fileId: string, env: Env): Promise<MediaMetadata> {
  const store = requireStore(env);
  if (!FILE_ID.test(fileId)) throw new ApiError(404, "media_not_found", "The image is unavailable.");
  const key = `media/${fileId}`;
  const object = await store.head(key);
  const metadata = object ? metadataFor(object) : null;
  if (!metadata) {
    throw new ApiError(404, "media_not_found", "The image is unavailable.");
  }
  if (metadata.expiresAt <= Math.floor(Date.now() / 1_000)) {
    await store.delete(key);
    throw new ApiError(404, "media_not_found", "The image is unavailable.");
  }
  return metadata;
}

export async function signedMediaUrl(
  fileId: string,
  requestOrigin: string,
  env: Env,
  readTtlSeconds: number
): Promise<string> {
  const metadata = await headStoredMedia(fileId, env);
  const expiresAt = Math.floor(Date.now() / 1_000) + readTtlSeconds;
  const token = await issueToken("read", fileId, metadata, expiresAt, env);
  return `${requestOrigin}/media/${fileId}?token=${encodeURIComponent(token)}`;
}

export async function serveLocalMedia(
  request: Request,
  fileId: string,
  env: Env
): Promise<Response> {
  const store = requireStore(env);
  if (!FILE_ID.test(fileId)) throw new ApiError(404, "media_not_found", "The image is unavailable.");
  const key = `media/${fileId}`;
  const object = await store.get(key);
  const metadata = object ? metadataFor(object) : null;
  if (!object || !metadata) {
    throw new ApiError(404, "media_not_found", "The image is unavailable.");
  }
  if (metadata.expiresAt <= Math.floor(Date.now() / 1_000)) {
    await store.delete(key);
    throw new ApiError(404, "media_not_found", "The image is unavailable.");
  }
  const token = new URL(request.url).searchParams.get("token") ?? "";
  if (!(await verifyToken(token, "read", fileId, metadata, env))) {
    throw new ApiError(404, "media_not_found", "The image is unavailable.");
  }
  return new Response(object.body, {
    status: 200,
    headers: {
      "content-type": metadata.contentType,
      "content-length": String(metadata.fileSize),
      "cache-control": "no-store, private",
      "x-content-type-options": "nosniff"
    }
  });
}
