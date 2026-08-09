import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiFetch } from '../../api/http.ts';
import { confirmAssistantAction, sendAssistantMessage } from './assistantApi.ts';

vi.mock('../../api/http.ts', () => ({ apiFetch: vi.fn() }));

describe('assistantApi', () => {
  beforeEach(() => vi.mocked(apiFetch).mockReset());

  it('sends chat messages with the current conversation id', async () => {
    vi.mocked(apiFetch).mockResolvedValue({ conversationId: 'c1', message: 'Hello', pendingActions: [] });

    await sendAssistantMessage('How much leave do I have?', 'c1');

    expect(apiFetch).toHaveBeenCalledWith('/api/assistant/chat', {
      method: 'POST',
      body: JSON.stringify({ message: 'How much leave do I have?', conversationId: 'c1' }),
    });
  });

  it('starts a new conversation when no conversation id exists', async () => {
    vi.mocked(apiFetch).mockResolvedValue({ conversationId: 'c2', message: 'Hello', pendingActions: [] });

    await sendAssistantMessage('Hello');

    expect(apiFetch).toHaveBeenCalledWith('/api/assistant/chat', {
      method: 'POST',
      body: JSON.stringify({ message: 'Hello', conversationId: null }),
    });
  });

  it('uses the secure confirmation endpoint rather than invoking business REST endpoints directly', async () => {
    vi.mocked(apiFetch).mockResolvedValue({ message: 'Completed' });

    await confirmAssistantAction('opaque-token');

    expect(apiFetch).toHaveBeenCalledWith('/api/assistant/actions/confirm', {
      method: 'POST',
      body: JSON.stringify({ confirmationToken: 'opaque-token' }),
    });
  });
});
