import { useQuery } from '@tanstack/react-query';
import { Select } from 'antd';

import { apiFetch } from '../../api/http.ts';
import { getJurisdictionLeaveTypeOptions, type JurisdictionLeaveTypeOptionSource } from './jurisdictionLeaveTypes.ts';

interface Props {
  jurisdictionId?: string;
  value?: string;
  onChange?: (value: string) => void;
  disabled?: boolean;
}

const loadJurisdictionLeaveTypes = () => apiFetch<JurisdictionLeaveTypeOptionSource[]>('/api/jurisdiction-leave-types');

export const JurisdictionLeaveTypeSelect = ({ jurisdictionId, value, onChange, disabled = false }: Props) => {
  const leaveTypesQuery = useQuery({
    queryKey: ['jurisdiction-leave-types', 'options'],
    queryFn: loadJurisdictionLeaveTypes,
    staleTime: 5 * 60 * 1000,
  });

  const noJurisdiction = !jurisdictionId;
  const options = getJurisdictionLeaveTypeOptions(leaveTypesQuery.data ?? [], jurisdictionId, value);

  let placeholder = 'Select a jurisdiction leave type';
  if (noJurisdiction) placeholder = 'Select a jurisdiction first';
  else if (leaveTypesQuery.isError) placeholder = 'Unable to load jurisdiction leave types';
  else if (!leaveTypesQuery.isLoading && options.length === 0) placeholder = 'No leave types available for this jurisdiction';

  return (
    <Select
      {...(value === undefined ? {} : { value })}
      {...(onChange === undefined ? {} : { onChange })}
      disabled={disabled || noJurisdiction || leaveTypesQuery.isError}
      loading={leaveTypesQuery.isLoading}
      options={options}
      showSearch
      optionFilterProp="label"
      placeholder={placeholder}
    />
  );
};
