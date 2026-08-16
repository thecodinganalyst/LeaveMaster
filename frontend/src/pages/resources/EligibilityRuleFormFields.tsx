import { useQuery } from '@tanstack/react-query';
import { Form, InputNumber, Select, Switch } from 'antd';
import { useEffect, useMemo, useRef } from 'react';
import type { KeyboardEvent } from 'react';

import { apiFetch } from '../../api/http.ts';
import { blockInvalidNumericKey } from './entitlementPolicyForm.ts';
import { getJurisdictionOptions, type JurisdictionOptionSource } from './jurisdictions.ts';

type CriterionType = 'LOCATION_ID' | 'JURISDICTION_CODE' | 'SERVICE_MONTHS';
type EligibilityOperator = 'EQUALS' | 'NOT_EQUALS' | 'IN' | 'NOT_IN' | 'GREATER_THAN' | 'GREATER_THAN_OR_EQUAL' | 'LESS_THAN' | 'LESS_THAN_OR_EQUAL';

interface PolicyOptionSource {
  id: string;
  name?: string | null;
  scope?: 'PLATFORM_TEMPLATE' | 'TENANT' | string | null;
  tenantId?: string | null;
}

interface LocationOptionSource {
  id: string;
  locationName?: string | null;
}

interface Props {
  editing?: boolean;
}

const criterionOptions = [
  { label: 'Employee location', value: 'LOCATION_ID' },
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

const loadPolicies = () => apiFetch<PolicyOptionSource[]>('/api/leave-entitlement-policies');
const loadLocations = () => apiFetch<LocationOptionSource[]>('/api/locations');
const loadJurisdictions = () => apiFetch<JurisdictionOptionSource[]>('/api/jurisdictions');

export const EligibilityRuleFormFields = ({ editing = false }: Props) => {
  const form = Form.useFormInstance();
  const policyId = Form.useWatch('policyId', form);
  const criterionType = Form.useWatch('criterionType', form) as CriterionType | undefined;
  const operator = Form.useWatch('operator', form) as EligibilityOperator | undefined;
  const previousCriterion = useRef<CriterionType | undefined>(criterionType);
  const previousOperator = useRef<EligibilityOperator | undefined>(operator);

  const policiesQuery = useQuery({ queryKey: ['leave-entitlement-policies', 'options'], queryFn: loadPolicies, staleTime: 5 * 60 * 1000 });
  const locationsQuery = useQuery({ queryKey: ['locations', 'eligibility-options'], queryFn: loadLocations, staleTime: 5 * 60 * 1000 });
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
    label: policy.name ? `${policy.name} (${policy.id})` : policy.id,
    value: policy.id,
  })), [policiesQuery.data]);

  const selectedPolicy = (policiesQuery.data ?? []).find((policy) => policy.id === policyId);
  const platformTemplate = Boolean(selectedPolicy && (
    selectedPolicy.scope === 'PLATFORM_TEMPLATE'
    || (!selectedPolicy.tenantId && selectedPolicy.scope !== 'TENANT')
  ));

  useEffect(() => {
    if (platformTemplate && criterionType === 'LOCATION_ID') {
      form.setFieldsValue({ criterionType: undefined, operator: undefined, value: undefined });
    }
  }, [criterionType, form, platformTemplate]);

  const operatorOptions = criterionType === 'SERVICE_MONTHS' ? numericOperators : setOperators;
  const isMulti = operator ? multiValueOperators.has(operator) : false;

  const locationOptions = useMemo(() => (locationsQuery.data ?? []).map((location) => ({
    label: location.locationName ? `${location.locationName} (${location.id})` : location.id,
    value: location.id,
  })).sort((left, right) => left.label.localeCompare(right.label)), [locationsQuery.data]);

  const jurisdictionOptions = useMemo(() => getJurisdictionOptions(jurisdictionsQuery.data ?? []), [jurisdictionsQuery.data]);

  const valueField = (() => {
    if (criterionType === 'SERVICE_MONTHS') {
      if (isMulti) {
        return (
          <Select
            mode="tags"
            tokenSeparators={[',']}
            placeholder="Enter one or more whole numbers"
            onInputKeyDown={blockInvalidTagKey}
            options={[]}
          />
        );
      }
      return (
        <InputNumber
          min={0}
          step={1}
          precision={0}
          inputMode="numeric"
          onKeyDown={(event) => blockInvalidNumericKey(event, true)}
          style={{ width: '100%' }}
          placeholder="Enter completed months of service"
        />
      );
    }

    if (criterionType === 'LOCATION_ID') {
      return (
        <Select
          {...(isMulti ? { mode: 'multiple' as const } : {})}
          loading={locationsQuery.isLoading}
          disabled={locationsQuery.isError}
          options={locationOptions}
          showSearch
          optionFilterProp="label"
          placeholder={locationsQuery.isError ? 'Unable to load locations' : isMulti ? 'Select locations' : 'Select a location'}
        />
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
    : criterionType === 'LOCATION_ID'
      ? 'Select the employee location value used by this rule.'
      : criterionType === 'JURISDICTION_CODE'
        ? 'Select the jurisdiction value used by this rule.'
        : 'Choose a criterion and operator before entering a value.';

  return (
    <>
      <Form.Item
        name="policyId"
        label="Policy"
        extra="Select the entitlement policy this eligibility rule belongs to."
        rules={[{ required: true, message: 'Policy is required' }]}
      >
        <Select
          disabled={editing || policiesQuery.isError}
          loading={policiesQuery.isLoading}
          options={policyOptions}
          showSearch
          optionFilterProp="label"
          placeholder={policiesQuery.isError ? 'Unable to load policies' : 'Select an entitlement policy'}
        />
      </Form.Item>

      <Form.Item
        name="criterionType"
        label="Criterion"
        extra={criterionType === 'LOCATION_ID'
          ? 'Matches the employee’s assigned work location.'
          : criterionType === 'JURISDICTION_CODE'
            ? 'Matches the jurisdiction derived from the employee’s work location.'
            : criterionType === 'SERVICE_MONTHS'
              ? 'Matches completed months of service as of the policy evaluation date.'
              : 'Choose the employee attribute this rule evaluates.'}
        rules={[{ required: true, message: 'Criterion is required' }]}
      >
        <Select
          options={criterionOptions.map((option) => option.value === 'LOCATION_ID' && platformTemplate ? { ...option, disabled: true } : option)}
          placeholder="Select a criterion"
        />
      </Form.Item>

      <Form.Item
        name="operator"
        label="Operator"
        extra="Available operators depend on the selected criterion."
        rules={[{ required: true, message: 'Operator is required' }]}
      >
        <Select options={operatorOptions} disabled={!criterionType} placeholder="Select an operator" />
      </Form.Item>

      <Form.Item
        name="value"
        label="Value"
        extra={valueExtra}
        {...(isMulti ? {
          getValueProps: (value: unknown) => ({ value: splitValues(value) }),
          normalize: joinValues,
        } : {})}
        rules={[{ required: true, message: 'Value is required' }]}
      >
        {valueField}
      </Form.Item>

      <Form.Item name="active" label="Active" valuePropName="checked">
        <Switch />
      </Form.Item>

      <Form.Item
        name="sortOrder"
        label="Sort order"
        extra="Controls the order in which eligibility rules are displayed and evaluated. It does not change the result while all rules use AND logic. Use 10, 20, 30... to leave room for inserting rules later."
        rules={[{ required: true, message: 'Sort order is required' }, { type: 'integer', min: 0, message: 'Sort order must be a whole number of at least 0' }]}
      >
        <InputNumber
          min={0}
          step={1}
          precision={0}
          inputMode="numeric"
          onKeyDown={(event) => blockInvalidNumericKey(event, true)}
          style={{ width: '100%' }}
        />
      </Form.Item>
    </>
  );
};
