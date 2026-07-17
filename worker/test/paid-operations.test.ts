import { describe, expect, it } from "vitest";
import {
  acceptPaidOperation,
  admitPaidOperation,
  fingerprintPaidRequest,
  markPaidOperationUnknown,
  paidBudgetStatus,
  rejectPaidOperation
} from "../src/paid-operations";
import type { Env } from "../src/types";
import { memoryPaidLedgerNamespace } from "./memory-paid-ledger";

const BUDGET_ID = "atomic-test";
const OPERATION_A = "10000000-0000-4000-8000-000000000001";
const OPERATION_B = "20000000-0000-4000-8000-000000000002";
const TASK_ID = "task_1234567890abcdef";

function atomicEnv(): Env {
  const env = { YOUCAM_API_KEY: "test-youcam-key" } as Env;
  env.PAID_TASK_LEDGER = memoryPaidLedgerNamespace(env);
  return env;
}

async function admissionInput(operationId: string, payload: unknown, unitCost = 2) {
  return {
    operationId,
    feature: "facial-colors" as const,
    fingerprint: await fingerprintPaidRequest("facial-colors", payload),
    unitCost,
    budget: 302,
    protectedFloor: 300,
    maxTasksPerDay: 10
  };
}

describe("atomic paid-operation ledger", () => {
  it("serializes concurrent reservations so the protected floor cannot be crossed", async () => {
    const env = atomicEnv();
    const first = await admissionInput(OPERATION_A, { sourceFileId: "one" });
    const second = await admissionInput(OPERATION_B, { sourceFileId: "two" });

    const outcomes = await Promise.allSettled([
      admitPaidOperation(env, BUDGET_ID, first),
      admitPaidOperation(env, BUDGET_ID, second)
    ]);

    expect(outcomes.filter((outcome) => outcome.status === "fulfilled")).toHaveLength(1);
    const rejected = outcomes.find((outcome) => outcome.status === "rejected");
    expect(rejected).toMatchObject({ reason: { code: "credit_reserve_reached" } });
    expect(await paidBudgetStatus(env, BUDGET_ID, 302, 300)).toMatchObject({
      reservedUnits: 2,
      remaining: 300,
      availableForTasks: 0
    });
  });

  it("reserves once and replays the same accepted task ID for one operation ID", async () => {
    const env = atomicEnv();
    const input = await admissionInput(OPERATION_A, { sourceFileId: "same" });
    const first = await admitPaidOperation(env, BUDGET_ID, input);
    const duplicateWhileCreating = await admitPaidOperation(env, BUDGET_ID, input);
    expect(first).toMatchObject({ state: "admitted", replayed: false });
    expect(duplicateWhileCreating).toMatchObject({ state: "in_progress", replayed: true });

    await acceptPaidOperation(env, BUDGET_ID, {
      operationId: OPERATION_A,
      feature: "facial-colors",
      fingerprint: input.fingerprint,
      taskId: TASK_ID
    });
    const replay = await admitPaidOperation(env, BUDGET_ID, input);
    expect(replay).toMatchObject({ state: "accepted", replayed: true, taskId: TASK_ID });
    expect((await paidBudgetStatus(env, BUDGET_ID, 302, 300)).reservedUnits).toBe(2);
  });

  it("rejects operation-ID reuse with a different request fingerprint", async () => {
    const env = atomicEnv();
    await admitPaidOperation(env, BUDGET_ID, await admissionInput(OPERATION_A, { sourceFileId: "one" }));
    await expect(
      admitPaidOperation(env, BUDGET_ID, await admissionInput(OPERATION_A, { sourceFileId: "changed" }))
    ).rejects.toMatchObject({ status: 409, code: "operation_id_conflict" });
  });

  it("keeps indeterminate costs reserved and never re-admits the operation", async () => {
    const env = atomicEnv();
    const input = await admissionInput(OPERATION_A, { sourceFileId: "unknown" });
    await admitPaidOperation(env, BUDGET_ID, input);
    await markPaidOperationUnknown(env, BUDGET_ID, {
      operationId: OPERATION_A,
      feature: "facial-colors",
      fingerprint: input.fingerprint
    });

    expect(await admitPaidOperation(env, BUDGET_ID, input)).toMatchObject({
      state: "unknown_reconcile",
      replayed: true
    });
    expect((await paidBudgetStatus(env, BUDGET_ID, 302, 300)).reservedUnits).toBe(2);
  });

  it("releases a known provider rejection exactly once", async () => {
    const env = atomicEnv();
    const input = await admissionInput(OPERATION_A, { sourceFileId: "rejected" });
    await admitPaidOperation(env, BUDGET_ID, input);
    const transition = {
      operationId: OPERATION_A,
      feature: "facial-colors" as const,
      fingerprint: input.fingerprint,
      rejectionCode: "youcam_rejected",
      rejectionMessage: "YouCam rejected the request."
    };
    await rejectPaidOperation(env, BUDGET_ID, transition);
    await rejectPaidOperation(env, BUDGET_ID, transition);

    expect((await paidBudgetStatus(env, BUDGET_ID, 302, 300)).reservedUnits).toBe(0);
    expect(await admitPaidOperation(env, BUDGET_ID, input)).toMatchObject({
      state: "rejected",
      rejectionCode: "youcam_rejected"
    });
  });
});
