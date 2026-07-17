import { paidBudgetStatus } from "./paid-operations";
import type { Env } from "./types";

export interface BudgetStatus {
  budget: number;
  reservedUnits: number;
  remaining: number;
  protectedFloor: number;
  availableForTasks: number;
}


export async function budgetStatus(
  env: Env,
  budgetId: string,
  budget: number,
  protectedFloor: number
): Promise<BudgetStatus> {
  return paidBudgetStatus(env, budgetId, budget, protectedFloor);
}

export function resetBudgetForTest(): void {
  // Kept for existing tests. Durable Object state is isolated by the test namespace.
}
