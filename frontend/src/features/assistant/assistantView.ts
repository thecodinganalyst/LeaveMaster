import type { PendingAction, StructuredResult } from './assistantApi.ts';

export type ActionState = 'pending' | 'confirming' | 'confirmed' | 'cancelled' | 'failed';

export interface AssistantMessageItem {
  id: string;
  role: 'user' | 'assistant' | 'system';
  text: string;
  actions?: AssistantActionItem[];
  results?: StructuredResult[];
}

export interface AssistantActionItem extends PendingAction {
  id: string;
  state: ActionState;
  error?: string | undefined;
  executionResult?: string | undefined;
  replayed?: boolean | undefined;
}

export const actionTitle = (toolName: string) => {
  const title = toolName
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .trim();
  return title ? title.charAt(0).toUpperCase() + title.slice(1) : 'Pending action';
};

export const resultTitle = (toolName: string) => {
  if (toolName === 'getLeaveEntitlementConfigurationByJurisdiction') return 'Leave entitlement summary';
  const readable = actionTitle(toolName).replace(/^Get\s+/i, '');
  return readable || 'LeaveMaster result';
};

const FIELD_LABELS: Record<string, string> = {
  leaveType: 'Leave type',
  policies: 'Policies',
  policyName: 'Policy',
  eligibility: 'Eligibility',
  entitlement: 'Entitlement',
  accrual: 'Accrual',
  proration: 'Proration',
  carryForward: 'Carry forward',
};

export const fieldLabel = (key: string) => {
  if (FIELD_LABELS[key]) return FIELD_LABELS[key];
  const spaced = key.replace(/([a-z0-9])([A-Z])/g, '$1 $2').replace(/[_-]+/g, ' ').trim();
  return spaced ? spaced.charAt(0).toUpperCase() + spaced.slice(1) : key;
};

export const actionEntries = (action: PendingAction) =>
  Object.entries(action.arguments ?? {}).filter(([, value]) => value !== undefined && value !== null);

export const dataEntries = (data: unknown): [string, unknown][] => {
  if (!data || Array.isArray(data) || typeof data !== 'object') return [];
  return Object.entries(data as Record<string, unknown>)
    .filter(([, value]) => value !== undefined && value !== null)
    .map(([key, value]) => [fieldLabel(key), value]);
};

export const printableValue = (value: unknown) => {
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }
  if (Array.isArray(value) && value.every((item) => typeof item === 'string' || typeof item === 'number')) {
    return value.join('; ');
  }
  return JSON.stringify(value);
};

export const canConfirmAction = (action: AssistantActionItem) =>
  action.state === 'pending' && Boolean(action.confirmationToken);
