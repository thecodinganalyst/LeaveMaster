import type { LeaveApplication, LeaveStatus } from './leaveApi.ts';

export const statusLabel: Record<LeaveStatus, string> = {
  DRAFT: 'Draft',
  PENDING_VERIFICATION: 'Awaiting event verification',
  PENDING: 'Pending',
  APPROVED: 'Approved',
  CANCEL_REQUESTED: 'Cancellation pending',
  CANCELLED: 'Cancelled',
  DENIED: 'Denied',
};

export const statusColor: Record<LeaveStatus, string> = {
  DRAFT: 'default',
  PENDING_VERIFICATION: 'warning',
  PENDING: 'processing',
  APPROVED: 'success',
  CANCEL_REQUESTED: 'warning',
  CANCELLED: 'default',
  DENIED: 'error',
};

export const canEditApplication = (application: LeaveApplication) =>
  ['DRAFT', 'PENDING_VERIFICATION', 'PENDING'].includes(application.status);

export const canCancelApplication = (application: LeaveApplication) =>
  !['CANCELLED', 'DENIED', 'CANCEL_REQUESTED'].includes(application.status);

export const isUpcoming = (application: LeaveApplication, today = new Date()) => {
  const todayText = today.toISOString().slice(0, 10);
  return application.leaveDate >= todayText && !['CANCELLED', 'DENIED'].includes(application.status);
};

export const sortByLeaveDate = (applications: LeaveApplication[]) =>
  [...applications].sort((a, b) => a.leaveDate.localeCompare(b.leaveDate));
