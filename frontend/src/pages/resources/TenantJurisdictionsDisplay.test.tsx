import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { TenantJurisdictionsDisplay, tenantJurisdictionIds } from './TenantJurisdictionsDisplay.tsx';

describe('TenantJurisdictionsDisplay', () => {
  it('returns every jurisdiction from the enriched tenant response', () => {
    expect(tenantJurisdictionIds({
      jurisdictionId: 'SG',
      jurisdictionIds: ['AU-NSW', 'SG'],
    })).toEqual(['AU-NSW', 'SG']);
  });

  it('falls back to the legacy single jurisdiction', () => {
    expect(tenantJurisdictionIds({ jurisdictionId: 'SG' })).toEqual(['SG']);
  });

  it('renders all jurisdictions as separate tags', () => {
    render(<TenantJurisdictionsDisplay record={{ jurisdictionIds: ['AU-NSW', 'SG'] }} />);

    expect(screen.getByText('AU-NSW')).toBeInTheDocument();
    expect(screen.getByText('SG')).toBeInTheDocument();
  });

  it('renders an empty state when the tenant has no jurisdiction data', () => {
    render(<TenantJurisdictionsDisplay record={{}} />);

    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
