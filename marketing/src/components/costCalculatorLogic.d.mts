export const supportedCurrencies: string[];

export function normalizeNonNegativeNumber(value: string | number): number;

export function calculateManualAdministrationCost(input: {
  hoursPerMonth: string | number;
  hourlyCost: string | number;
}): {
  annualHours: number;
  annualCost: number;
};

export function formatCurrency(amount: string | number, currency?: string, locale?: string): string;
