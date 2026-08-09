import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiFetch } from '../../api/http.ts';
import {
  applyForLeave,
  approveCancellation,
  approveLeave,
  cancelLeave,
  getLeaveBalances,
  getPendingApprovals,
  getVisibleLeave,
  rejectCancellation,
  rejectLeave,
} from './leaveApi.ts';

vi.mock('../../api/http.ts', () => ({ apiFetch: vi.fn() }));

describe('leaveApi', () => {
  beforeEach(() => vi.mocked(apiFetch).mockReset());

  it('loads employee-visible leave and balances for the signed-in staff id', async () => {
    vi.mocked(apiFetch).mockResolvedValue([]);
    await getVisibleLeave('S 1');
    await getLeaveBalances('S 1');
    expect(apiFetch).toHaveBeenNthCalledWith(1, '/leave-applications?staffId=S%201');
    expect(apiFetch).toHaveBeenNthCalledWith(2, '/leave-applications/staff/S%201/balance');
  });

  it('submits a pending leave request using the JSON endpoint', async () => {
    vi.mocked(apiFetch).mockResolvedValue([]);
    const request = {
      staffId: 'S1',
      fromDate: '2026-08-12',
      toDate: '2026-08-13',
      leaveTypeId: 'AL',
      leaveDuration: 'FULL' as const,
      status: 'PENDING' as const,
    };
    await applyForLeave(request);
    expect(apiFetch).toHaveBeenCalledWith('/leave-applications', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  });

  it('uses the dedicated approval and cancellation-decision endpoints', async () => {
    vi.mocked(apiFetch).mockResolvedValue({});
    await getPendingApprovals('M1');
    await approveLeave('L1', 'M1');
    await rejectLeave('L2', 'M1');
    await approveCancellation('L3');
    await rejectCancellation('L4');
    await cancelLeave('L5');

    expect(apiFetch).toHaveBeenNthCalledWith(1, '/leave-applications/approver/M1');
    expect(apiFetch).toHaveBeenNthCalledWith(2, '/leave-applications/L1/approve?approverId=M1', { method: 'PUT' });
    expect(apiFetch).toHaveBeenNthCalledWith(3, '/leave-applications/L2/reject?approverId=M1', { method: 'PUT' });
    expect(apiFetch).toHaveBeenNthCalledWith(4, '/leave-applications/L3/approve-cancellation', { method: 'PUT' });
    expect(apiFetch).toHaveBeenNthCalledWith(5, '/leave-applications/L4/reject-cancellation', { method: 'PUT' });
    expect(apiFetch).toHaveBeenNthCalledWith(6, '/leave-applications/L5', { method: 'DELETE' });
  });
});
