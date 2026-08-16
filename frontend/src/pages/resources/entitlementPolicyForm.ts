import type { KeyboardEvent } from 'react';

export const normalisePolicyIdPart = (value: string) => value
  .trim()
  .toUpperCase()
  .replace(/[^A-Z0-9]+/g, '_')
  .replace(/_+/g, '_')
  .replace(/^_|_$/g, '');

export const generateEntitlementPolicyId = (jurisdiction: string, name: string) => {
  const jurisdictionPart = normalisePolicyIdPart(jurisdiction);
  const namePart = normalisePolicyIdPart(name);
  return [jurisdictionPart, namePart].filter(Boolean).join('_');
};

export const sanitiseNumericInput = (value: string | undefined, integer: boolean) => {
  const raw = value ?? '';
  if (integer) return raw.replace(/[^0-9]/g, '');

  const cleaned = raw.replace(/[^0-9.]/g, '');
  const [whole, ...decimalParts] = cleaned.split('.');
  return decimalParts.length ? `${whole}.${decimalParts.join('')}` : whole;
};

export const blockInvalidNumericKey = (event: KeyboardEvent<HTMLInputElement>, integer: boolean) => {
  if (event.ctrlKey || event.metaKey || event.altKey) return;
  if (event.key.length !== 1) return;
  if (/^[0-9]$/.test(event.key)) return;
  if (!integer && event.key === '.') return;
  event.preventDefault();
};
