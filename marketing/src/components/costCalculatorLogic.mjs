export const supportedCurrencies = ['SGD', 'USD', 'AUD', 'GBP', 'EUR'];

export function normalizeNonNegativeNumber(value) {
  const parsed = typeof value === 'number' ? value : Number(value);
  if (!Number.isFinite(parsed) || parsed < 0) return 0;
  return parsed;
}

export function calculateManualAdministrationCost({ hoursPerMonth, hourlyCost }) {
  const monthlyHours = normalizeNonNegativeNumber(hoursPerMonth);
  const costPerHour = normalizeNonNegativeNumber(hourlyCost);

  return {
    annualHours: monthlyHours * 12,
    annualCost: monthlyHours * costPerHour * 12,
  };
}

export function formatCurrency(amount, currency = 'SGD', locale = 'en-SG') {
  const safeCurrency = supportedCurrencies.includes(currency) ? currency : 'SGD';
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: safeCurrency,
    maximumFractionDigits: 0,
  }).format(normalizeNonNegativeNumber(amount));
}
