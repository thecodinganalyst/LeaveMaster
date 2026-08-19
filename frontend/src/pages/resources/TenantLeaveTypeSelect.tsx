import { useQuery } from '@tanstack/react-query';
import { Select } from 'antd';

import { apiFetch } from '../../api/http.ts';

interface LeaveTypeOptionSource {
  id: string;
  name: string;
}

interface Props {
  value?: string;
  onChange?: (value: string) => void;
  disabled?: boolean;
}

const loadTenantLeaveTypes = () => apiFetch<LeaveTypeOptionSource[]>('/api/leave-types');

export const TenantLeaveTypeSelect = ({ value, onChange, disabled = false }: Props) => {
  const leaveTypesQuery = useQuery({
    queryKey: ['leave-types', 'tenant-options'],
    queryFn: loadTenantLeaveTypes,
    staleTime: 5 * 60 * 1000,
  });

  const options = (leaveTypesQuery.data ?? []).map((leaveType) => ({
    label: `${leaveType.name} (${leaveType.id})`,
    value: leaveType.id,
  }));

  let placeholder = 'Select a leave type';
  if (leaveTypesQuery.isError) placeholder = 'Unable to load leave types';
  else if (!leaveTypesQuery.isLoading && options.length === 0) placeholder = 'No leave types available for this tenant';

  return (
    <Select
      {...(value === undefined ? {} : { value })}
      {...(onChange === undefined ? {} : { onChange })}
      disabled={disabled || leaveTypesQuery.isError}
      loading={leaveTypesQuery.isLoading}
      options={options}
      showSearch
      optionFilterProp="label"
      placeholder={placeholder}
    />
  );
};
