import { describe, expect, it } from 'vitest';

import type { LeaveApplication } from './leaveApi.ts';
import { canCancelApplication, canEditApplication, isUpcoming, sortByLeaveDate } from './leaveView.ts';

const application = (overrides: Partial<LeaveApplication> = {}): LeaveApplication => ({
  id: 'L1',
  staff: { id: 'S1', name: 'Dennis' },
  leaveDate: '2026-08-12',
  leaveType: { id: 'AL', name: 'Annual Leave' },
  leaveDuration: 'FULL',
  status: 'PENDING',
  applicationDate: '2026-08-09',
  ...overrides,
});

describe('leave workflow presentation rules', () => {
  it('allows editing only draft or pending applications', () => {
    expect(canEditApplication(application({ status: 'DRAFT' }))).toBe(true);
    expect(canEditApplication(application({ status: 'PENDING' }))).toBe(true);
    expect(canEditApplication(application({ status: 'APPROVED' }))).toBe(false);
  });

  it('hides cancellation after a terminal or already requested status', () => {
    expect(canCancelApplication(application({ status: 'APPROVED' }))).toBe(true);
    expect(canCancelApplication(application({ status: 'CANCEL_REQUESTED' }))).toBe(false);
    expect(canCancelApplication(application({ status: 'CANCELLED' }))).toBe(false);
    expect(canCancelApplication(application({ status: 'DENIED' }))).toBe(false);
  });

  it('filters upcoming active leave and orders by date', () => {
    const today = new Date('2026-08-09T00:00:00Z');
    expect(isUpcoming(application({ leaveDate: '2026-08-10' }), today)).toBe(true);
    expect(isUpcoming(application({ leaveDate: '2026-08-08' }), today)).toBe(false);
    expect(isUpcoming(application({ leaveDate: '2026-08-10', status: 'CANCELLED' }), today)).toBe(false);

    expect(sortByLeaveDate([
      application({ id: '2', leaveDate: '2026-08-20' }),
      application({ id: '1', leaveDate: '2026-08-10' }),
    ]).map((item) => item.id)).toEqual(['1', '2']);
  });
});
