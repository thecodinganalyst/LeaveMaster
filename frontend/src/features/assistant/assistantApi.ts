import { apiFetch } from '../../api/http.ts';

export interface PendingAction {
  toolName: string;
  arguments: Record<string, unknown>;
  requiredAuthority: string;
  actorLoginName: string;
  actorStaffId: string | null;
  tenantId: string | null;
  confirmationToken: string;
  expiresAt: string;
}

export interface StructuredResult {
  toolName: string;
  data: unknown;
}

export interface AssistantChatResponse {
  conversationId: string;
  message: string;
  pendingActions: PendingAction[];
  structuredResults?: StructuredResult[];
}

export interface AssistantConfirmationResponse {
  toolName: string;
  status: string;
  result: string;
  replayed: boolean;
}

export const sendAssistantMessage = (message: string, conversationId?: string) =>
  apiFetch<AssistantChatResponse>('/api/assistant/chat', {
    method: 'POST',
    body: JSON.stringify({ message, conversationId: conversationId || null }),
  });

export const confirmAssistantAction = (confirmationToken: string) =>
  apiFetch<AssistantConfirmationResponse>('/api/assistant/actions/confirm', {
    method: 'POST',
    body: JSON.stringify({ confirmationToken }),
  });
