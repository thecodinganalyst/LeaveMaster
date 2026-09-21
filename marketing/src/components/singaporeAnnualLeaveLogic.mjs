function parseDate(value) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return null;
  const [year, month, day] = value.split('-').map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) return null;
  return { year, month, day };
}

function compareDate(a, b) {
  return a.year - b.year || a.month - b.month || a.day - b.day;
}

export function completedMonthsBetween(startValue, endValue) {
  const start = parseDate(startValue);
  const end = parseDate(endValue);
  if (!start || !end || compareDate(end, start) < 0) return null;
  let months = (end.year - start.year) * 12 + end.month - start.month;
  if (end.day < start.day) months -= 1;
  return Math.max(0, months);
}

export function statutoryAnnualLeaveDays(yearOfService) {
  if (!Number.isInteger(yearOfService) || yearOfService < 1) return null;
  return Math.min(14, 6 + yearOfService);
}

export function roundMomLeaveDays(days) {
  if (!Number.isFinite(days) || days < 0) return null;
  return Math.floor(days + 0.5);
}

export function calculateSingaporeAnnualLeave({ startDate, calculationDate, employmentEnded = false }) {
  const completedMonths = completedMonthsBetween(startDate, calculationDate);
  if (completedMonths === null) return { valid: false, error: 'Enter valid dates with the calculation date on or after the start date.' };

  const completedYears = Math.floor(completedMonths / 12);
  const yearOfService = completedYears + 1;
  const annualTierDays = statutoryAnnualLeaveDays(yearOfService);
  const eligible = completedMonths >= 3;

  if (!eligible) {
    return { valid: true, eligible: false, completedMonths, completedYears, yearOfService, annualTierDays, entitlementDays: 0, prorated: true };
  }

  if (!employmentEnded && completedMonths >= 12) {
    return { valid: true, eligible: true, completedMonths, completedYears, yearOfService, annualTierDays, entitlementDays: annualTierDays, prorated: false };
  }

  const monthsInCurrentServiceYear = completedMonths < 12 ? completedMonths : completedMonths % 12;
  const entitlementDays = roundMomLeaveDays((monthsInCurrentServiceYear / 12) * annualTierDays);
  return { valid: true, eligible: true, completedMonths, completedYears, yearOfService, annualTierDays, entitlementDays, prorated: true };
}
