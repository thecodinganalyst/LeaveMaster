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
  error?: string;
  executionResult?: string;
  replayed?: boolean;
}

export const actionTitle = (toolName: string) => {
  const title = toolName
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .trim();
  return title ? title.charAt(0).toUpperCase() + title.slice(1) : 'Pending action';
};

export const resultTitle = (toolName: string) => {
  const readable = actionTitle(toolName).replace(/^Get\s+/i, '');
  return readable || 'LeaveMaster result';
};

export const actionEntries = (action: PendingAction) =>
  Object.entries(action.arguments ?? {}).filter(([, value]) => value !== undefined && value !== null);

export const dataEntries = (data: unknown): [string, unknown][] => {
  if (!data || Array.isArray(data) || typeof data !== 'object') return [];
  return Object.entries(data as Record<string, unknown>).filter(([, value]) => value !== undefined && value !== null);
};

export const printableValue = (value: unknown) => {
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }
  return JSON.stringify(value);
};

export const canConfirmAction = (action: AssistantActionItem) =>
  action.state === 'pending' && Boolean(action.confirmationToken);
