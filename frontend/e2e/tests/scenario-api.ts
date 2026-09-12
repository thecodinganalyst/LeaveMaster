import type { APIRequestContext } from '@playwright/test';

export interface ScenarioUser {
  alias: string;
  loginName: string;
  staffId: string;
  email: string;
}

export interface BootstrapScenario {
  scenarioId: string;
  tenantId: string;
  jurisdictionId: string;
  referenceDate: string;
  password: string;
  users: Record<string, ScenarioUser>;
}

const backendBaseUrl = () => process.env.E2E_BACKEND_URL ?? 'http://127.0.0.1:8080';

export const createPersistedScenario = async (
  request: APIRequestContext,
  scenarioId: string,
  referenceDate = '2026-09-12',
): Promise<BootstrapScenario> => {
  const response = await request.post(`${backendBaseUrl()}/test/scenarios/standard-sg-company`, {
    params: { scenarioId, referenceDate },
  });
  if (!response.ok()) {
    throw new Error(`Failed to create E2E scenario (${response.status()}): ${await response.text()}`);
  }
  return response.json() as Promise<BootstrapScenario>;
};

export const deletePersistedScenario = async (
  request: APIRequestContext,
  scenarioId: string,
): Promise<void> => {
  const response = await request.delete(`${backendBaseUrl()}/test/scenarios/${encodeURIComponent(scenarioId)}`);
  if (!response.ok()) {
    throw new Error(`Failed to delete E2E scenario (${response.status()}): ${await response.text()}`);
  }
};
