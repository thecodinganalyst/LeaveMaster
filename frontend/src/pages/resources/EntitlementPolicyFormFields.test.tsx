import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { Form } from 'antd';
import { describe, expect, it } from 'vitest';

import { EntitlementPolicyFormFields, generateEntitlementPolicyId, normalisePolicyIdPart } from './EntitlementPolicyFormFields.tsx';

describe('EntitlementPolicyFormFields', () => {
  it('generates a deterministic readable policy id from jurisdiction and name', () => {
    expect(normalisePolicyIdPart(' Standard  annual-leave! ')).toBe('STANDARD_ANNUAL_LEAVE');
    expect(generateEntitlementPolicyId('SG', 'Standard Annual Leave')).toBe('SG_STANDARD_ANNUAL_LEAVE');
  });

  it('renders policy guidance, days-only unit, and numeric controls', () => {
    render(
      <Form initialValues={{
        leaveTypeId: 'SG_ANNUAL_LEAVE',
        name: 'Standard Annual Leave',
        active: true,
        priority: 10,
        entitlementAmount: 14,
        entitlementUnit: 'DAYS',
        accrualMethod: 'NONE',
        prorationMethod: 'NONE',
      }}>
        <EntitlementPolicyFormFields />
      </Form>,
    );

    expect(screen.getByText('What an entitlement policy controls')).toBeInTheDocument();
    expect(screen.getByText(/Eligibility rules determine which employees/i)).toBeInTheDocument();
    expect(screen.getByText(/CALENDAR_DAYS: prorate by eligible calendar days/i)).toBeInTheDocument();
    expect(screen.getByText(/PER_PAY_PERIOD accrues by payroll period/i)).toBeInTheDocument();

    expect(screen.getByRole('combobox', { name: 'Unit' })).toHaveTextContent('Days');
    expect(screen.queryByText('HOURS')).not.toBeInTheDocument();
    expect(screen.queryByText('Hours')).not.toBeInTheDocument();

    expect(screen.getByRole('spinbutton', { name: /Priority/i })).toBeInTheDocument();
    expect(screen.getByRole('spinbutton', { name: 'Entitlement amount' })).toBeInTheDocument();
    expect(screen.getByRole('spinbutton', { name: 'Accrual rate' })).toBeDisabled();
    expect(screen.getByRole('spinbutton', { name: 'Carry forward limit' })).toBeInTheDocument();
    expect(screen.getByRole('spinbutton', { name: /Carry forward expiry/i })).toBeInTheDocument();
  });

  it('auto-generates an id and keeps a user override', async () => {
    render(
      <Form initialValues={{ leaveTypeId: 'SG_ANNUAL_LEAVE', name: 'Standard Annual Leave' }}>
        <EntitlementPolicyFormFields />
      </Form>,
    );

    const idInput = screen.getByRole('textbox', { name: 'ID' });
    await waitFor(() => expect(idInput).toHaveValue('SG_ANNUAL_LEAVE_STANDARD_ANNUAL_LEAVE'));

    fireEvent.change(idInput, { target: { value: 'MY_CUSTOM_POLICY' } });
    fireEvent.change(screen.getByRole('textbox', { name: 'Name' }), { target: { value: 'Updated Name' } });

    await waitFor(() => expect(idInput).toHaveValue('MY_CUSTOM_POLICY'));
  });

  it('does not display alphabetic characters in numeric fields', () => {
    render(
      <Form initialValues={{ leaveTypeId: 'SG_ANNUAL_LEAVE', name: 'Standard Annual Leave' }}>
        <EntitlementPolicyFormFields />
      </Form>,
    );

    const amount = screen.getByRole('spinbutton', { name: 'Entitlement amount' });
    fireEvent.change(amount, { target: { value: 'abc' } });
    expect(amount).not.toHaveValue('abc');
  });

  it('keeps an existing id fixed during edit', () => {
    render(
      <Form initialValues={{ id: 'EXISTING_POLICY', leaveTypeId: 'SG_ANNUAL_LEAVE', name: 'Existing policy' }}>
        <EntitlementPolicyFormFields editing />
      </Form>,
    );

    expect(screen.getByRole('textbox', { name: 'ID' })).toBeDisabled();
    expect(screen.getByRole('textbox', { name: 'ID' })).toHaveValue('EXISTING_POLICY');
  });
});
