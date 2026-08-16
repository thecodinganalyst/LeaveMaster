import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { Form } from 'antd';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const useQueryMock = vi.fn();

vi.mock('@tanstack/react-query', () => ({
  useQuery: (options: unknown) => useQueryMock(options),
}));

import { EligibilityRuleFormFields } from './EligibilityRuleFormFields.tsx';

const queryResult = (data: unknown) => ({ data, isLoading: false, isError: false });

beforeEach(() => {
  useQueryMock.mockImplementation(({ queryKey }: { queryKey: string[] }) => {
    if (queryKey[0] === 'leave-entitlement-policies') {
      return queryResult([
        { id: 'SG_ANNUAL', name: 'Singapore Annual Leave', scope: 'TENANT', tenantId: 'ACME' },
        { id: 'SG_TEMPLATE', name: 'Singapore Template', scope: 'PLATFORM_TEMPLATE', tenantId: null },
      ]);
    }
    if (queryKey[0] === 'locations') {
      return queryResult([
        { id: 'SG-HQ', locationName: 'Singapore HQ' },
        { id: 'SG-EAST', locationName: 'Singapore East' },
      ]);
    }
    return queryResult([
      { id: 'SG', code: 'SG', name: 'Singapore' },
      { id: 'SG-01', code: 'SG-01', name: 'Central', parentId: 'SG' },
    ]);
  });
});

describe('EligibilityRuleFormFields', () => {
  it('uses a readable policy dropdown and initializes sort order to 10', async () => {
    render(
      <Form initialValues={{ policyId: 'SG_ANNUAL' }}>
        <EligibilityRuleFormFields />
      </Form>,
    );

    expect(screen.getByRole('combobox', { name: 'Policy' })).toBeInTheDocument();
    expect(screen.getByText(/Singapore Annual Leave/)).toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole('spinbutton', { name: 'Sort order' })).toHaveValue('10'));
    expect(screen.getByText(/does not change the result while all rules use AND logic/i)).toBeInTheDocument();
  });

  it('renders service months as integer-only numeric input with comparison operators', () => {
    render(
      <Form initialValues={{ policyId: 'SG_ANNUAL', criterionType: 'SERVICE_MONTHS', operator: 'GREATER_THAN_OR_EQUAL' }}>
        <EligibilityRuleFormFields />
      </Form>,
    );

    const value = screen.getByRole('spinbutton', { name: 'Value' });
    expect(value).toHaveAttribute('inputmode', 'numeric');
    expect(fireEvent.keyDown(value, { key: 'a' })).toBe(false);
    expect(fireEvent.keyDown(value, { key: '.' })).toBe(false);

    const operator = screen.getByRole('combobox', { name: 'Operator' });
    fireEvent.mouseDown(operator);
    expect(screen.getByText('Greater than or equal to')).toBeInTheDocument();
    expect(screen.getByText('Less than or equal to')).toBeInTheDocument();
  });

  it('uses a location dropdown and limits location operators to set comparisons', () => {
    render(
      <Form initialValues={{ policyId: 'SG_ANNUAL', criterionType: 'LOCATION_ID', operator: 'EQUALS' }}>
        <EligibilityRuleFormFields />
      </Form>,
    );

    const value = screen.getByRole('combobox', { name: 'Value' });
    fireEvent.mouseDown(value);
    expect(screen.getByText(/Singapore HQ/)).toBeInTheDocument();

    const operator = screen.getByRole('combobox', { name: 'Operator' });
    fireEvent.mouseDown(operator);
    const listboxes = screen.getAllByRole('listbox');
    const operatorList = listboxes[listboxes.length - 1];
    expect(within(operatorList).queryByText('Greater than')).not.toBeInTheDocument();
  });

  it('uses hierarchical jurisdiction labels and clears stale value when switching criteria', async () => {
    const Probe = () => {
      const form = Form.useFormInstance();
      return <button type="button" onClick={() => form.setFieldValue('criterionType', 'JURISDICTION_CODE')}>Switch criterion</button>;
    };

    render(
      <Form initialValues={{ policyId: 'SG_ANNUAL', criterionType: 'SERVICE_MONTHS', operator: 'EQUALS', value: 3 }}>
        <EligibilityRuleFormFields />
        <Probe />
      </Form>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Switch criterion' }));
    await waitFor(() => expect(screen.getByRole('combobox', { name: 'Operator' })).toHaveTextContent(''));

    const value = screen.getByRole('combobox', { name: 'Value' });
    fireEvent.mouseDown(value);
    expect(screen.getByText('Singapore > Central')).toBeInTheDocument();
  });
});
