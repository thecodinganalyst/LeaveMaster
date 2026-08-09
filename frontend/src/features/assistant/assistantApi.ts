import { apiFetch } from '../../api/http.ts';

export interface PendingAction {
  toolName: string;
  arguments: Record<string, unknown>;
  requiredAuthority: string;
  actorLoginName: string;
  actorStaffId: string | null;
  tenantId: string | null;
}

export interface AssistantChatResponse {
  conversationId: string;
  message: string;
  pendingActions: PendingAction[];
}

export const sendAssistantMessage = (message: string, conversationId?: string) =>
  apiFetch<AssistantChatResponse>('/api/assistant/chat', {
    method: 'POST',
    body: JSON.stringify({ message, conversationId: conversationId || null }),
  });

/**
 * Secure action execution is intentionally a separate server contract. Issue #115
 * is responsible for issuing an opaque/idempotent confirmation token and exposing
 * this endpoint. Keeping the client contract here lets the UI be completed without
 * falling back to unsafe direct REST execution based on mutable browser arguments.
 */
export const confirmAssistantAction = (confirmationToken: string) =>
  apiFetch<{ message?: string }>('/api/assistant/actions/confirm', {
    method: 'POST',
    body: JSON.stringify({ confirmationToken }),
  });
