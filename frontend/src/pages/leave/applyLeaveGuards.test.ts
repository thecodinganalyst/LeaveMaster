import { describe, expect, it } from 'vitest';

import { normalizeLeaveTypes, normalizePolicyMetadata } from './applyLeaveGuards.ts';

describe('apply leave response guards', () => {
  it('normalizes non-array leave type payloads to an empty list', () => {
    expect(normalizeLeaveTypes(null)).toEqual([]);
    expect(normalizeLeaveTypes({ content: [] })).toEqual([]);
  });

  it('keeps only valid leave type records', () => {
    expect(normalizeLeaveTypes([
      { id: 'AL', name: 'Annual Leave' },
      null,
      { id: 'SICK' },
      { id: 3, name: 'Invalid' },
    ])).toEqual([{ id: 'AL', name: 'Annual Leave' }]);
  });

  it('rejects malformed policy metadata', () => {
    expect(normalizePolicyMetadata(null)).toBeUndefined();
    expect(normalizePolicyMetadata({ policyModel: 'EVENT_BASED' })).toBeUndefined();
  });

  it('accepts valid policy metadata', () => {
    expect(normalizePolicyMetadata({
      policyModel: 'EVENT_BASED',
      eventBased: true,
      eventRequiresVerification: true,
    })).toEqual({
      policyModel: 'EVENT_BASED',
      eventBased: true,
      eventRequiresVerification: true,
    });
  });
});
