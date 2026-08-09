import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

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
    fireEvent.change(screen.getByLabelText('Message Ask LeaveMaster'), { target: { value: 'How much annual leave do I have?' } });
    fireEvent.click(screen.getByLabelText('Send message'));

    expect(await screen.findByText('You have 12.5 days of annual leave remaining.')).toBeInTheDocument();
    expect(screen.getByText('Authoritative LeaveMaster data')).toBeInTheDocument();
    expect(screen.getByText('Annual Leave')).toBeInTheDocument();
    expect(screen.getByText('12.5')).toBeInTheDocument();
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
          actorLoginName: 'dennis',
          actorStaffId: 'S1',
          tenantId: 'T1',
          confirmationToken: 'opaque-token',
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
    fireEvent.change(screen.getByLabelText('Message Ask LeaveMaster'), { target: { value: 'Apply annual leave' } });
    fireEvent.click(screen.getByLabelText('Send message'));

    const confirm = await screen.findByLabelText('Confirm Apply For Leave');
    fireEvent.click(confirm);

    await waitFor(() => expect(confirmAssistantAction).toHaveBeenCalledTimes(1));
    expect(confirmAssistantAction).toHaveBeenCalledWith('opaque-token');
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
          actorLoginName: 'manager',
          actorStaffId: 'M1',
          tenantId: 'T1',
          confirmationToken: 'opaque-token-2',
          expiresAt: '2026-08-09T12:15:00Z',
        },
      ],
    });

    render(<AssistantPanel />);
    fireEvent.change(screen.getByLabelText('Message Ask LeaveMaster'), { target: { value: 'Approve request L1' } });
    fireEvent.click(screen.getByLabelText('Send message'));

    fireEvent.click(await screen.findByLabelText('Cancel Approve Leave Application'));
    expect(await screen.findByText('Cancelled')).toBeInTheDocument();
    expect(confirmAssistantAction).not.toHaveBeenCalled();
  });
});
