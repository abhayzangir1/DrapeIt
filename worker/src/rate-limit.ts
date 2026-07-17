import { ApiError } from "./errors";
import type { Env, RateLimitResult } from "./types";

interface CounterValue {
  count: number;
  resetAt: number;
}

interface CounterStore {
  get(key: string): Promise<CounterValue | null>;
  put(key: string, value: CounterValue, ttlSeconds: number): Promise<void>;
}

const memoryCounters = new Map<string, CounterValue>();

class MemoryCounterStore implements CounterStore {
  async get(key: string): Promise<CounterValue | null> {
    const value = memoryCounters.get(key);
    if (!value) return null;
    if (value.resetAt <= Math.floor(Date.now() / 1_000)) {
      memoryCounters.delete(key);
      return null;
    }
    return value;
  }

  async put(key: string, value: CounterValue): Promise<void> {
    memoryCounters.set(key, value);
  }
}

class KvCounterStore implements CounterStore {
  constructor(private readonly kv: KVNamespace) {}

  async get(key: string): Promise<CounterValue | null> {
    const raw = await this.kv.get(key, "text");
    if (!raw) return null;
    try {
      const parsed = JSON.parse(raw) as Record<string, unknown>;
      if (
        typeof parsed.count === "number" &&
        Number.isSafeInteger(parsed.count) &&
        typeof parsed.resetAt === "number" &&
        Number.isSafeInteger(parsed.resetAt)
      ) {
        return { count: parsed.count, resetAt: parsed.resetAt };
      }
    } catch {
      // Treat corrupt or stale state as absent. The next write repairs it.
    }
    return null;
  }

  async put(key: string, value: CounterValue, ttlSeconds: number): Promise<void> {
    await this.kv.put(key, JSON.stringify(value), { expirationTtl: Math.max(60, ttlSeconds) });
  }
}

function storeFor(env: Env): CounterStore {
  return env.DRAPEPROOF_STATE ? new KvCounterStore(env.DRAPEPROOF_STATE) : new MemoryCounterStore();
}

async function anonymize(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest).slice(0, 12), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

export async function takeRateLimit(
  env: Env,
  scope: string,
  identity: string,
  limit: number,
  windowSeconds = 60,
  nowSeconds = Math.floor(Date.now() / 1_000)
): Promise<RateLimitResult> {
  const window = Math.floor(nowSeconds / windowSeconds);
  const resetAt = (window + 1) * windowSeconds;
  const key = `counter:v1:${scope}:${window}:${await anonymize(identity)}`;
  const store = storeFor(env);
  const current = await store.get(key);
  const count = (current?.count ?? 0) + 1;
  await store.put(key, { count, resetAt }, resetAt - nowSeconds + 5);

  return {
    allowed: count <= limit,
    limit,
    remaining: Math.max(0, limit - count),
    resetAt
  };
}

export async function enforceRateLimit(
  env: Env,
  scope: string,
  identity: string,
  limit: number,
  windowSeconds = 60
): Promise<RateLimitResult> {
  const result = await takeRateLimit(env, scope, identity, limit, windowSeconds);
  if (!result.allowed) {
    const retryAfter = Math.max(1, result.resetAt - Math.floor(Date.now() / 1_000));
    throw new ApiError(429, "rate_limited", "Too many requests. Try again shortly.", undefined, {
      "retry-after": String(retryAfter),
      "x-ratelimit-limit": String(result.limit),
      "x-ratelimit-remaining": "0",
      "x-ratelimit-reset": String(result.resetAt)
    });
  }
  return result;
}

export async function reserveDailyTask(env: Env, maximum: number): Promise<void> {
  const result = await takeRateLimit(env, "tasks-daily", "global", maximum, 86_400);
  if (!result.allowed) {
    throw new ApiError(
      429,
      "daily_task_limit_reached",
      "The demo task limit has been reached for today.",
      undefined,
      { "retry-after": String(Math.max(1, result.resetAt - Math.floor(Date.now() / 1_000))) }
    );
  }
}

export function resetMemoryCountersForTest(): void {
  memoryCounters.clear();
}
