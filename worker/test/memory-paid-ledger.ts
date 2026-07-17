import { PaidTaskLedger } from "../src/paid-operations";
import type { Env } from "../src/types";

class MemoryDurableObjectStorage {
  private readonly values = new Map<string, unknown>();
  private tail: Promise<void> = Promise.resolve();

  async get<T = unknown>(key: string): Promise<T | undefined> {
    return structuredClone(this.values.get(key)) as T | undefined;
  }

  async put<T>(keyOrEntries: string | Record<string, T>, value?: T): Promise<void> {
    if (typeof keyOrEntries === "string") {
      this.values.set(keyOrEntries, structuredClone(value));
      return;
    }
    for (const [key, entry] of Object.entries(keyOrEntries)) {
      this.values.set(key, structuredClone(entry));
    }
  }

  async transaction<T>(
    closure: (transaction: DurableObjectTransaction) => Promise<T>
  ): Promise<T> {
    const previous = this.tail;
    let release = () => {};
    this.tail = new Promise<void>((resolve) => { release = resolve; });
    await previous;
    try {
      return await closure(this as unknown as DurableObjectTransaction);
    } finally {
      release();
    }
  }
}

export function memoryPaidLedgerNamespace(env: Env): DurableObjectNamespace {
  const ledgers = new Map<string, PaidTaskLedger>();
  return {
    idFromName: (name: string) => ({
      name,
      toString: () => name,
      equals: (other: DurableObjectId) => other.toString() === name
    }) as unknown as DurableObjectId,
    get: (id: DurableObjectId) => {
      const name = id.toString();
      let ledger = ledgers.get(name);
      if (!ledger) {
        const storage = new MemoryDurableObjectStorage();
        ledger = new PaidTaskLedger({ storage } as unknown as DurableObjectState, env);
        ledgers.set(name, ledger);
      }
      return {
        fetch: (input: RequestInfo | URL, init?: RequestInit) => {
          const request = input instanceof Request ? input : new Request(input, init);
          return ledger.fetch(request);
        }
      } as unknown as DurableObjectStub;
    }
  } as unknown as DurableObjectNamespace;
}
