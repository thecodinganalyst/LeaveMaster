import { useQuery } from '@tanstack/react-query';

import { apiFetch } from '../../api/http.ts';

interface LeaveTypeOptionSource {
  id: string;
  name: string;
}

interface Props {
  leaveTypeId: string;
}

const loadTenantLeaveTypes = () => apiFetch<LeaveTypeOptionSource[]>('/api/leave-types');

export const TenantLeaveTypeName = ({ leaveTypeId }: Props) => {
  const leaveTypesQuery = useQuery({
    queryKey: ['leave-types', 'tenant-options'],
    queryFn: loadTenantLeaveTypes,
    staleTime: 5 * 60 * 1000,
  });

  if (leaveTypesQuery.isLoading) return <>Loading…</>;
  if (leaveTypesQuery.isError) return <>Unavailable</>;

  const leaveType = (leaveTypesQuery.data ?? []).find((item) => item.id === leaveTypeId);
  return <>{leaveType?.name ?? 'Unknown leave type'}</>;
};
