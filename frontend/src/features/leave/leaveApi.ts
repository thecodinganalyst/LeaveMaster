import { apiFetch } from '../../api/http.ts';

export type LeaveDuration = 'FULL' | 'AM' | 'PM';
export type LeaveStatus = 'DRAFT' | 'PENDING_VERIFICATION' | 'PENDING' | 'APPROVED' | 'CANCEL_REQUESTED' | 'CANCELLED' | 'DENIED';

export interface LeaveTypeSummary {
  id: string;
  name: string;
}

export interface StaffSummary {
  id: string;
  name: string;
  email?: string | null;
}

export interface LeaveApplication {
  id: string;
  staff: StaffSummary;
  leaveDate: string;
  leaveType: LeaveTypeSummary;
  leaveDuration: LeaveDuration;
  status: LeaveStatus;
  attachmentUrl?: string | null;
  approver?: StaffSummary | null;
  applicationDate: string;
  approvalDate?: string | null;
  eventEntitlementId?: string | null;
}

export interface LeaveBalance {
  leaveType: LeaveTypeSummary;
  entitlement: number;
  used: number;
  balance: number;
}

export interface ApplyLeaveRequest {
  staffId: string;
  fromDate: string;
  toDate: string;
  leaveTypeId: string;
  leaveDuration: LeaveDuration;
  status: 'PENDING' | 'DRAFT';
  qualifyingEventId?: string;
  eventTypeCode?: string;
  eventDate?: string;
  eventStartDate?: string;
  eventEndDate?: string;
  dependantId?: string;
  eventExternalReference?: string;
  eventSupportingDocumentReference?: string;
}

export const getVisibleLeave = (staffId: string) =>
  apiFetch<LeaveApplication[]>(`/leave-applications?staffId=${encodeURIComponent(staffId)}`);

export const getLeaveBalances = (staffId: string) =>
  apiFetch<LeaveBalance[]>(`/leave-applications/staff/${encodeURIComponent(staffId)}/balance`);

export const getLeaveApplication = (id: string) =>
  apiFetch<LeaveApplication>(`/leave-applications/${encodeURIComponent(id)}`);

export const getLeaveTypes = () => apiFetch<LeaveTypeSummary[]>('/leave-types');

export const applyForLeave = (request: ApplyLeaveRequest) =>
  apiFetch<LeaveApplication[]>('/leave-applications', { method: 'POST', body: JSON.stringify(request) });

export const updateLeaveDuration = (application: LeaveApplication, leaveDuration: LeaveDuration) =>
  apiFetch<LeaveApplication>(`/leave-applications/${encodeURIComponent(application.id)}`, {
    method: 'PUT',
    body: JSON.stringify({
      status: application.status,
      approver: application.approver ?? null,
      approvalDate: application.approvalDate ?? null,
      leaveDuration,
    }),
  });

export const cancelLeave = (id: string) =>
  apiFetch<void>(`/leave-applications/${encodeURIComponent(id)}`, { method: 'DELETE' });

export const getPendingApprovals = (approverId: string) =>
  apiFetch<LeaveApplication[]>(`/leave-applications/approver/${encodeURIComponent(approverId)}`);

export const approveLeave = (id: string, approverId: string) =>
  apiFetch<LeaveApplication>(`/leave-applications/${encodeURIComponent(id)}/approve?approverId=${encodeURIComponent(approverId)}`, { method: 'PUT' });

export const rejectLeave = (id: string, approverId: string) =>
  apiFetch<LeaveApplication>(`/leave-applications/${encodeURIComponent(id)}/reject?approverId=${encodeURIComponent(approverId)}`, { method: 'PUT' });

export const approveCancellation = (id: string) =>
  apiFetch<LeaveApplication>(`/leave-applications/${encodeURIComponent(id)}/approve-cancellation`, { method: 'PUT' });

export const rejectCancellation = (id: string) =>
  apiFetch<LeaveApplication>(`/leave-applications/${encodeURIComponent(id)}/reject-cancellation`, { method: 'PUT' });
