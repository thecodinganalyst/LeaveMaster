import { fireEvent, render, screen, within } from '@testing-library/react';
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
      return queryResult([{ id: 'ANNUAL', name: 'Annual', leaveTypeId: 'LT', entitlementAmount: 14, entitlementUnit: 'DAYS' }]);
    }
    if (queryKey[0] === 'leave-types') return queryResult([{ id: 'LT', name: 'Annual Leave' }]);
    return queryResult([]);
  });
});

describe('employment type eligibility rule editor', () => {
  it('offers employment type as a criterion with set operators only', () => {
    render(
      <Form initialValues={{ policyId: 'ANNUAL', criterionType: 'EMPLOYMENT_TYPE', operator: 'EQUALS' }}>
        <EligibilityRuleFormFields />
      </Form>,
    );

    expect(screen.getByText(/matches the staff member’s employment type/i)).toBeInTheDocument();
    const operator = screen.getByRole('combobox', { name: 'Operator' });
    fireEvent.mouseDown(operator);
    const listboxes = screen.getAllByRole('listbox');
    const operatorList = listboxes[listboxes.length - 1];
    expect(within(operatorList).getByText('Equals')).toBeInTheDocument();
    expect(within(operatorList).getByText('Does not equal')).toBeInTheDocument();
    expect(within(operatorList).getByText('Is one of')).toBeInTheDocument();
    expect(within(operatorList).getByText('Is not one of')).toBeInTheDocument();
    expect(within(operatorList).queryByText('Greater than')).not.toBeInTheDocument();
  });

  it('uses the shared human-readable employment type options for single values', () => {
    render(
      <Form initialValues={{ policyId: 'ANNUAL', criterionType: 'EMPLOYMENT_TYPE', operator: 'EQUALS', value: 'FULL_TIME' }}>
        <EligibilityRuleFormFields />
      </Form>,
    );

    expect(screen.getByRole('combobox', { name: 'Value' }).closest('.ant-select')).toHaveTextContent('Full Time');
    fireEvent.mouseDown(screen.getByRole('combobox', { name: 'Value' }));
    expect(screen.getByText('Part Time')).toBeInTheDocument();
    expect(screen.getByText('Casual')).toBeInTheDocument();
    expect(screen.getByText('Contract')).toBeInTheDocument();
    expect(screen.getByText('Intern')).toBeInTheDocument();
  });

  it('loads comma-separated IN values into a multi-select', () => {
    render(
      <Form initialValues={{ policyId: 'ANNUAL', criterionType: 'EMPLOYMENT_TYPE', operator: 'IN', value: 'FULL_TIME,PART_TIME' }}>
        <EligibilityRuleFormFields />
      </Form>,
    );

    const value = screen.getByRole('combobox', { name: 'Value' });
    const select = value.closest('.ant-select');
    expect(select).toHaveTextContent('Full Time');
    expect(select).toHaveTextContent('Part Time');
    expect(screen.getByText(/select the staff employment type value used by this rule/i)).toBeInTheDocument();
  });
});
