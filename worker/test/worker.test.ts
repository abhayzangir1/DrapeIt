import { beforeEach, describe, expect, it, vi } from "vitest";
import { issueSessionToken } from "../src/auth";
import { resetBudgetForTest } from "../src/budget";
import { createHandler } from "../src/index";
import {
  createLocalUploadTickets,
  signedMediaUrl,
  storeLocalUpload
} from "../src/image-store";
import { resetMemoryCountersForTest } from "../src/rate-limit";
import type { Env, Fetcher } from "../src/types";
import { memoryPaidLedgerNamespace } from "./memory-paid-ledger";

const API_KEY = "test-youcam-secret-api-key";
const TASK_ID = "task_1234567890abcdef";
const FILE_ID = "file/AbCdEfGhIjKlMnOp+123=";
const OPERATION_ID = "12345678-1234-4123-8123-123456789abc";

function env(overrides: Partial<Env> = {}): Env {
  const value: Env = {
    YOUCAM_API_KEY: API_KEY,
    JUDGE_ACCESS_CODE: "test-judge-code",
    DRAPEPROOF_STATE: memoryStateKv(),
    CREDIT_FLOOR: "300",
    VTO_PROVIDER: "clothes",
    RATE_LIMIT_SESSION: "100",
    RATE_LIMIT_UPLOAD: "100",
    RATE_LIMIT_TASK: "100",
    RATE_LIMIT_POLL: "100",
    MAX_TASKS_PER_DAY: "100",
    ...overrides
  };
  if (!("PAID_TASK_LEDGER" in overrides)) {
    value.PAID_TASK_LEDGER = memoryPaidLedgerNamespace(value);
  }
  return value;
}

async function auth(envValue: Env): Promise<string> {
  const { token } = await issueSessionToken(envValue, 1_800);
  return `Bearer ${token}`;
}

async function request(
  handler: ReturnType<typeof createHandler>,
  envValue: Env,
  path: string,
  options: {
    method?: string;
    body?: unknown;
    token?: string;
    headers?: Record<string, string>;
  } = {}
): Promise<Response> {
  const headers = new Headers(options.headers);
  headers.set("cf-connecting-ip", "203.0.113.8");
  let body = options.body;
  if (
    options.method !== "GET" &&
    (path === "/v1/tasks/facial-colors" || path === "/v1/tasks/try-on") &&
    body && typeof body === "object" && !Array.isArray(body) && !("operationId" in body)
  ) {
    body = { operationId: crypto.randomUUID(), ...body };
  }
  if (body !== undefined) headers.set("content-type", "application/json");
  if (options.token) headers.set("authorization", options.token);
  return handler(
    new Request(`https://api.example.test${path}`, {
      method: options.method ?? (body === undefined ? "GET" : "POST"),
      headers,
      ...(body === undefined ? {} : { body: JSON.stringify(body) })
    }),
    envValue
  );
}

function jsonResponse(body: unknown, status = 200, headers?: HeadersInit): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json", ...Object.fromEntries(new Headers(headers)) }
  });
}

function featureCostResponse(path: string, amount = 2, nextToken: string | null = null): Response {
  return jsonResponse({
    status: 200,
    result: {
      next_token: nextToken,
      skus: [{ description: "feature", amount, unit: "image", proc_unit: 1, run_task_url: path }]
    }
  });
}

function queuedFetch(responses: Response[]) {
  const calls: Array<{ url: string; init?: RequestInit }> = [];
  const fetcher: Fetcher = async (input, init) => {
    calls.push({ url: String(input), ...(init ? { init } : {}) });
    const response = responses.shift();
    if (!response) throw new Error("Unexpected upstream request");
    return response;
  };
  return { fetcher, calls };
}

function memoryR2(): R2Bucket {
  const values = new Map<
    string,
    { value: ArrayBuffer; httpMetadata: R2HTTPMetadata; customMetadata: Record<string, string> }
  >();
  const objectFor = (key: string, entry: NonNullable<ReturnType<typeof values.get>>) => ({
    key,
    version: "test-version",
    size: entry.value.byteLength,
    etag: "test-etag",
    httpEtag: '"test-etag"',
    uploaded: new Date(),
    storageClass: "Standard",
    httpMetadata: entry.httpMetadata,
    customMetadata: entry.customMetadata,
    range: undefined,
    checksums: {},
    writeHttpMetadata: vi.fn()
  });
  return {
    put: vi.fn(async (key: string, value: ArrayBuffer, options?: R2PutOptions) => {
      const entry = {
        value: value.slice(0),
        httpMetadata: (options?.httpMetadata ?? {}) as R2HTTPMetadata,
        customMetadata: options?.customMetadata ?? {}
      };
      values.set(key, entry);
      return objectFor(key, entry);
    }),
    head: vi.fn(async (key: string) => {
      const entry = values.get(key);
      return entry ? objectFor(key, entry) : null;
    }),
    get: vi.fn(async (key: string) => {
      const entry = values.get(key);
      if (!entry) return null;
      const header = objectFor(key, entry);
      const copy = entry.value.slice(0);
      return {
        ...header,
        body: new Response(copy).body,
        bodyUsed: false,
        arrayBuffer: async () => copy.slice(0),
        text: async () => new TextDecoder().decode(copy),
        json: async () => JSON.parse(new TextDecoder().decode(copy)),
        blob: async () => new Blob([copy], { type: entry.httpMetadata.contentType ?? "" })
      };
    }),
    delete: vi.fn(async (key: string) => {
      values.delete(key);
    })
  } as unknown as R2Bucket;
}

function memoryStateKv(): KVNamespace {
  const values = new Map<string, string>();
  return {
    get: vi.fn(async (key: string) => values.get(key) ?? null),
    put: vi.fn(async (key: string, value: string) => {
      values.set(key, value);
    }),
    delete: vi.fn(async (key: string) => {
      values.delete(key);
    }),
    list: vi.fn(async () => ({ keys: [], list_complete: true, cacheStatus: null }))
  } as unknown as KVNamespace;
}

beforeEach(() => {
  resetMemoryCountersForTest();
  resetBudgetForTest();
  vi.restoreAllMocks();
});

describe("health, CORS, and sessions", () => {
  it("reports readiness without exposing credentials", async () => {
    const envValue = env();
    const response = await request(createHandler(), envValue, "/healthz");
    const text = await response.text();

    expect(response.status).toBe(200);
    expect(JSON.parse(text)).toMatchObject({
      status: "ok",
      service: "drapeproof-api",
      vtoProvider: "clothes",
      vtoProviderConfigured: true
    });
    expect(text).not.toContain(API_KEY);
    expect(response.headers.get("cache-control")).toBe("no-store");
    expect(response.headers.get("x-content-type-options")).toBe("nosniff");
  });

  it("fails health when Scarf was selected without verified paths", async () => {
    const response = await request(createHandler(), env({ VTO_PROVIDER: "scarf" }), "/healthz");
    expect(response.status).toBe(503);
    expect(await response.json()).toMatchObject({
      status: "degraded",
      vtoProvider: "scarf",
      vtoProviderConfigured: false
    });
  });

  it("degrades health and fails closed without the access gate and state KV", async () => {
    const envValue = env();
    delete envValue.JUDGE_ACCESS_CODE;
    delete envValue.DRAPEPROOF_STATE;
    const response = await request(createHandler(), envValue, "/healthz");
    expect(response.status).toBe(503);
    expect(await response.json()).toMatchObject({
      status: "degraded",
      accessGateConfigured: false,
      stateStoreConfigured: false
    });
  });

  it("uses Scarf as the default provider when no override is present", async () => {
    const envValue = env({ IMAGE_STORE: memoryR2() });
    delete envValue.VTO_PROVIDER;
    const response = await request(
      createHandler(),
      envValue,
      "/healthz"
    );
    expect(response.status).toBe(200);
    expect(await response.json()).toMatchObject({ status: "ok", vtoProvider: "scarf" });
  });

  it("denies unlisted browser origins", async () => {
    const response = await request(createHandler(), env(), "/healthz", {
      headers: { origin: "https://evil.example" }
    });
    expect(response.status).toBe(403);
    expect(response.headers.get("access-control-allow-origin")).toBeNull();
    expect(await response.json()).toMatchObject({ error: { code: "origin_not_allowed" } });
  });

  it("returns exact-origin preflight headers", async () => {
    const response = await request(
      createHandler(),
      env({ ALLOWED_ORIGINS: "https://judge.example" }),
      "/v1/uploads",
      { method: "OPTIONS", headers: { origin: "https://judge.example" } }
    );
    expect(response.status).toBe(204);
    expect(response.headers.get("access-control-allow-origin")).toBe("https://judge.example");
    expect(response.headers.get("vary")).toContain("Origin");
  });

  it("requires the judge code and issues a usable token", async () => {
    const envValue = env({ JUDGE_ACCESS_CODE: "drape-judge" });
    const handler = createHandler();
    const denied = await request(handler, envValue, "/v1/session", {
      body: { accessCode: "wrong" }
    });
    expect(denied.status).toBe(401);

    const created = await request(handler, envValue, "/v1/session", {
      body: { accessCode: "drape-judge" }
    });
    const session = (await created.json()) as { token: string; expiresInSeconds: number };
    expect(created.status).toBe(201);
    expect(session.token).toMatch(/^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/);
    expect(session.expiresInSeconds).toBe(1_800);

    const protectedResponse = await request(handler, envValue, "/v1/credits", {
      token: `Bearer ${session.token}`
    });
    expect(protectedResponse.status).not.toBe(401);
  });

  it("refuses session issuance when the code or persistent state is missing", async () => {
    const noCode = env();
    const previouslyIssued = await auth(noCode);
    delete noCode.JUDGE_ACCESS_CODE;
    const missingCode = await request(createHandler(), noCode, "/v1/session", {
      body: { accessCode: "test-judge-code" }
    });
    expect(missingCode.status).toBe(503);
    expect(await missingCode.json()).toMatchObject({ error: { code: "access_gate_not_configured" } });
    const protectedRoute = await request(createHandler(), noCode, "/v1/credits", {
      token: previouslyIssued
    });
    expect(protectedRoute.status).toBe(503);
    expect(await protectedRoute.json()).toMatchObject({ error: { code: "access_gate_not_configured" } });

    const noState = env();
    delete noState.DRAPEPROOF_STATE;
    const missingState = await request(createHandler(), noState, "/v1/session", {
      body: { accessCode: "test-judge-code" }
    });
    expect(missingState.status).toBe(503);
    expect(await missingState.json()).toMatchObject({ error: { code: "state_store_not_configured" } });
  });

  it("rejects non-canonically encoded and missing sessions", async () => {
    const envValue = env();
    const valid = await auth(envValue);
    const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    const finalIndex = alphabet.indexOf(valid.at(-1) ?? "");
    const alias = alphabet[finalIndex + 1];
    if (finalIndex < 0 || finalIndex % 4 !== 0 || !alias) {
      throw new Error("The issued HMAC did not have canonical 32-byte base64url encoding.");
    }
    // A permissive decoder ignores the last character's two unused bits, so this alternate
    // spelling decodes to the same HMAC bytes. The serialized token must still fail closed.
    const tampered = `${valid.slice(0, -1)}${alias}`;
    const handler = createHandler();

    expect((await request(handler, envValue, "/v1/credits")).status).toBe(401);
    expect((await request(handler, envValue, "/v1/credits", { token: tampered })).status).toBe(401);
  });

  it("uses the KV-compatible limiter and returns Retry-After", async () => {
    const values = new Map<string, string>();
    const fakeKv = {
      get: vi.fn(async (key: string) => values.get(key) ?? null),
      put: vi.fn(async (key: string, value: string) => {
        values.set(key, value);
      })
    } as unknown as KVNamespace;
    const envValue = env({ RATE_LIMIT_SESSION: "1", DRAPEPROOF_STATE: fakeKv });
    const handler = createHandler();

    expect(
      (await request(handler, envValue, "/v1/session", { body: { accessCode: "test-judge-code" } })).status
    ).toBe(201);
    const limited = await request(handler, envValue, "/v1/session", {
      body: { accessCode: "test-judge-code" }
    });
    expect(limited.status).toBe(429);
    expect(limited.headers.get("retry-after")).toMatch(/^\d+$/);
    expect((fakeKv.put as unknown as ReturnType<typeof vi.fn>)).toHaveBeenCalled();
  });
});

describe("strict request validation and upload tickets", () => {
  it("requires JSON and rejects unknown fields", async () => {
    const envValue = env();
    const token = await auth(envValue);
    const handler = createHandler();
    const noType = await handler(
      new Request("https://api.example.test/v1/uploads", {
        method: "POST",
        headers: { authorization: token },
        body: "{}"
      }),
      envValue
    );
    expect(noType.status).toBe(415);

    const unknown = await request(handler, envValue, "/v1/uploads", {
      token,
      body: {
        feature: "facial-colors",
        files: [{ contentType: "image/jpeg", fileName: "face.jpg", fileSize: 1000 }],
        upstreamPath: "/steal"
      }
    });
    expect(unknown.status).toBe(400);
    expect(await unknown.json()).toMatchObject({ error: { code: "validation_error" } });
  });

  it("rejects PNG and multiple files for facial color analysis", async () => {
    const envValue = env();
    const token = await auth(envValue);
    const response = await request(createHandler(), envValue, "/v1/uploads", {
      token,
      body: {
        feature: "facial-colors",
        files: [{ contentType: "image/png", fileName: "face.png", fileSize: 1000 }]
      }
    });
    expect(response.status).toBe(400);
    expect(JSON.stringify(await response.json())).toContain("JPEG files only");
  });

  it("rejects mismatched extensions and files at or above 10 MB", async () => {
    const envValue = env();
    const token = await auth(envValue);
    const handler = createHandler();
    const mismatch = await request(handler, envValue, "/v1/uploads", {
      token,
      body: {
        feature: "try-on",
        files: [{ contentType: "image/png", fileName: "garment.jpg", fileSize: 1000 }]
      }
    });
    expect(mismatch.status).toBe(400);

    const large = await request(handler, envValue, "/v1/uploads", {
      token,
      body: {
        feature: "try-on",
        files: [{ contentType: "image/png", fileName: "garment.png", fileSize: 10 * 1024 * 1024 }]
      }
    });
    expect(large.status).toBe(400);
  });

  it("maps upload metadata and returns only a safe presigned ticket", async () => {
    const { fetcher, calls } = queuedFetch([
      jsonResponse({
        status: 200,
        data: {
          files: [
            {
              file_id: FILE_ID,
              requests: [
                {
                  method: "PUT",
                  url: "https://yce-us.s3-accelerate.amazonaws.com/demo/signed?sig=abc",
                  headers: {
                    "Content-Type": "image/jpg",
                    "Content-Length": "1234",
                    "X-Amz-Security-Token": "upload-only",
                    Authorization: "must-not-pass",
                    Cookie: "must-not-pass"
                  }
                }
              ]
            }
          ]
        }
      })
    ]);
    const envValue = env();
    const token = await auth(envValue);
    const response = await request(createHandler(fetcher), envValue, "/v1/uploads", {
      token,
      body: {
        feature: "facial-colors",
        files: [{ contentType: "image/jpg", fileName: "face.jpg", fileSize: 1234 }]
      }
    });
    const body = await response.json();
    const serialized = JSON.stringify(body);

    expect(response.status).toBe(201);
    expect(calls[0]?.url.endsWith("/s2s/v2.0/file/skin-tone-analysis")).toBe(true);
    expect(new Headers(calls[0]?.init?.headers).get("authorization")).toBe(`Bearer ${API_KEY}`);
    expect(serialized).not.toContain(API_KEY);
    expect(serialized).not.toContain("must-not-pass");
    expect(body).toMatchObject({
      feature: "facial-colors",
      files: [{ fileId: FILE_ID, upload: { method: "PUT" } }]
    });
  });

  it("rejects an untrusted upload host returned by upstream", async () => {
    const { fetcher } = queuedFetch([
      jsonResponse({
        status: 200,
        data: {
          files: [{ file_id: FILE_ID, requests: [{ method: "PUT", url: "https://evil.example/upload" }] }]
        }
      })
    ]);
    const envValue = env();
    const response = await request(createHandler(fetcher), envValue, "/v1/uploads", {
      token: await auth(envValue),
      body: {
        feature: "try-on",
        files: [{ contentType: "image/png", fileName: "top.png", fileSize: 1000 }]
      }
    });
    expect(response.status).toBe(502);
    expect(await response.json()).toMatchObject({ error: { code: "invalid_upstream_response" } });
  });
});

describe("private Scarf media adapter", () => {
  it("stores signed uploads and supplies short-lived fetchable URLs to YouCam", async () => {
    const { fetcher, calls } = queuedFetch([
      featureCostResponse("/s2s/v2.0/task/scarf", 2),
      jsonResponse({ status: 200, data: { task_id: TASK_ID } })
    ]);
    const imageStore = memoryR2();
    const envValue = env({ VTO_PROVIDER: "scarf", IMAGE_STORE: imageStore });
    const handler = createHandler(fetcher);
    const token = await auth(envValue);
    const source = new Uint8Array([0xff, 0xd8, 0xff, 0xd9]);
    const reference = new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);

    const ticketsResponse = await request(handler, envValue, "/v1/uploads", {
      token,
      body: {
        feature: "try-on",
        files: [
          { contentType: "image/jpeg", fileName: "person.jpg", fileSize: source.byteLength },
          { contentType: "image/png", fileName: "scarf.png", fileSize: reference.byteLength }
        ]
      }
    });
    const ticketBody = (await ticketsResponse.json()) as {
      files: Array<{
        fileId: string;
        upload: { url: string; headers: Record<string, string> };
      }>;
    };
    expect(ticketsResponse.status).toBe(201);
    expect(calls).toHaveLength(0);
    expect(ticketBody.files).toHaveLength(2);
    expect(ticketBody.files[0]?.fileId).toMatch(/^[a-f0-9]{32}$/);

    for (const [index, bytes] of [source, reference].entries()) {
      const ticket = ticketBody.files[index];
      expect(ticket).toBeDefined();
      const uploaded = await handler(
        new Request(ticket!.upload.url, {
          method: "PUT",
          headers: ticket!.upload.headers,
          body: bytes
        }),
        envValue
      );
      expect(uploaded.status).toBe(204);
    }

    const taskResponse = await request(handler, envValue, "/v1/tasks/try-on", {
      token,
      body: {
        sourceFileId: ticketBody.files[0]?.fileId,
        referenceFileId: ticketBody.files[1]?.fileId,
        gender: "male",
        style: "style_modern_chic"
      }
    });
    const taskBody = await taskResponse.json();
    expect(taskResponse.status).toBe(202);
    expect(taskBody).toMatchObject({ provider: "scarf", reservedUnitCost: 2 });

    const upstreamPayload = JSON.parse(String(calls[1]?.init?.body)) as {
      src_file_url: string;
      ref_file_url: string;
      gender: string;
      style: string;
    };
    expect(upstreamPayload.gender).toBe("male");
    expect(upstreamPayload.style).toBe("style_modern_chic");
    expect(upstreamPayload.src_file_url).toMatch(/^https:\/\/api\.example\.test\/media\/[a-f0-9]{32}\?token=/);
    expect(JSON.stringify(taskBody)).not.toContain("/media/");

    const fetchedSource = await handler(new Request(upstreamPayload.src_file_url), envValue);
    expect(fetchedSource.status).toBe(200);
    expect(fetchedSource.headers.get("content-type")).toBe("image/jpeg");
    expect(new Uint8Array(await fetchedSource.arrayBuffer())).toEqual(source);

    const invalid = new URL(upstreamPayload.ref_file_url);
    invalid.searchParams.set("token", "0000000000.invalid");
    expect((await handler(new Request(invalid), envValue)).status).toBe(404);
  });

  it("binds an upload ticket to exact content type and byte length", async () => {
    const imageStore = memoryR2();
    const envValue = env({ VTO_PROVIDER: "scarf", IMAGE_STORE: imageStore });
    const handler = createHandler();
    const response = await request(handler, envValue, "/v1/uploads", {
      token: await auth(envValue),
      body: {
        feature: "try-on",
        files: [{ contentType: "image/jpeg", fileName: "person.jpg", fileSize: 4 }]
      }
    });
    const body = (await response.json()) as {
      files: Array<{ upload: { url: string; headers: Record<string, string> } }>;
    };
    const ticket = body.files[0]!;
    const alteredHeaders = new Headers(ticket.upload.headers);
    alteredHeaders.set("content-type", "image/png");
    const rejected = await handler(
      new Request(ticket.upload.url, {
        method: "PUT",
        headers: alteredHeaders,
        body: new Uint8Array([0, 1, 2, 3])
      }),
      envValue
    );
    expect(rejected.status).toBe(401);
    expect(await rejected.json()).toMatchObject({ error: { code: "invalid_upload_ticket" } });

    const disguised = await handler(
      new Request(ticket.upload.url, {
        method: "PUT",
        headers: ticket.upload.headers,
        body: new Uint8Array([0, 1, 2, 3])
      }),
      envValue
    );
    expect(disguised.status).toBe(400);
    expect(await disguised.json()).toMatchObject({ error: { code: "invalid_image_content" } });
  });

  it("logically expires after 24 hours and deletes an expired R2 object on access", async () => {
    vi.useFakeTimers();
    try {
      const startedAt = new Date("2026-07-17T12:00:00.000Z");
      vi.setSystemTime(startedAt);
      const imageStore = memoryR2();
      const envValue = env({ VTO_PROVIDER: "scarf", IMAGE_STORE: imageStore });
      const files = [{
        contentType: "image/jpeg" as const,
        fileName: "person.jpg",
        fileSize: 4
      }];
      const tickets = await createLocalUploadTickets(
        new Request("https://api.example.test/v1/uploads"),
        files,
        envValue,
        600
      );
      const ticket = tickets[0]!;
      await storeLocalUpload(
        new Request(ticket.upload.url, {
          method: "PUT",
          headers: ticket.upload.headers,
          body: new Uint8Array([0xff, 0xd8, 0xff, 0xd9])
        }),
        ticket.fileId,
        envValue,
        86_400
      );
      await expect(
        signedMediaUrl(ticket.fileId, "https://api.example.test", envValue, 900)
      ).resolves.toContain("/media/");

      vi.setSystemTime(new Date(startedAt.getTime() + 86_401_000));
      await expect(
        signedMediaUrl(ticket.fileId, "https://api.example.test", envValue, 900)
      ).rejects.toMatchObject({ code: "media_not_found" });
      expect(imageStore.delete as unknown as ReturnType<typeof vi.fn>).toHaveBeenCalledWith(
        `media/${ticket.fileId}`
      );
    } finally {
      vi.useRealTimers();
    }
  });
});

describe("credit protection and task creation", () => {
  it("refuses paid tasks without persistent state even with a valid signed session", async () => {
    const fetcher = vi.fn<Fetcher>();
    const envValue = env();
    const token = await auth(envValue);
    delete envValue.DRAPEPROOF_STATE;
    const response = await request(createHandler(fetcher), envValue, "/v1/tasks/facial-colors", {
      token,
      body: { sourceFileId: FILE_ID }
    });
    expect(response.status).toBe(503);
    expect(await response.json()).toMatchObject({ error: { code: "state_store_not_configured" } });
    expect(fetcher).not.toHaveBeenCalled();
  });

  it("returns live feature costs with the protected local budget ledger", async () => {
    const { fetcher } = queuedFetch([
      featureCostResponse("/s2s/v2.0/task/skin-tone-analysis", 1),
      featureCostResponse("/s2s/v2.0/task/cloth-v3", 2)
    ]);
    const envValue = env();
    const response = await request(createHandler(fetcher), envValue, "/v1/credits", {
      token: await auth(envValue)
    });
    expect(response.status).toBe(200);
    expect(await response.json()).toEqual({
      budget: 1_000,
      reservedUnits: 0,
      remaining: 1_000,
      protectedFloor: 300,
      availableForTasks: 700,
      provider: "clothes",
      costs: { facialColors: 1, tryOn: 2 },
      balanceSource: "protected-local-ledger"
    });
  });

  it("blocks a task before creation when it would cross the floor", async () => {
    const { fetcher, calls } = queuedFetch([
      featureCostResponse("/s2s/v2.0/task/skin-tone-analysis", 2)
    ]);
    const envValue = env({ UNIT_BUDGET: "300" });
    const response = await request(createHandler(fetcher), envValue, "/v1/tasks/facial-colors", {
      token: await auth(envValue),
      body: { sourceFileId: FILE_ID, faceAngleStrictness: "strict" }
    });
    expect(response.status).toBe(402);
    expect(calls).toHaveLength(1);
    expect(await response.json()).toMatchObject({
      error: { code: "credit_reserve_reached", details: { remaining: 300, floor: 300, taskCost: 2 } }
    });
  });

  it("fails closed if the feature-cost schema cannot be interpreted", async () => {
    const { fetcher, calls } = queuedFetch([
      jsonResponse({ status: 200, result: { next_token: null, skus: [{ description: "missing cost" }] } })
    ]);
    const envValue = env();
    const response = await request(createHandler(fetcher), envValue, "/v1/tasks/facial-colors", {
      token: await auth(envValue),
      body: { sourceFileId: FILE_ID }
    });
    expect(response.status).toBe(503);
    expect(calls).toHaveLength(1);
    expect(await response.json()).toMatchObject({ error: { code: "feature_cost_unavailable" } });
  });

  it("creates a Facial Color task with only the documented payload", async () => {
    const { fetcher, calls } = queuedFetch([
      featureCostResponse("/s2s/v2.0/task/skin-tone-analysis", 1),
      jsonResponse({ status: 200, data: { task_id: TASK_ID } })
    ]);
    const envValue = env();
    const response = await request(createHandler(fetcher), envValue, "/v1/tasks/facial-colors", {
      token: await auth(envValue),
      body: { sourceFileId: FILE_ID, faceAngleStrictness: "strict" }
    });
    expect(response.status).toBe(202);
    expect(calls[1]?.url.endsWith("/s2s/v2.0/task/skin-tone-analysis")).toBe(true);
    expect(JSON.parse(String(calls[1]?.init?.body))).toEqual({
      src_file_id: FILE_ID,
      face_angle_strictness_level: "strict"
    });
    expect(await response.json()).toMatchObject({ taskId: TASK_ID, status: "accepted", reservedUnitCost: 1 });
  });

  it("replays one accepted task ID without issuing a second paid create", async () => {
    let paidCreateCalls = 0;
    const fetcher: Fetcher = async (input) => {
      const url = String(input);
      if (url.includes("/credit/feature-cost")) {
        return featureCostResponse("/s2s/v2.0/task/skin-tone-analysis", 1);
      }
      paidCreateCalls += 1;
      return jsonResponse({ status: 200, data: { task_id: TASK_ID } });
    };
    const envValue = env();
    const handler = createHandler(fetcher);
    const token = await auth(envValue);
    const body = {
      operationId: OPERATION_ID,
      sourceFileId: FILE_ID,
      faceAngleStrictness: "strict"
    };

    const first = await request(handler, envValue, "/v1/tasks/facial-colors", { token, body });
    const replay = await request(handler, envValue, "/v1/tasks/facial-colors", { token, body });

    expect(first.status).toBe(202);
    expect(replay.status).toBe(202);
    expect(await replay.json()).toMatchObject({
      operationId: OPERATION_ID,
      taskId: TASK_ID,
      status: "accepted",
      replayed: true
    });
    expect(paidCreateCalls).toBe(1);
  });

  it("reports Durable Object reservations through the credits endpoint", async () => {
    const { fetcher } = queuedFetch([
      featureCostResponse("/s2s/v2.0/task/skin-tone-analysis", 2),
      jsonResponse({ status: 200, data: { task_id: TASK_ID } }),
      featureCostResponse("/s2s/v2.0/task/skin-tone-analysis", 2),
      featureCostResponse("/s2s/v2.0/task/cloth-v3", 3)
    ]);
    const envValue = env();
    const handler = createHandler(fetcher);
    const token = await auth(envValue);
    const created = await request(handler, envValue, "/v1/tasks/facial-colors", {
      token,
      body: { operationId: OPERATION_ID, sourceFileId: FILE_ID }
    });
    const credits = await request(handler, envValue, "/v1/credits", { token });

    expect(created.status).toBe(202);
    expect(await credits.json()).toMatchObject({
      budget: 1_000,
      reservedUnits: 2,
      remaining: 998,
      protectedFloor: 300,
      availableForTasks: 698
    });
  });

  it("turns an indeterminate upstream outcome into UNKNOWN_RECONCILE and never retries it", async () => {
    let paidCreateCalls = 0;
    const fetcher: Fetcher = async (input) => {
      const url = String(input);
      if (url.includes("/credit/feature-cost")) {
        return featureCostResponse("/s2s/v2.0/task/skin-tone-analysis", 1);
      }
      paidCreateCalls += 1;
      throw new Error("simulated connection loss after send");
    };
    const envValue = env();
    const handler = createHandler(fetcher);
    const token = await auth(envValue);
    const body = { operationId: OPERATION_ID, sourceFileId: FILE_ID };

    const first = await request(handler, envValue, "/v1/tasks/facial-colors", { token, body });
    const replay = await request(handler, envValue, "/v1/tasks/facial-colors", { token, body });
    const status = await request(handler, envValue, `/v1/operations/${OPERATION_ID}`, { token });

    expect(first.status).toBe(409);
    expect(replay.status).toBe(409);
    expect(status.status).toBe(200);
    for (const response of [first, replay]) {
      expect(await response.json()).toMatchObject({
        error: {
          code: "operation_outcome_unknown",
          details: { operationId: OPERATION_ID, state: "UNKNOWN_RECONCILE" }
        }
      });
    }
    expect(await status.json()).toMatchObject({
      operationId: OPERATION_ID,
      status: "unknown_reconcile"
    });
    expect(paidCreateCalls).toBe(1);
  });

  it("creates a Clothes V3 task through the provider adapter", async () => {
    const { fetcher, calls } = queuedFetch([
      featureCostResponse("/s2s/v2.0/task/cloth-v3", 2),
      jsonResponse({ status: 200, data: { task_id: TASK_ID } })
    ]);
    const envValue = env();
    const response = await request(createHandler(fetcher), envValue, "/v1/tasks/try-on", {
      token: await auth(envValue),
      body: {
        sourceFileId: FILE_ID,
        referenceFileId: "reference/AbCdEfGhIjKlMnOp+123=",
        garmentCategory: "upper_body"
      }
    });
    expect(response.status).toBe(202);
    expect(calls[1]?.url.endsWith("/s2s/v2.0/task/cloth-v3")).toBe(true);
    expect(JSON.parse(String(calls[1]?.init?.body))).toEqual({
      src_file_id: FILE_ID,
      ref_file_id: "reference/AbCdEfGhIjKlMnOp+123=",
      garment_category: "upper_body"
    });
    expect(await response.json()).toMatchObject({ provider: "clothes", status: "accepted" });
  });

  it("rejects ambiguous try-on references", async () => {
    const envValue = env();
    const response = await request(createHandler(), envValue, "/v1/tasks/try-on", {
      token: await auth(envValue),
      body: { sourceFileId: FILE_ID, referenceFileId: FILE_ID, templateId: "top-1" }
    });
    expect(response.status).toBe(400);
    expect(await response.json()).toMatchObject({ error: { code: "validation_error" } });
  });

  it("refuses Scarf without the private image store before spending units", async () => {
    const fetcher = vi.fn<Fetcher>();
    const envValue = env({ VTO_PROVIDER: "scarf" });
    const response = await request(createHandler(fetcher), envValue, "/v1/tasks/try-on", {
      token: await auth(envValue),
      body: { sourceFileId: FILE_ID, referenceFileId: FILE_ID }
    });
    expect(response.status).toBe(503);
    expect(fetcher).not.toHaveBeenCalled();
    expect(await response.json()).toMatchObject({ error: { code: "vto_provider_not_configured" } });
  });

  it("creates an official Scarf URL task with fixed validated gender and style", async () => {
    const { fetcher, calls } = queuedFetch([
      featureCostResponse("/s2s/v2.0/task/scarf", 2),
      jsonResponse({ status: 200, data: { task_id: TASK_ID } })
    ]);
    const envValue = env({
      VTO_PROVIDER: "scarf",
      IMAGE_STORE: memoryR2(),
      SCARF_ALLOWED_IMAGE_HOSTS: "assets.example.com"
    });
    const response = await request(createHandler(fetcher), envValue, "/v1/tasks/try-on", {
      token: await auth(envValue),
      body: {
        sourceImageUrl: "https://assets.example.com/person.jpg",
        referenceImageUrl: "https://assets.example.com/scarf.jpg",
        gender: "female"
      }
    });
    expect(response.status).toBe(202);
    expect(calls[1]?.url.endsWith("/s2s/v2.0/task/scarf")).toBe(true);
    expect(JSON.parse(String(calls[1]?.init?.body))).toEqual({
      src_file_url: "https://assets.example.com/person.jpg",
      ref_file_url: "https://assets.example.com/scarf.jpg",
      gender: "female",
      style: "style_modern_chic"
    });
  });

  it("rejects random styles, missing gender, and unallowlisted direct URLs", async () => {
    const fetcher = vi.fn<Fetcher>();
    const envValue = env({
      VTO_PROVIDER: "scarf",
      IMAGE_STORE: memoryR2(),
      SCARF_ALLOWED_IMAGE_HOSTS: "assets.example.com"
    });
    const handler = createHandler(fetcher);
    const token = await auth(envValue);
    const base = {
      sourceImageUrl: "https://assets.example.com/person.jpg",
      referenceImageUrl: "https://assets.example.com/scarf.jpg"
    };

    const random = await request(handler, envValue, "/v1/tasks/try-on", {
      token,
      body: { ...base, gender: "female", style: "random" }
    });
    expect(random.status).toBe(400);

    const missingGender = await request(handler, envValue, "/v1/tasks/try-on", {
      token,
      body: base
    });
    expect(missingGender.status).toBe(400);
    expect(await missingGender.json()).toMatchObject({ error: { code: "gender_required" } });

    const unlisted = await request(handler, envValue, "/v1/tasks/try-on", {
      token,
      body: {
        sourceImageUrl: "https://evil.example/person.jpg",
        referenceImageUrl: "https://assets.example.com/scarf.jpg",
        gender: "male"
      }
    });
    expect(unlisted.status).toBe(400);
    expect(await unlisted.json()).toMatchObject({ error: { code: "image_url_not_allowed" } });
    expect(fetcher).not.toHaveBeenCalled();
  });

  it("enforces the global daily task cap", async () => {
    const { fetcher } = queuedFetch([
      featureCostResponse("/s2s/v2.0/task/skin-tone-analysis", 1),
      jsonResponse({ status: 200, data: { task_id: TASK_ID } }),
      featureCostResponse("/s2s/v2.0/task/skin-tone-analysis", 1)
    ]);
    const envValue = env({ MAX_TASKS_PER_DAY: "1" });
    const handler = createHandler(fetcher);
    const token = await auth(envValue);
    const body = { sourceFileId: FILE_ID };
    expect((await request(handler, envValue, "/v1/tasks/facial-colors", { token, body })).status).toBe(202);
    const second = await request(handler, envValue, "/v1/tasks/facial-colors", { token, body });
    expect(second.status).toBe(429);
    expect(await second.json()).toMatchObject({ error: { code: "daily_task_limit_reached" } });
  });
});

describe("poll normalization and upstream isolation", () => {
  it("normalizes a running task", async () => {
    const { fetcher } = queuedFetch([
      jsonResponse({ status: 200, data: { task_status: "running" } })
    ]);
    const envValue = env();
    const response = await request(createHandler(fetcher), envValue, `/v1/tasks/facial-colors/${TASK_ID}`, {
      token: await auth(envValue)
    });
    expect(await response.json()).toEqual({
      taskId: TASK_ID,
      feature: "facial-colors",
      status: "running",
      retryAfterSeconds: 2
    });
  });

  it("whitelists facial colors and drops unexpected upstream data", async () => {
    const { fetcher } = queuedFetch([
      jsonResponse({
        status: 200,
        data: {
          task_status: "success",
          results: {
            color: {
              skin_color: "#B9947C",
              eye_color: "#293F9B",
              eye_color_name: "Blue",
              internal_model_score: 0.999
            },
            private_debug: API_KEY
          }
        }
      })
    ]);
    const envValue = env();
    const response = await request(createHandler(fetcher), envValue, `/v1/tasks/facial-colors/${TASK_ID}`, {
      token: await auth(envValue)
    });
    const text = await response.text();
    expect(response.status).toBe(200);
    expect(JSON.parse(text)).toMatchObject({
      status: "success",
      result: { colors: { skin_color: "#B9947C", eye_color: "#293F9B" } }
    });
    expect(text).not.toContain("internal_model_score");
    expect(text).not.toContain(API_KEY);
  });

  it("returns only a trusted VTO image URL", async () => {
    const { fetcher } = queuedFetch([
      jsonResponse({
        status: 200,
        data: {
          task_status: "success",
          results: {
            url: "https://yce-us.s3-accelerate.amazonaws.com/demo/result.jpg?sig=abc",
            internal_id: "hidden"
          }
        }
      })
    ]);
    const envValue = env();
    const response = await request(createHandler(fetcher), envValue, `/v1/tasks/try-on/${TASK_ID}`, {
      token: await auth(envValue)
    });
    const body = await response.json();
    expect(body).toMatchObject({
      status: "success",
      result: { imageUrl: "https://yce-us.s3-accelerate.amazonaws.com/demo/result.jpg?sig=abc" }
    });
    expect(JSON.stringify(body)).not.toContain("internal_id");
  });

  it("rejects malicious result URLs and task-ID traversal", async () => {
    const { fetcher } = queuedFetch([
      jsonResponse({
        status: 200,
        data: { task_status: "success", results: { url: "https://evil.example/result.jpg" } }
      })
    ]);
    const envValue = env();
    const token = await auth(envValue);
    const malicious = await request(createHandler(fetcher), envValue, `/v1/tasks/try-on/${TASK_ID}`, { token });
    expect(malicious.status).toBe(502);

    const traversal = await request(createHandler(), envValue, "/v1/tasks/try-on/..%2Fsecret", { token });
    expect(traversal.status).toBe(400);
    expect(await traversal.json()).toMatchObject({ error: { code: "invalid_task_id" } });
  });

  it("sanitizes upstream authentication failures and never reflects the key", async () => {
    const { fetcher } = queuedFetch([
      jsonResponse({ status: 401, error: `bad-${API_KEY}` }, 401)
    ]);
    const envValue = env();
    const response = await request(createHandler(fetcher), envValue, "/v1/tasks/facial-colors", {
      token: await auth(envValue),
      body: { sourceFileId: FILE_ID }
    });
    const text = await response.text();
    expect(response.status).toBe(502);
    expect(JSON.parse(text)).toMatchObject({ error: { code: "youcam_auth_failed" } });
    expect(text).not.toContain(API_KEY);
  });
});
