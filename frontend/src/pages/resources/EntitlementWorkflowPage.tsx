import { useCan } from '@refinedev/core';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, App, Button, Card, Form, Input, InputNumber, Select, Space, Spin, Switch, Typography } from 'antd';
import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { apiFetch } from '../../api/http.ts';
import { FormSection } from '../../components/common/FormSection.tsx';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import { blockInvalidNumericKey } from './entitlementPolicyForm.ts';
import {
  createEntitlementConfiguration,
  EntitlementPartialSaveError,
  type EligibilityRuleValues,
  updateEntitlementConfiguration,
} from './entitlementWorkflow.ts';
import { getAdminResourceConfig, normaliseFormValues, toFormValues } from './resourceConfigResolver.ts';

interface LeaveTypeRecord {
  id: string;
  name?: string | null;
}

interface PolicyRecord extends Record<string, unknown> {
  id: string;
  leaveTypeId?: string | null;
}

interface EligibilityRuleRecord extends EligibilityRuleValues {
  id: string;
  policyId: string;
}

interface WorkflowValues extends Record<string, unknown> {
  eligibilityRules?: EligibilityRuleValues[];
}

const policyModels = [
  { label: 'Annual entitlement', value: 'ANNUAL_ENTITLEMENT' },
  { label: 'Conditional annual entitlement', value: 'CONDITIONAL_ANNUAL_ENTITLEMENT' },
  { label: 'Event-based (no annual balance)', value: 'EVENT_BASED' },
];

const accrualMethods = [
  { label: 'Front-loaded', value: 'NONE' },
  { label: 'Monthly accrual', value: 'MONTHLY' },
];

const prorationMethods = [
  { label: 'None', value: 'NONE' },
  { label: 'Calendar days', value: 'CALENDAR_DAYS' },
  { label: 'Months', value: 'MONTHS' },
];

const criterionOptions = [
  { label: 'Jurisdiction', value: 'JURISDICTION_CODE' },
  { label: 'Length of service (months)', value: 'SERVICE_MONTHS' },
];

const operatorOptions = [
  { label: 'Equals', value: 'EQUALS' },
  { label: 'Does not equal', value: 'NOT_EQUALS' },
  { label: 'Is one of', value: 'IN' },
  { label: 'Is not one of', value: 'NOT_IN' },
  { label: 'Greater than', value: 'GREATER_THAN' },
  { label: 'Greater than or equal to', value: 'GREATER_THAN_OR_EQUAL' },
  { label: 'Less than', value: 'LESS_THAN' },
  { label: 'Less than or equal to', value: 'LESS_THAN_OR_EQUAL' },
];

const loadLeaveType = (leaveTypeId: string) => apiFetch<LeaveTypeRecord>(`/api/leave-types/${encodeURIComponent(leaveTypeId)}`);
const loadPolicy = (policyId: string) => apiFetch<PolicyRecord>(`/api/leave-entitlement-policies/${encodeURIComponent(policyId)}`);
const loadRules = () => apiFetch<EligibilityRuleRecord[]>('/api/leave-entitlement-policy-eligibility-rules');

const monthlyAccrualRate = (amount: unknown) => {
  const numericAmount = Number(amount);
  if (!Number.isFinite(numericAmount) || numericAmount < 0) return undefined;
  return Number((numericAmount / 12).toFixed(8));
};

const EligibilityRulesFields = () => (
  <Form.List name="eligibilityRules">
    {(fields, { add, remove }) => (
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        {fields.length === 0 ? (
          <Alert type="info" showIcon message="No eligibility rules" description="Without eligibility rules, this entitlement applies to all staff who otherwise match the policy." />
        ) : null}
        {fields.map((field, index) => (
          <Card
            key={field.key}
            size="small"
            title={`Rule ${index + 1}`}
            extra={<Button danger type="link" onClick={() => remove(field.name)}>Remove</Button>}
          >
            <Form.Item name={[field.name, 'id']} hidden><Input /></Form.Item>
            <Form.Item name={[field.name, 'criterionType']} label="Criterion" rules={[{ required: true, message: 'Criterion is required' }]}>
              <Select options={criterionOptions} placeholder="Select a criterion" />
            </Form.Item>
            <Form.Item name={[field.name, 'operator']} label="Operator" rules={[{ required: true, message: 'Operator is required' }]}>
              <Select options={operatorOptions} placeholder="Select an operator" />
            </Form.Item>
            <Form.Item
              name={[field.name, 'value']}
              label="Value"
              rules={[{ required: true, message: 'Value is required' }]}
              extra="For service-length rules, enter completed months. For multiple values, separate entries with commas."
            >
              <Input placeholder="e.g. 24 or SG" />
            </Form.Item>
            <Form.Item name={[field.name, 'active']} label="Active" valuePropName="checked"><Switch /></Form.Item>
            <Form.Item
              name={[field.name, 'sortOrder']}
              label="Sort order"
              rules={[{ required: true, message: 'Sort order is required' }, { type: 'integer', min: 0, message: 'Sort order must be 0 or greater' }]}
            >
              <InputNumber min={0} step={1} precision={0} style={{ width: '100%' }} />
            </Form.Item>
          </Card>
        ))}
        <Button
          type="dashed"
          block
          onClick={() => add({ active: true, sortOrder: (fields.length + 1) * 10 })}
        >
          Add eligibility rule
        </Button>
      </Space>
    )}
  </Form.List>
);

export const EntitlementWorkflowPage = () => {
  const { leaveTypeId = '', policyId } = useParams();
  const editing = Boolean(policyId);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { message } = App.useApp();
  const [form] = Form.useForm<WorkflowValues>();
  const policyModel = Form.useWatch('policyModel', form);
  const accrualMethod = Form.useWatch('accrualMethod', form);
  const entitlementAmount = Form.useWatch('entitlementAmount', form);
  const { data: canManagePolicy } = useCan({ resource: 'leave-entitlement-policies', action: editing ? 'edit' : 'create' });
  const { data: canManageRules } = useCan({ resource: 'leave-entitlement-policy-eligibility-rules', action: editing ? 'edit' : 'create' });

  const leaveTypeQuery = useQuery({
    queryKey: ['leave-types', 'entitlement-workflow', leaveTypeId],
    queryFn: () => loadLeaveType(leaveTypeId),
    enabled: Boolean(leaveTypeId),
  });
  const policyQuery = useQuery({
    queryKey: ['leave-entitlement-policies', 'entitlement-workflow', policyId],
    queryFn: () => loadPolicy(String(policyId)),
    enabled: editing,
  });
  const rulesQuery = useQuery({
    queryKey: ['leave-entitlement-policy-eligibility-rules', 'entitlement-workflow', policyId],
    queryFn: loadRules,
    enabled: editing,
  });

  const policyConfig = getAdminResourceConfig('leave-entitlement-policies');
  const originalRules = (rulesQuery.data ?? []).filter((rule) => rule.policyId === policyId);

  useEffect(() => {
    if (!editing) {
      form.setFieldsValue({
        leaveTypeId,
        active: true,
        policyModel: 'ANNUAL_ENTITLEMENT',
        priority: 10,
        entitlementUnit: 'DAYS',
        accrualMethod: 'NONE',
        prorationMethod: 'NONE',
        carryForwardAllowed: false,
        eligibilityRules: [],
      });
      return;
    }
    if (!policyConfig || !policyQuery.data || rulesQuery.isLoading) return;
    form.setFieldsValue({
      ...toFormValues(policyConfig, policyQuery.data),
      leaveTypeId,
      eligibilityRules: originalRules.map((rule) => ({ ...rule })),
    });
  }, [editing, form, leaveTypeId, originalRules, policyConfig, policyQuery.data, rulesQuery.isLoading]);

  const eventBased = policyModel === 'EVENT_BASED';
  const calculatedRate = !eventBased && accrualMethod === 'MONTHLY' ? monthlyAccrualRate(entitlementAmount) : undefined;

  useEffect(() => {
    form.setFieldValue('accrualRate', calculatedRate);
  }, [calculatedRate, form]);

  useEffect(() => {
    if (!eventBased) return;
    form.setFieldsValue({
      entitlementUnit: 'DAYS',
      accrualMethod: 'NONE',
      accrualRate: undefined,
      prorationMethod: 'NONE',
      carryForwardAllowed: false,
      carryForwardLimit: undefined,
      carryForwardExpiryMonths: undefined,
    });
  }, [eventBased, form]);

  if (!leaveTypeId || !policyConfig) return null;
  if (leaveTypeQuery.isLoading || (editing && (policyQuery.isLoading || rulesQuery.isLoading))) return <Spin />;
  if (editing && policyQuery.data?.leaveTypeId && policyQuery.data.leaveTypeId !== leaveTypeId) {
    return <PageContainer><Alert type="error" showIcon message="This entitlement does not belong to the selected leave type." /></PageContainer>;
  }

  const canSave = Boolean(canManagePolicy?.can && canManageRules?.can);
  const backPath = `/leave-types/show/${encodeURIComponent(leaveTypeId)}`;

  const submit = async (values: WorkflowValues) => {
    const { eligibilityRules = [], ...rawPolicyValues } = values;
    const policyValues = normaliseFormValues(policyConfig, rawPolicyValues);
    delete policyValues.leaveTypeId;
    try {
      if (editing && policyId) {
        await updateEntitlementConfiguration(apiFetch, policyId, leaveTypeId, policyValues, originalRules, eligibilityRules);
        message.success('Entitlement updated');
      } else {
        await createEntitlementConfiguration(apiFetch, leaveTypeId, policyValues, eligibilityRules);
        message.success('Entitlement created');
      }
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['leave-entitlement-policies'] }),
        queryClient.invalidateQueries({ queryKey: ['leave-entitlement-policy-eligibility-rules'] }),
      ]);
      navigate(backPath);
    } catch (error) {
      const text = error instanceof EntitlementPartialSaveError || error instanceof Error
        ? error.message
        : 'Unable to save entitlement';
      message.error(text);
    }
  };

  return (
    <PageContainer>
      <PageHeader
        title={editing ? 'Edit entitlement' : 'Add entitlement'}
        subtitle={leaveTypeQuery.data?.name ? `Leave type: ${leaveTypeQuery.data.name}` : 'Configure entitlement and eligibility together.'}
      />
      {!canSave ? (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message="You do not have permission to manage both entitlement policies and eligibility rules."
        />
      ) : null}
      <Form form={form} layout="vertical" onFinish={submit}>
        <Form.Item name="leaveTypeId" hidden><Input /></Form.Item>
        <FormSection title="Entitlement">
          <Typography.Paragraph type="secondary">
            The leave type is fixed from the page you came from. Configure the entitlement and its availability below.
          </Typography.Paragraph>
          <Form.Item name="name" label="Name" rules={[{ required: true, message: 'Name is required' }]}><Input /></Form.Item>
          <Form.Item name="active" label="Active" valuePropName="checked"><Switch /></Form.Item>
          <Form.Item name="policyModel" label="Policy model" rules={[{ required: true, message: 'Policy model is required' }]}>
            <Select options={policyModels} />
          </Form.Item>
          {eventBased ? (
            <>
              <Form.Item name="qualifyingEventTypeCode" label="Qualifying event type" rules={[{ required: true, message: 'Qualifying event type is required' }]}>
                <Input placeholder="e.g. MILITARY_CALL_UP" />
              </Form.Item>
              <Form.Item name="eventRequiresVerification" label="Requires verification before use" valuePropName="checked"><Switch /></Form.Item>
              <Space.Compact block>
                <Form.Item name="eventValidityDaysBefore" label="Validity days before" style={{ flex: 1 }}>
                  <InputNumber min={0} step={1} precision={0} style={{ width: '100%' }} />
                </Form.Item>
                <Form.Item name="eventValidityDaysAfter" label="Validity days after" style={{ flex: 1 }}>
                  <InputNumber min={0} step={1} precision={0} style={{ width: '100%' }} />
                </Form.Item>
              </Space.Compact>
            </>
          ) : null}
          <Form.Item name="priority" label="Priority (higher number wins)" rules={[{ required: true }, { type: 'integer', min: 0 }]}>
            <InputNumber min={0} step={1} precision={0} onKeyDown={(event) => blockInvalidNumericKey(event, true)} style={{ width: '100%' }} />
          </Form.Item>
          <Space.Compact block>
            <Form.Item name="entitlementAmount" label={eventBased ? 'Entitlement per event' : 'Entitlement amount'} style={{ flex: 1 }} rules={[{ required: true, message: 'Entitlement amount is required' }]}>
              <InputNumber min={eventBased ? 0.5 : 0} step={0.5} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="entitlementUnit" label="Unit" style={{ width: 130 }} rules={[{ required: true }]}>
              <Select options={[{ label: 'Days', value: 'DAYS' }]} />
            </Form.Item>
          </Space.Compact>
          <Form.Item name="accrualMethod" label="Accrual method" rules={[{ required: true }]}>
            <Select disabled={eventBased} options={accrualMethods} />
          </Form.Item>
          <Form.Item name="accrualRate" hidden><Input /></Form.Item>
          {accrualMethod === 'MONTHLY' && calculatedRate !== undefined ? (
            <Form.Item label="Calculated monthly accrual"><Input readOnly value={`${calculatedRate} days per month`} /></Form.Item>
          ) : null}
          <Form.Item name="prorationMethod" label="Proration method" rules={[{ required: true }]}>
            <Select disabled={eventBased} options={prorationMethods} />
          </Form.Item>
          <Form.Item name="carryForwardAllowed" label="Carry forward allowed" valuePropName="checked"><Switch disabled={eventBased} /></Form.Item>
          <Form.Item name="carryForwardLimit" label="Carry forward limit"><InputNumber disabled={eventBased} min={0} step={0.5} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="carryForwardExpiryMonths" label="Carry forward expiry (months)"><InputNumber disabled={eventBased} min={0} step={1} precision={0} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="effectiveFrom" label="Effective from" rules={[{ required: true, message: 'Effective from is required' }]}><Input type="date" allowClear /></Form.Item>
          <Form.Item name="effectiveTo" label="Effective to"><Input type="date" allowClear /></Form.Item>
        </FormSection>

        <FormSection title="Eligibility">
          <EligibilityRulesFields />
        </FormSection>

        <Space>
          <Button type="primary" htmlType="submit" disabled={!canSave}>Save</Button>
          <Button htmlType="button" onClick={() => navigate(backPath)}>Cancel</Button>
        </Space>
      </Form>
    </PageContainer>
  );
};
