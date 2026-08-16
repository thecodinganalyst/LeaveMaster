import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { Button, Form } from 'antd';
import { describe, expect, it, vi } from 'vitest';

vi.mock('./JurisdictionSelect.tsx', () => ({
  JurisdictionSelect: ({ value, onChange, disabled = false }: { value?: string; onChange?: (value: string) => void; disabled?: boolean }) => (
    <select aria-label="Jurisdiction" value={value ?? ''} onChange={(event) => onChange?.(event.target.value)} disabled={disabled}>
      <option value="">Select a jurisdiction</option>
      <option value="SG">Singapore</option>
      <option value="AU">Australia</option>
    </select>
  ),
}));

vi.mock('./JurisdictionLeaveTypeSelect.tsx', () => ({
  JurisdictionLeaveTypeSelect: ({ jurisdictionId, value, onChange, disabled = false }: {
    jurisdictionId?: string;
    value?: string;
    onChange?: (value: string) => void;
    disabled?: boolean;
  }) => (
    <select
      aria-label="Jurisdiction leave type"
      data-jurisdiction={jurisdictionId ?? ''}
      value={value ?? ''}
      onChange={(event) => onChange?.(event.target.value)}
      disabled={disabled || !jurisdictionId}
    >
      <option value="">Select a jurisdiction leave type</option>
      {jurisdictionId === 'SG' && <option value="SG:ANNUAL_LEAVE">Annual Leave (ANNUAL_LEAVE)</option>}
      {jurisdictionId === 'AU' && <option value="AU:ANNUAL_LEAVE">Annual Leave (ANNUAL_LEAVE)</option>}
    </select>
  ),
}));

import { EntitlementPolicyFormFields } from './EntitlementPolicyFormFields.tsx';
import { generateEntitlementPolicyId, normalisePolicyIdPart, sanitiseNumericInput } from './entitlementPolicyForm.ts';
import { getJurisdictionLeaveTypeOptions } from './jurisdictionLeaveTypes.ts';

describe('EntitlementPolicyFormFields', () => {
  it('generates a deterministic readable policy id from jurisdiction and name', () => {
    expect(normalisePolicyIdPart(' Standard  annual-leave! ')).toBe('STANDARD_ANNUAL_LEAVE');
    expect(generateEntitlementPolicyId('SG', 'Standard Annual Leave')).toBe('SG_STANDARD_ANNUAL_LEAVE');
  });

  it('sanitises numeric input without losing valid decimals', () => {
    expect(sanitiseNumericInput('abc12.5xyz', false)).toBe('12.5');
    expect(sanitiseNumericInput('12.5', true)).toBe('125');
  });

  it('filters jurisdiction leave type options and uses readable labels with ids as values', () => {
    const options = getJurisdictionLeaveTypeOptions([
      { id: 'SG:ANNUAL_LEAVE', jurisdictionId: 'SG', code: 'ANNUAL_LEAVE', name: 'Annual Leave', active: true },
      { id: 'SG:OLD_LEAVE', jurisdictionId: 'SG', code: 'OLD_LEAVE', name: 'Old Leave', active: false },
      { id: 'AU:ANNUAL_LEAVE', jurisdictionId: 'AU', code: 'ANNUAL_LEAVE', name: 'Annual Leave', active: true },
    ], 'SG');

    expect(options).toEqual([{ label: 'Annual Leave (ANNUAL_LEAVE)', value: 'SG:ANNUAL_LEAVE' }]);
    expect(getJurisdictionLeaveTypeOptions([
      { id: 'SG:OLD_LEAVE', jurisdictionId: 'SG', code: 'OLD_LEAVE', name: 'Old Leave', active: false },
    ], 'SG', 'SG:OLD_LEAVE')).toEqual([{ label: 'Old Leave (OLD_LEAVE)', value: 'SG:OLD_LEAVE' }]);
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

    expect(screen.getByText('Days')).toBeInTheDocument();
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

  it('updates leave type options with jurisdiction and clears the previous selection', async () => {
    render(
      <Form initialValues={{ jurisdictionId: 'SG', jurisdictionLeaveTypeId: 'SG:ANNUAL_LEAVE', name: 'Policy' }}>
        <EntitlementPolicyFormFields platformAdmin />
      </Form>,
    );

    const leaveType = screen.getByRole('combobox', { name: 'Jurisdiction leave type' });
    expect(leaveType).toHaveAttribute('data-jurisdiction', 'SG');
    expect(leaveType).toHaveValue('SG:ANNUAL_LEAVE');

    fireEvent.change(screen.getByRole('combobox', { name: 'Jurisdiction' }), { target: { value: 'AU' } });

    await waitFor(() => expect(leaveType).toHaveAttribute('data-jurisdiction', 'AU'));
    await waitFor(() => expect(leaveType).toHaveValue(''));
  });

  it('submits the jurisdiction leave type id selected from the dropdown', async () => {
    const onFinish = vi.fn();
    render(
      <Form initialValues={{ jurisdictionId: 'SG', name: 'Policy' }} onFinish={onFinish}>
        <EntitlementPolicyFormFields platformAdmin />
        <Button htmlType="submit">Save</Button>
      </Form>,
    );

    fireEvent.change(screen.getByRole('combobox', { name: 'Jurisdiction leave type' }), { target: { value: 'SG:ANNUAL_LEAVE' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    await waitFor(() => expect(onFinish).toHaveBeenCalled());
    expect(onFinish.mock.calls[0]?.[0]).toMatchObject({ jurisdictionId: 'SG', jurisdictionLeaveTypeId: 'SG:ANNUAL_LEAVE' });
  });

  it('locks jurisdiction and jurisdiction leave type during edit', () => {
    render(
      <Form initialValues={{
        id: 'SG_POLICY',
        jurisdictionId: 'SG',
        jurisdictionLeaveTypeId: 'SG:ANNUAL_LEAVE',
        name: 'Existing policy',
      }}>
        <EntitlementPolicyFormFields editing platformAdmin />
      </Form>,
    );

    expect(screen.getByRole('combobox', { name: 'Jurisdiction' })).toBeDisabled();
    expect(screen.getByRole('combobox', { name: 'Jurisdiction leave type' })).toBeDisabled();
    expect(screen.getByRole('combobox', { name: 'Jurisdiction leave type' })).toHaveValue('SG:ANNUAL_LEAVE');
    expect(screen.getByRole('textbox', { name: 'ID' })).toBeDisabled();
  });

  it('blocks alphabetic keystrokes in numeric fields while allowing decimal input', () => {
    render(
      <Form initialValues={{ leaveTypeId: 'SG_ANNUAL_LEAVE', name: 'Standard Annual Leave' }}>
        <EntitlementPolicyFormFields />
      </Form>,
    );

    const amount = screen.getByRole('spinbutton', { name: 'Entitlement amount' });
    expect(fireEvent.keyDown(amount, { key: 'a' })).toBe(false);
    expect(fireEvent.keyDown(amount, { key: '.' })).toBe(true);

    const priority = screen.getByRole('spinbutton', { name: /Priority/i });
    expect(fireEvent.keyDown(priority, { key: 'b' })).toBe(false);
    expect(fireEvent.keyDown(priority, { key: '.' })).toBe(false);
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
