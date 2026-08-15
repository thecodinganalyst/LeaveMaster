import { describe, expect, it } from 'vitest';

import { getJurisdictionOptions } from './jurisdictions.ts';

describe('getJurisdictionOptions', () => {
  it('uses jurisdiction names as labels and codes as option values', () => {
    expect(getJurisdictionOptions([
      { id: 'SG', code: 'SG', name: 'Singapore' },
      { id: 'AU', code: 'AU', name: 'Australia' },
    ])).toEqual([
      { label: 'Australia', value: 'AU' },
      { label: 'Singapore', value: 'SG' },
    ]);
  });

  it('prefixes child jurisdictions with the parent name', () => {
    expect(getJurisdictionOptions([
      { id: 'AU', code: 'AU', name: 'Australia' },
      { id: 'AU-NSW', code: 'AU-NSW', name: 'New South Wales', parentId: 'AU' },
    ])).toEqual([
      { label: 'Australia', value: 'AU' },
      { label: 'Australia > New South Wales', value: 'AU-NSW' },
    ]);
  });

  it('falls back to the jurisdiction name when its parent is not in the response', () => {
    expect(getJurisdictionOptions([
      { id: 'AU-NSW', code: 'AU-NSW', name: 'New South Wales', parentId: 'AU' },
    ])).toEqual([
      { label: 'New South Wales', value: 'AU-NSW' },
    ]);
  });
});
