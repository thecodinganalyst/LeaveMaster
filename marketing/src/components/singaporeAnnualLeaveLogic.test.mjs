import assert from 'node:assert/strict';
import test from 'node:test';
import { calculateSingaporeAnnualLeave, completedMonthsBetween, roundMomLeaveDays, statutoryAnnualLeaveDays } from './singaporeAnnualLeaveLogic.mjs';

test('counts only completed months of service', () => {
  assert.equal(completedMonthsBetween('2026-01-14', '2026-05-13'), 3);
  assert.equal(completedMonthsBetween('2026-01-14', '2026-05-14'), 4);
});

test('rejects invalid dates and reversed periods', () => {
  assert.equal(completedMonthsBetween('2026-02-30', '2026-05-30'), null);
  assert.equal(completedMonthsBetween('2026-05-01', '2026-04-30'), null);
});

test('uses statutory tiers from 7 days to the 14 day cap', () => {
  assert.equal(statutoryAnnualLeaveDays(1), 7);
  assert.equal(statutoryAnnualLeaveDays(7), 13);
  assert.equal(statutoryAnnualLeaveDays(8), 14);
  assert.equal(statutoryAnnualLeaveDays(20), 14);
  assert.equal(statutoryAnnualLeaveDays(0), null);
});

test('rounds MOM prorated results to the nearest whole day with halves up', () => {
  assert.equal(roundMomLeaveDays(3.33), 3);
  assert.equal(roundMomLeaveDays(3.5), 4);
  assert.equal(roundMomLeaveDays(-1), null);
});

test('shows no statutory paid annual leave before three completed months', () => {
  const result = calculateSingaporeAnnualLeave({ startDate: '2026-01-14', calculationDate: '2026-04-13' });
  assert.equal(result.valid, true);
  assert.equal(result.eligible, false);
  assert.equal(result.entitlementDays, 0);
});

test('prorates first-year entitlement after three completed months', () => {
  const result = calculateSingaporeAnnualLeave({ startDate: '2026-01-14', calculationDate: '2026-07-14' });
  assert.equal(result.completedMonths, 6);
  assert.equal(result.annualTierDays, 7);
  assert.equal(result.entitlementDays, 4);
  assert.equal(result.prorated, true);
});

test('returns the full current tier for an ongoing employee after one year', () => {
  const result = calculateSingaporeAnnualLeave({ startDate: '2024-03-14', calculationDate: '2026-03-14' });
  assert.equal(result.yearOfService, 3);
  assert.equal(result.annualTierDays, 9);
  assert.equal(result.entitlementDays, 9);
  assert.equal(result.prorated, false);
});

test('prorates the current service year when employment has ended', () => {
  const result = calculateSingaporeAnnualLeave({ startDate: '2024-03-14', calculationDate: '2025-07-31', employmentEnded: true });
  assert.equal(result.yearOfService, 2);
  assert.equal(result.annualTierDays, 8);
  assert.equal(result.entitlementDays, 3);
  assert.equal(result.prorated, true);
});

test('returns a validation error when calculation date precedes start date', () => {
  const result = calculateSingaporeAnnualLeave({ startDate: '2026-06-01', calculationDate: '2026-05-31' });
  assert.equal(result.valid, false);
  assert.match(result.error, /on or after/);
});
