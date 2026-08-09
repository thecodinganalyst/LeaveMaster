import type { PendingAction } from './assistantApi.ts';

export type ActionState = 'pending' | 'confirming' | 'confirmed' | 'cancelled' | 'failed';

export interface AssistantMessageItem {
  id: string;
  role: 'user' | 'assistant' | 'system';
  text: string;
  actions?: AssistantActionItem[];
}

export interface AssistantActionItem extends PendingAction {
  id: string;
  state: ActionState;
  confirmationToken?: string;
  error?: string;
}

export const actionTitle = (toolName: string) => {
  const title = toolName
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .trim();
  return title ? title.charAt(0).toUpperCase() + title.slice(1) : 'Pending action';
};

export const actionEntries = (action: PendingAction) =>
  Object.entries(action.arguments ?? {}).filter(([, value]) => value !== undefined && value !== null);

export const printableValue = (value: unknown) => {
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }
  return JSON.stringify(value);
};

export const canConfirmAction = (action: AssistantActionItem) =>
  action.state === 'pending' && Boolean(action.confirmationToken);
