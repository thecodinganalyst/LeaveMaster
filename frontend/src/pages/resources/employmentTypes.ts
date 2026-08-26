export const EMPLOYMENT_TYPE_OPTIONS = [
  { label: 'Full Time', value: 'FULL_TIME' },
  { label: 'Part Time', value: 'PART_TIME' },
  { label: 'Casual', value: 'CASUAL' },
  { label: 'Contract', value: 'CONTRACT' },
  { label: 'Intern', value: 'INTERN' },
] as const;

export type EmploymentType = typeof EMPLOYMENT_TYPE_OPTIONS[number]['value'];

export const employmentTypeLabel = (value: unknown) => {
  if (value == null || value === '') return 'Not specified';
  return EMPLOYMENT_TYPE_OPTIONS.find((option) => option.value === value)?.label ?? String(value);
};
