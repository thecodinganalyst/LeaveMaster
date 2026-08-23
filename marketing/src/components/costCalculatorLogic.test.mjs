import assert from 'node:assert/strict';
import test from 'node:test';

import {
  calculateManualAdministrationCost,
  formatCurrency,
  normalizeNonNegativeNumber,
} from './costCalculatorLogic.mjs';

test('calculates annual hours and annual manual administration cost', () => {
  assert.deepEqual(
    calculateManualAdministrationCost({ hoursPerMonth: 12, hourlyCost: 45 }),
    { annualHours: 144, annualCost: 6480 },
  );
});

test('supports numeric strings from form inputs', () => {
  assert.deepEqual(
    calculateManualAdministrationCost({ hoursPerMonth: '10.5', hourlyCost: '50' }),
    { annualHours: 126, annualCost: 6300 },
  );
});

test('normalizes negative and invalid inputs to zero', () => {
  assert.equal(normalizeNonNegativeNumber(-2), 0);
  assert.equal(normalizeNonNegativeNumber('not-a-number'), 0);
  assert.deepEqual(
    calculateManualAdministrationCost({ hoursPerMonth: -5, hourlyCost: 50 }),
    { annualHours: 0, annualCost: 0 },
  );
});

test('formats supported currencies and safely falls back to SGD', () => {
  assert.match(formatCurrency(6480, 'USD', 'en-US'), /\$6,480/);
  assert.match(formatCurrency(6480, 'INVALID', 'en-SG'), /6,480/);
});
