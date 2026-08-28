import { App } from 'antd';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getCurrentUser } from '../../auth/session.ts';
import {
  applyForLeave,
  getLeaveApplicationPolicyMetadata,
  getLeaveTypes,
} from '../../features/leave/leaveApi.ts';
import { ApplyLeavePage } from './ApplyLeavePage.tsx';

vi.mock('@refinedev/core', () => ({
  useCan: () => ({ data: { can: true } }),
}));

vi.mock('../../auth/session.ts', () => ({ getCurrentUser: vi.fn() }));
vi.mock('../../features/leave/leaveApi.ts', async () => {
  const actual = await vi.importActual<typeof import('../../features/leave/leaveApi.ts')>('../../features/leave/leaveApi.ts');
  return {
    ...actual,
    applyForLeave: vi.fn(),
    getLeaveApplicationPolicyMetadata: vi.fn(),
    getLeaveTypes: vi.fn(),
  };
});

const mockedGetCurrentUser = vi.mocked(getCurrentUser);
const mockedGetLeaveTypes = vi.mocked(getLeaveTypes);
const mockedGetPolicyMetadata = vi.mocked(getLeaveApplicationPolicyMetadata);
const mockedApplyForLeave = vi.mocked(applyForLeave);

const renderPage = () => render(
  <MemoryRouter>
    <App>
      <ApplyLeavePage />
    </App>
  </MemoryRouter>,
);

const selectLeaveType = async (label: string) => {
  const selects = screen.getAllByRole('combobox');
  fireEvent.mouseDown(selects[0]!);
  fireEvent.click(await screen.findByText(label));
};

describe('ApplyLeavePage policy-aware event fields', () => {
  beforeEach(() => {
    mockedGetCurrentUser.mockReset();
    mockedGetLeaveTypes.mockReset();
    mockedGetPolicyMetadata.mockReset();
    mockedApplyForLeave.mockReset();
    mockedGetCurrentUser.mockResolvedValue({
      loginName: 'alice',
      staffId: 'S1',
      tenantId: 'T1',
      active: true,
      authorities: ['LEAVE_APPLICATION_WRITE'],
    });
    mockedGetLeaveTypes.mockResolvedValue([
      { id: 'AL', name: 'Annual Leave' },
      { id: 'MAT', name: 'Maternity Leave' },
    ]);
    mockedGetPolicyMetadata.mockImplementation(async (_staffId, leaveTypeId) => leaveTypeId === 'MAT'
      ? { policyModel: 'EVENT_BASED', eventBased: true, eventRequiresVerification: true }
      : { policyModel: 'ANNUAL_ENTITLEMENT', eventBased: false, eventRequiresVerification: false });
    mockedApplyForLeave.mockResolvedValue([]);
  });

  it('hides qualifying-event inputs for non-event leave and shows them for event leave', async () => {
    renderPage();

    await waitFor(() => expect(mockedGetLeaveTypes).toHaveBeenCalled());
    await selectLeaveType('Annual Leave');
    await waitFor(() => expect(mockedGetPolicyMetadata).toHaveBeenCalledWith('S1', 'AL', undefined));
    expect(screen.queryByText('Qualifying event')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Event date')).not.toBeInTheDocument();

    await selectLeaveType('Maternity Leave');
    await waitFor(() => expect(screen.getByText('Qualifying event')).toBeInTheDocument());
    expect(screen.getByLabelText('Event date')).toBeInTheDocument();
    expect(screen.getByLabelText('Event reference')).toBeInTheDocument();
  });

  it('requires event date and the normal attachment when verification is required', async () => {
    renderPage();

    await waitFor(() => expect(mockedGetLeaveTypes).toHaveBeenCalled());
    await selectLeaveType('Maternity Leave');
    await waitFor(() => expect(screen.getByText('Qualifying event')).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText('From date'), { target: { value: '2026-08-28' } });
    fireEvent.change(screen.getByLabelText('To date'), { target: { value: '2026-08-28' } });
    await waitFor(() => expect(mockedGetPolicyMetadata).toHaveBeenCalledWith('S1', 'MAT', '2026-08-28'));
    fireEvent.click(await screen.findByRole('button', { name: 'Submit request' }));

    expect(await screen.findByText('Event date is required for event-based leave')).toBeInTheDocument();
    expect(screen.getByText('Attachment is required because this qualifying event must be verified')).toBeInTheDocument();
    expect(mockedApplyForLeave).not.toHaveBeenCalled();
  });
});
