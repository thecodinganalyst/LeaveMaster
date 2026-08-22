import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiError } from '../../api/http.ts';
import { AssistantPanel } from './AssistantPanel.tsx';
import { confirmAssistantAction, sendAssistantMessage } from './assistantApi.ts';

vi.mock('./assistantApi.ts', () => ({
  sendAssistantMessage: vi.fn(),
  confirmAssistantAction: vi.fn(),
}));

beforeEach(() => {
  vi.mocked(sendAssistantMessage).mockReset();
  vi.mocked(confirmAssistantAction).mockReset();
  Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
    configurable: true,
    value: vi.fn(),
  });
});

describe('AssistantPanel', () => {
  it('renders authoritative structured business results alongside the assistant explanation', async () => {
    vi.mocked(sendAssistantMessage).mockResolvedValue({
      conversationId: 'c1',
      message: 'You have 12.5 days of annual leave remaining.',
      pendingActions: [],
      structuredResults: [
        {
          toolName: 'getLeaveBalances',
          data: [{ leaveType: 'Annual Leave', entitlement: 18, used: 5.5, balance: 12.5 }],
        },
      ],
    });

    render(<AssistantPanel />);
    fireEvent.change(screen.getByLabelText('Message Ask LeaveMaestro'), { target: { value: 'How much annual leave do I have?' } });
    fireEvent.click(screen.getByLabelText('Send message'));

    expect(await screen.findByText('You have 12.5 days of annual leave remaining.')).toBeInTheDocument();
    expect(screen.getByText('Authoritative LeaveMaestro data')).toBeInTheDocument();
    expect(screen.getByText('Annual Leave')).toBeInTheDocument();
    expect(screen.getByText('12.5')).toBeInTheDocument();
  });

  it('renders markdown only for assistant messages while keeping user text literal', async () => {
    vi.mocked(sendAssistantMessage).mockResolvedValue({
      conversationId: 'markdown-1',
      message: '### Annual Leave\n\nEmployees receive **7 days**.\n\n| Service | Entitlement |\n| --- | --- |\n| 3–11 months | 7 days |',
      pendingActions: [],
      structuredResults: [],
    });

    render(<AssistantPanel />);
    fireEvent.change(screen.getByLabelText('Message Ask LeaveMaestro'), { target: { value: '**show** annual leave' } });
    fireEvent.click(screen.getByLabelText('Send message'));

    expect(await screen.findByRole('heading', { level: 3, name: 'Annual Leave' })).toBeInTheDocument();
    expect(screen.getByRole('table')).toBeInTheDocument();
    expect(screen.getByText('7 days', { selector: 'strong' })).toBeInTheDocument();
    expect(screen.getByText('**show** annual leave')).toBeInTheDocument();
    expect(screen.queryByText('show', { selector: 'strong' })).not.toBeInTheDocument();
  });

  it('shows the conversation id when an assistant provider request fails', async () => {
    vi.mocked(sendAssistantMessage).mockRejectedValue(new ApiError(
      'The AI provider timed out',
      502,
      { conversationId: 'conversation-timeout' },
    ));

    render(<AssistantPanel />);
    fireEvent.change(screen.getByLabelText('Message Ask LeaveMaestro'), { target: { value: 'Show Singapore policies' } });
    fireEvent.click(screen.getByLabelText('Send message'));

    expect(await screen.findByText('The AI provider timed out')).toBeInTheDocument();
    expect(screen.getByText('Conversation ID: conversation-timeout')).toBeInTheDocument();
  });

  it('confirms the exact server token once and displays the authoritative execution result', async () => {
    vi.mocked(sendAssistantMessage).mockResolvedValue({
      conversationId: 'c2',
      message: 'I can apply this leave after you confirm.',
      structuredResults: [],
      pendingActions: [
        {
          toolName: 'applyForLeave',
          arguments: { leaveTypeId: 'ANNUAL', fromDate: '2026-08-13', toDate: '2026-08-14' },
          requiredAuthority: 'LEAVE_APPLICATION_WRITE',
          actorLoginName: 'test-user',
          actorStaffId: 'S1',
          tenantId: 'T1',
          confirmationToken: 'test-confirmation-token',
          expiresAt: '2026-08-09T12:15:00Z',
        },
      ],
    });
    vi.mocked(confirmAssistantAction).mockResolvedValue({
      toolName: 'applyForLeave',
      status: 'EXECUTED',
      result: '{"id":"L1","status":"PENDING"}',
      replayed: false,
    });

    render(<AssistantPanel />);
    fireEvent.change(screen.getByLabelText('Message Ask LeaveMaestro'), { target: { value: 'Apply annual leave' } });
    fireEvent.click(screen.getByLabelText('Send message'));

    const confirm = await screen.findByLabelText('Confirm Apply For Leave');
    fireEvent.click(confirm);

    await waitFor(() => expect(confirmAssistantAction).toHaveBeenCalledTimes(1));
    expect(confirmAssistantAction).toHaveBeenCalledWith('test-confirmation-token');
    expect(await screen.findByText('Authoritative server result')).toBeInTheDocument();
    expect(screen.getByText('{"id":"L1","status":"PENDING"}')).toBeInTheDocument();
    expect(screen.queryByLabelText('Confirm Apply For Leave')).not.toBeInTheDocument();
  });

  it('cancels a proposal locally without invoking the write endpoint', async () => {
    vi.mocked(sendAssistantMessage).mockResolvedValue({
      conversationId: 'c3',
      message: 'This request needs confirmation.',
      structuredResults: [],
      pendingActions: [
        {
          toolName: 'approveLeaveApplication',
          arguments: { leaveApplicationId: 'L1' },
          requiredAuthority: 'LEAVE_APPLICATION_APPROVE',
          actorLoginName: 'test-manager',
          actorStaffId: 'M1',
          tenantId: 'T1',
          confirmationToken: 'test-confirmation-token-2',
          expiresAt: '2026-08-09T12:15:00Z',
        },
      ],
    });

    render(<AssistantPanel />);
    fireEvent.change(screen.getByLabelText('Message Ask LeaveMaestro'), { target: { value: 'Approve request L1' } });
    fireEvent.click(screen.getByLabelText('Send message'));

    fireEvent.click(await screen.findByLabelText('Cancel Approve Leave Application'));
    expect(await screen.findByText('Cancelled')).toBeInTheDocument();
    expect(confirmAssistantAction).not.toHaveBeenCalled();
  });
});
