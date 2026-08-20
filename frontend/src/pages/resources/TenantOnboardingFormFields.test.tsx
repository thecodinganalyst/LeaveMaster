import { fireEvent, render, screen } from '@testing-library/react';
import { Form } from 'antd';
import { describe, expect, it, vi } from 'vitest';

import { TenantOnboardingFormFields } from './TenantOnboardingFormFields.tsx';

vi.mock('./JurisdictionSelect.tsx', () => ({
  JurisdictionSelect: () => <select aria-label="Jurisdiction"><option value="SG">Singapore</option></select>,
}));

describe('TenantOnboardingFormFields', () => {
  it('supports multiple jurisdictions with independent template options', () => {
    render(
      <Form initialValues={{ jurisdictions: [{ includePublicHolidays: true, includeLeaveConfiguration: true }] }}>
        <TenantOnboardingFormFields />
      </Form>,
    );

    expect(screen.getByText('Jurisdiction 1')).toBeInTheDocument();
    expect(screen.getAllByText('Add public holidays from template')).toHaveLength(1);
    expect(screen.getAllByText('Add leave types, entitlement policies and eligibility rules from template')).toHaveLength(1);

    fireEvent.click(screen.getByRole('button', { name: /Add jurisdiction/i }));

    expect(screen.getByText('Jurisdiction 2')).toBeInTheDocument();
    expect(screen.getAllByText('Add public holidays from template')).toHaveLength(2);
    expect(screen.getAllByText('Add leave types, entitlement policies and eligibility rules from template')).toHaveLength(2);
    expect(screen.getAllByRole('switch')).toHaveLength(4);
  });
});
