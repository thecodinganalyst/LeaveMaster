import { describe, expect, it } from 'vitest';

import {
  buildLeaveTypeJurisdictionMap,
  canUseJurisdictionFilter,
  filterRecordsByJurisdiction,
  getRecordJurisdictionId,
  supportsJurisdictionFilter,
} from './jurisdictionListFilter.ts';

describe('jurisdictionListFilter', () => {
  it('supports the leave type and leave calendar lists only', () => {
    expect(supportsJurisdictionFilter('leave-types')).toBe(true);
    expect(supportsJurisdictionFilter('leave-calendars')).toBe(true);
    expect(supportsJurisdictionFilter('employees')).toBe(false);
  });

  it('only enables tenant jurisdiction filtering when both supporting reads are authorized', () => {
    expect(canUseJurisdictionFilter(true, false, true, true)).toBe(true);
    expect(canUseJurisdictionFilter(true, false, false, true)).toBe(false);
    expect(canUseJurisdictionFilter(true, false, true, false)).toBe(false);
    expect(canUseJurisdictionFilter(false, false, true, true)).toBe(false);
  });

  it('lets platform administrators filter with jurisdiction read access alone', () => {
    expect(canUseJurisdictionFilter(true, true, true, false)).toBe(true);
    expect(canUseJurisdictionFilter(true, true, false, true)).toBe(false);
  });

  it('builds a source leave type to jurisdiction lookup', () => {
    const map = buildLeaveTypeJurisdictionMap([
      { id: 'SG:ANNUAL_LEAVE', jurisdictionId: 'SG' },
      { id: 'custom-au', jurisdictionId: 'AU-NSW' },
      { id: '', jurisdictionId: 'AU' },
    ]);

    expect(map.get('SG:ANNUAL_LEAVE')).toBe('SG');
    expect(map.get('custom-au')).toBe('AU-NSW');
    expect(map.size).toBe(2);
  });

  it('resolves leave calendar jurisdiction directly and leave type jurisdiction from its source', () => {
    const map = new Map([['custom-au', 'AU-NSW']]);

    expect(getRecordJurisdictionId('leave-calendars', { jurisdictionId: 'SG' }, map)).toBe('SG');
    expect(getRecordJurisdictionId('leave-types', { sourceJurisdictionLeaveTypeId: 'custom-au' }, map)).toBe('AU-NSW');
    expect(getRecordJurisdictionId('leave-types', { jurisdictionId: 'AU', sourceJurisdictionLeaveTypeId: 'custom-au' }, map)).toBe('AU');
  });

  it('filters leave calendars by a selected jurisdiction', () => {
    const records = [
      { id: 'sg-2026', jurisdictionId: 'SG' },
      { id: 'au-2026', jurisdictionId: 'AU' },
    ];

    expect(filterRecordsByJurisdiction('leave-calendars', records, 'AU')).toEqual([
      { id: 'au-2026', jurisdictionId: 'AU' },
    ]);
  });

  it('filters leave types through their source jurisdiction leave type', () => {
    const records = [
      { id: 'annual-sg', sourceJurisdictionLeaveTypeId: 'SG:ANNUAL_LEAVE' },
      { id: 'annual-au', sourceJurisdictionLeaveTypeId: 'AU:ANNUAL_LEAVE' },
    ];
    const map = new Map([
      ['SG:ANNUAL_LEAVE', 'SG'],
      ['AU:ANNUAL_LEAVE', 'AU'],
    ]);

    expect(filterRecordsByJurisdiction('leave-types', records, 'SG', map)).toEqual([
      { id: 'annual-sg', sourceJurisdictionLeaveTypeId: 'SG:ANNUAL_LEAVE' },
    ]);
  });

  it('returns every record when All jurisdictions is selected', () => {
    const records = [
      { id: 'sg-2026', jurisdictionId: 'SG' },
      { id: 'au-2026', jurisdictionId: 'AU' },
    ];

    expect(filterRecordsByJurisdiction('leave-calendars', records, undefined)).toBe(records);
  });
});
