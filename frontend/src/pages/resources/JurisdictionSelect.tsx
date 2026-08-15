import { useQuery } from '@tanstack/react-query';
import { Select } from 'antd';

import { apiFetch } from '../../api/http.ts';
import { getJurisdictionOptions, type JurisdictionOptionSource } from './jurisdictions.ts';

interface Props {
  value?: string;
  onChange?: (value: string) => void;
  disabled?: boolean;
}

const loadJurisdictions = () => apiFetch<JurisdictionOptionSource[]>('/api/jurisdictions');

export const JurisdictionSelect = ({ value, onChange, disabled = false }: Props) => {
  const jurisdictionsQuery = useQuery({
    queryKey: ['jurisdictions', 'options'],
    queryFn: loadJurisdictions,
    staleTime: 5 * 60 * 1000,
  });

  return (
    <Select
      value={value}
      onChange={onChange}
      disabled={disabled || jurisdictionsQuery.isError}
      loading={jurisdictionsQuery.isLoading}
      options={getJurisdictionOptions(jurisdictionsQuery.data ?? [])}
      showSearch
      optionFilterProp="label"
      placeholder={jurisdictionsQuery.isError ? 'Unable to load jurisdictions' : 'Select a jurisdiction'}
    />
  );
};
