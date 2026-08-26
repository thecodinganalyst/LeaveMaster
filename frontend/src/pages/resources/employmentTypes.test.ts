import { describe, expect, it } from 'vitest';

import { EMPLOYMENT_TYPE_OPTIONS, employmentTypeLabel } from './employmentTypes.ts';

describe('employment types', () => {
  it('uses the backend stable codes with human-readable labels', () => {
    expect(EMPLOYMENT_TYPE_OPTIONS).toEqual([
      { label: 'Full Time', value: 'FULL_TIME' },
      { label: 'Part Time', value: 'PART_TIME' },
      { label: 'Casual', value: 'CASUAL' },
      { label: 'Contract', value: 'CONTRACT' },
      { label: 'Intern', value: 'INTERN' },
    ]);
  });

  it('shows legacy null values as not specified', () => {
    expect(employmentTypeLabel(null)).toBe('Not specified');
    expect(employmentTypeLabel(undefined)).toBe('Not specified');
    expect(employmentTypeLabel('')).toBe('Not specified');
  });

  it('formats supported values and preserves unexpected values for diagnosis', () => {
    expect(employmentTypeLabel('FULL_TIME')).toBe('Full Time');
    expect(employmentTypeLabel('PART_TIME')).toBe('Part Time');
    expect(employmentTypeLabel('UNKNOWN')).toBe('UNKNOWN');
  });
});
