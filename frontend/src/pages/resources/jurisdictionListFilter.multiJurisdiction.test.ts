import { describe, expect, it } from 'vitest';

import { filterRecordsByJurisdiction, getRecordJurisdictionId } from './jurisdictionListFilter.ts';

describe('multi-jurisdiction leave type filtering', () => {
  it('uses the applicable child jurisdiction before inherited source provenance', () => {
    const sourceMap = new Map([['AU:ANNUAL_LEAVE', 'AU']]);
    const record = {
      id: 'tenant:AU-NSW:ANNUAL_LEAVE',
      jurisdictionId: 'AU-NSW',
      sourceJurisdictionLeaveTypeId: 'AU:ANNUAL_LEAVE',
    };

    expect(getRecordJurisdictionId('leave-types', record, sourceMap)).toBe('AU-NSW');
  });

  it('keeps inherited federal leave types visible under each applicable state', () => {
    const sourceMap = new Map([['AU:ANNUAL_LEAVE', 'AU']]);
    const records = [
      {
        id: 'tenant:AU-NSW:ANNUAL_LEAVE',
        jurisdictionId: 'AU-NSW',
        sourceJurisdictionLeaveTypeId: 'AU:ANNUAL_LEAVE',
      },
      {
        id: 'tenant:AU-WA:ANNUAL_LEAVE',
        jurisdictionId: 'AU-WA',
        sourceJurisdictionLeaveTypeId: 'AU:ANNUAL_LEAVE',
      },
    ];

    expect(filterRecordsByJurisdiction('leave-types', records, 'AU-NSW', sourceMap))
      .toEqual([records[0]]);
    expect(filterRecordsByJurisdiction('leave-types', records, 'AU-WA', sourceMap))
      .toEqual([records[1]]);
  });
});
