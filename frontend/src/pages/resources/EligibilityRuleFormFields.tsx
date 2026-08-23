import { useQuery } from '@tanstack/react-query';
import { Form, InputNumber, Select, Switch } from 'antd';
import { useEffect, useMemo, useRef } from 'react';
import type { KeyboardEvent } from 'react';

import { apiFetch } from '../../api/http.ts';
import { blockInvalidNumericKey } from './entitlementPolicyForm.ts';
import {
  formatEntitlementPolicyLabel,
  type EntitlementPolicyOptionSource,
  type LeaveTypeOptionSource,
} from './entitlementPolicyPresentation.ts';
import { getJurisdictionOptions, type JurisdictionOptionSource } from './jurisdictions.ts';

type CriterionType = 'JURISDICTION_CODE' | 'SERVICE_MONTHS';
type EligibilityOperator = 'EQUALS' | 'NOT_EQUALS' | 'IN' | 'NOT_IN' | 'GREATER_THAN' | 'GREATER_THAN_OR_EQUAL' | 'LESS_THAN' | 'LESS_THAN_OR_EQUAL';

interface Props {
  editing?: boolean;
}

const criterionOptions = [
  { label: 'Jurisdiction', value: 'JURISDICTION_CODE' },
  { label: 'Length of service (months)', value: 'SERVICE_MONTHS' },
];

const setOperators = [
  { label: 'Equals', value: 'EQUALS' },
  { label: 'Does not equal', value: 'NOT_EQUALS' },
  { label: 'Is one of', value: 'IN' },
  { label: 'Is not one of', value: 'NOT_IN' },
];

const numericOperators = [
  ...setOperators,
  { label: 'Greater than', value: 'GREATER_THAN' },
  { label: 'Greater than or equal to', value: 'GREATER_THAN_OR_EQUAL' },
  { label: 'Less than', value: 'LESS_THAN' },
  { label: 'Less than or equal to', value: 'LESS_THAN_OR_EQUAL' },
];

const multiValueOperators = new Set<EligibilityOperator>(['IN', 'NOT_IN']);

const splitValues = (value: unknown) => {
  if (Array.isArray(value)) return value.map(String);
  if (value === undefined || value === null || value === '') return [];
  return String(value).split(',').map((item) => item.trim()).filter(Boolean);
};

const joinValues = (value: unknown) => Array.isArray(value) ? value.map(String).join(',') : value;

const blockInvalidTagKey = (event: KeyboardEvent<HTMLInputElement | HTMLTextAreaElement>) => {
  if (event.ctrlKey || event.metaKey || event.altKey) return;
  if (event.key.length !== 1) return;
  if (/^[0-9]$/.test(event.key) || event.key === ',') return;
  event.preventDefault();
};

const loadPolicies = () => apiFetch<EntitlementPolicyOptionSource[]>('/api/leave-entitlement-policies');
const loadLeaveTypes = () => apiFetch<LeaveTypeOptionSource[]>('/api/leave-types');
const loadJurisdictions = () => apiFetch<JurisdictionOptionSource[]>('/api/jurisdictions');

export const EligibilityRuleFormFields = ({ editing = false }: Props) => {
  const form = Form.useFormInstance();
  const criterionType = Form.useWatch('criterionType', form) as CriterionType | undefined;
  const operator = Form.useWatch('operator', form) as EligibilityOperator | undefined;
  const previousCriterion = useRef<CriterionType | undefined>(criterionType);
  const previousOperator = useRef<EligibilityOperator | undefined>(operator);

  const policiesQuery = useQuery({ queryKey: ['leave-entitlement-policies', 'options'], queryFn: loadPolicies, staleTime: 5 * 60 * 1000 });
  const leaveTypesQuery = useQuery({ queryKey: ['leave-types', 'tenant-options'], queryFn: loadLeaveTypes, staleTime: 5 * 60 * 1000 });
  const jurisdictionsQuery = useQuery({ queryKey: ['jurisdictions', 'eligibility-options'], queryFn: loadJurisdictions, staleTime: 5 * 60 * 1000 });

  useEffect(() => {
    if (editing) return;
    if (form.getFieldValue('sortOrder') === undefined) form.setFieldValue('sortOrder', 10);
    if (form.getFieldValue('active') === undefined) form.setFieldValue('active', true);
  }, [editing, form]);

  useEffect(() => {
    if (previousCriterion.current !== undefined && previousCriterion.current !== criterionType) {
      form.setFieldsValue({ operator: undefined, value: undefined });
    }
    previousCriterion.current = criterionType;
  }, [criterionType, form]);

  useEffect(() => {
    if (previousOperator.current !== undefined && previousOperator.current !== operator) {
      const wasMulti = multiValueOperators.has(previousOperator.current);
      const isMulti = operator ? multiValueOperators.has(operator) : false;
      if (wasMulti !== isMulti) form.setFieldValue('value', undefined);
    }
    previousOperator.current = operator;
  }, [form, operator]);

  const policyOptions = useMemo(() => (policiesQuery.data ?? []).map((policy) => ({
    label: formatEntitlementPolicyLabel(policy, leaveTypesQuery.data ?? []),
    value: policy.id,
  })), [leaveTypesQuery.data, policiesQuery.data]);

  const operatorOptions = criterionType === 'SERVICE_MONTHS' ? numericOperators : setOperators;
  const isMulti = operator ? multiValueOperators.has(operator) : false;

  const jurisdictionOptions = useMemo(() => getJurisdictionOptions(jurisdictionsQuery.data ?? []), [jurisdictionsQuery.data]);

  const valueField = (() => {
    if (criterionType === 'SERVICE_MONTHS') {
      if (isMulti) {
        return (
          <Select mode="tags" tokenSeparators={[',']} placeholder="Enter one or more whole numbers" onInputKeyDown={blockInvalidTagKey} options={[]} />
        );
      }
      return (
        <InputNumber min={0} step={1} precision={0} inputMode="numeric" onKeyDown={(event) => blockInvalidNumericKey(event, true)} style={{ width: '100%' }} placeholder="Enter completed months of service" />
      );
    }

    if (criterionType === 'JURISDICTION_CODE') {
      return (
        <Select
          {...(isMulti ? { mode: 'multiple' as const } : {})}
          loading={jurisdictionsQuery.isLoading}
          disabled={jurisdictionsQuery.isError}
          options={jurisdictionOptions}
          showSearch
          optionFilterProp="label"
          placeholder={jurisdictionsQuery.isError ? 'Unable to load jurisdictions' : isMulti ? 'Select jurisdictions' : 'Select a jurisdiction'}
        />
      );
    }

    return <Select disabled placeholder="Select a criterion first" />;
  })();

  const valueExtra = criterionType === 'SERVICE_MONTHS'
    ? isMulti ? 'Enter one or more non-negative whole numbers.' : 'Enter a non-negative whole number of completed service months.'
    : criterionType === 'JURISDICTION_CODE'
      ? 'Select the jurisdiction value used by this rule.'
      : 'Choose a criterion and operator before entering a value.';

  return (
    <>
      <Form.Item
        name="policyId"
        label="Entitlement policy"
        extra="Select the entitlement policy by name, leave type, entitlement amount and unit."
        rules={[{ required: true, message: 'Policy is required' }]}
      >
        <Select
          disabled={editing || policiesQuery.isError || leaveTypesQuery.isError}
          loading={policiesQuery.isLoading || leaveTypesQuery.isLoading}
          options={policyOptions}
          showSearch
          optionFilterProp="label"
          placeholder={policiesQuery.isError || leaveTypesQuery.isError ? 'Unable to load policies' : 'Select an entitlement policy'}
        />
      </Form.Item>

      <Form.Item
        name="criterionType"
        label="Criterion"
        extra={criterionType === 'JURISDICTION_CODE'
          ? 'Matches the employee’s assigned jurisdiction or an active parent jurisdiction.'
          : criterionType === 'SERVICE_MONTHS'
            ? 'Matches completed months of service as of the policy evaluation date.'
            : 'Choose the employee attribute this rule evaluates.'}
        rules={[{ required: true, message: 'Criterion is required' }]}
      >
        <Select options={criterionOptions} placeholder="Select a criterion" />
      </Form.Item>

      <Form.Item name="operator" label="Operator" extra="Available operators depend on the selected criterion." rules={[{ required: true, message: 'Operator is required' }]}>
        <Select options={operatorOptions} disabled={!criterionType} placeholder="Select an operator" />
      </Form.Item>

      <Form.Item
        name="value"
        label="Value"
        extra={valueExtra}
        {...(isMulti ? { getValueProps: (value: unknown) => ({ value: splitValues(value) }), normalize: joinValues } : {})}
        rules={[{ required: true, message: 'Value is required' }]}
      >
        {valueField}
      </Form.Item>

      <Form.Item name="active" label="Active" valuePropName="checked"><Switch /></Form.Item>

      <Form.Item
        name="sortOrder"
        label="Sort order"
        extra="Controls the order in which eligibility rules are displayed and evaluated. It does not change the result while all rules use AND logic. Use 10, 20, 30... to leave room for inserting rules later."
        rules={[{ required: true, message: 'Sort order is required' }, { type: 'integer', min: 0, message: 'Sort order must be a whole number of at least 0' }]}
      >
        <InputNumber min={0} step={1} precision={0} inputMode="numeric" onKeyDown={(event) => blockInvalidNumericKey(event, true)} style={{ width: '100%' }} />
      </Form.Item>
    </>
  );
};
