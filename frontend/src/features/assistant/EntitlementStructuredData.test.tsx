import { render, screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import type { StructuredResult } from './assistantApi.ts';
import { EntitlementStructuredData, isEntitlementStructuredResult } from './EntitlementStructuredData.tsx';

const result: StructuredResult = {
  toolName: 'getLeaveEntitlementConfigurationByJurisdiction',
  data: [
    {
      leaveType: 'Annual Leave',
      accrual: 'Granted upfront',
      proration: 'Not prorated',
      carryForward: 'Unused leave cannot be carried forward',
      policies: [
        {
          policyName: '1st year',
          servicePeriod: '3–11 months',
          eligibility: null,
          entitlement: '7 days',
          accrual: null,
          proration: null,
          carryForward: null,
        },
        {
          policyName: '2nd year',
          servicePeriod: '12–23 months',
          eligibility: 'Jurisdiction is SG',
          entitlement: '8 days',
          accrual: null,
          proration: 'Prorated by calendar days',
          carryForward: null,
        },
      ],
    },
  ],
};

describe('EntitlementStructuredData', () => {
  it('recognizes only the entitlement configuration structured tool', () => {
    expect(isEntitlementStructuredResult(result)).toBe(true);
    expect(isEntitlementStructuredResult({ toolName: 'getLeaveBalances', data: [] })).toBe(false);
  });

  it('renders exact service ranges in a compact tier table and common settings once', () => {
    render(<EntitlementStructuredData result={result} />);

    expect(screen.getByText('Leave entitlement details')).toBeInTheDocument();
    expect(screen.getByText('Annual Leave')).toBeInTheDocument();
    expect(screen.getByText('3–11 months')).toBeInTheDocument();
    expect(screen.getByText('12–23 months')).toBeInTheDocument();
    expect(screen.queryByText('3 to 12 months')).not.toBeInTheDocument();
    expect(screen.getByText('Granted upfront')).toBeInTheDocument();
    expect(screen.getAllByText('Not prorated')).toHaveLength(1);
    expect(screen.getByText('Proration: Prorated by calendar days')).toBeInTheDocument();

    const table = screen.getByRole('table');
    expect(within(table).getByText('Service period')).toBeInTheDocument();
    expect(within(table).getByText('Entitlement')).toBeInTheDocument();
    expect(within(table).getByText('Additional eligibility')).toBeInTheDocument();
    expect(within(table).getByText('Policy exceptions')).toBeInTheDocument();
    expect(screen.queryByText(/Policy: 1st year · Eligibility:/)).not.toBeInTheDocument();
  });

  it('handles empty authoritative results without falling back to a raw object dump', () => {
    render(<EntitlementStructuredData result={{ ...result, data: [] }} />);
    expect(screen.getByText('No entitlement policies found.')).toBeInTheDocument();
  });
});
