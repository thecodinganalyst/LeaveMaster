import { Alert, Form, Input, InputNumber, Select, Space, Switch } from 'antd';
import { useEffect, useRef } from 'react';

import { blockInvalidNumericKey, generateEntitlementPolicyId } from './entitlementPolicyForm.ts';
import { JurisdictionLeaveTypeSelect } from './JurisdictionLeaveTypeSelect.tsx';
import { JurisdictionSelect } from './JurisdictionSelect.tsx';
import { TenantLeaveTypeSelect } from './TenantLeaveTypeSelect.tsx';

interface Props {
  editing?: boolean;
  platformAdmin?: boolean;
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

const monthlyAccrualRate = (amount: unknown) => {
  const numericAmount = Number(amount);
  if (!Number.isFinite(numericAmount) || numericAmount < 0) return undefined;
  return Number((numericAmount / 12).toFixed(8));
};

const formatMonthlyAccrual = (rate: number | undefined) => {
  if (rate === undefined) return '';
  return `${rate.toFixed(4).replace(/0+$/, '').replace(/\.$/, '')} days per month`;
};

export const EntitlementPolicyFormFields = ({ editing = false, platformAdmin = false }: Props) => {
  const form = Form.useFormInstance();
  const jurisdictionId = Form.useWatch('jurisdictionId', form);
  const leaveTypeId = Form.useWatch('leaveTypeId', form);
  const name = Form.useWatch('name', form);
  const policyModel = Form.useWatch('policyModel', form);
  const accrualMethod = Form.useWatch('accrualMethod', form);
  const entitlementAmount = Form.useWatch('entitlementAmount', form);
  const previousJurisdictionId = useRef<string | undefined>(undefined);
  const eventBased = policyModel === 'EVENT_BASED';

  useEffect(() => {
    if (editing) return;
    if (!form.getFieldValue('policyModel')) form.setFieldValue('policyModel', 'ANNUAL_ENTITLEMENT');
    if (!form.getFieldValue('entitlementUnit')) form.setFieldValue('entitlementUnit', 'DAYS');
    if (!form.getFieldValue('priority')) form.setFieldValue('priority', 10);
    if (!form.getFieldValue('accrualMethod')) form.setFieldValue('accrualMethod', 'NONE');
    if (form.getFieldValue('carryForwardAllowed') == null) form.setFieldValue('carryForwardAllowed', false);
  }, [editing, form]);

  useEffect(() => {
    if (!editing || form.getFieldValue('accrualMethod') !== 'ANNUAL') return;
    form.setFieldValue('accrualMethod', 'NONE');
  }, [editing, form]);

  useEffect(() => {
    if (!eventBased) return;
    form.setFieldsValue({
      entitlementAmount: 0,
      entitlementUnit: 'DAYS',
      accrualMethod: 'NONE',
      accrualRate: undefined,
      prorationMethod: 'NONE',
      carryForwardAllowed: false,
      carryForwardLimit: undefined,
      carryForwardExpiryMonths: undefined,
    });
  }, [eventBased, form]);

  useEffect(() => {
    if (!platformAdmin || editing) return;

    const currentJurisdictionId = jurisdictionId ? String(jurisdictionId) : '';
    if (previousJurisdictionId.current !== undefined && previousJurisdictionId.current !== currentJurisdictionId) {
      form.setFieldValue('jurisdictionLeaveTypeId', undefined);
    }
    previousJurisdictionId.current = currentJurisdictionId;
  }, [editing, form, jurisdictionId, platformAdmin]);

  useEffect(() => {
    if (editing) return;
    const prefix = platformAdmin ? String(jurisdictionId ?? '') : String(leaveTypeId ?? '');
    const generated = generateEntitlementPolicyId(prefix, String(name ?? ''));
    form.setFieldValue('id', generated || undefined);
  }, [editing, form, jurisdictionId, leaveTypeId, name, platformAdmin]);

  const calculatedMonthlyRate = !eventBased && accrualMethod === 'MONTHLY' ? monthlyAccrualRate(entitlementAmount) : undefined;

  useEffect(() => {
    form.setFieldValue('accrualRate', calculatedMonthlyRate);
  }, [calculatedMonthlyRate, form]);

  return (
    <>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 20 }}
        message="What an entitlement policy controls"
        description="An entitlement policy defines how leave is made available. Annual policies create a leave-year balance, conditional annual policies do so only when eligibility rules match, and event-based policies are resolved against a qualifying event instead of an annual balance."
      />

      {platformAdmin ? (
        <>
          <Form.Item name="jurisdictionId" label="Jurisdiction" rules={[{ required: true, message: 'Jurisdiction is required' }]}>
            <JurisdictionSelect disabled={editing} />
          </Form.Item>
          <Form.Item name="jurisdictionLeaveTypeId" label="Jurisdiction leave type" rules={[{ required: true, message: 'Jurisdiction leave type is required' }]}>
            <JurisdictionLeaveTypeSelect
              {...(jurisdictionId ? { jurisdictionId: String(jurisdictionId) } : {})}
              disabled={editing}
            />
          </Form.Item>
        </>
      ) : (
        <Form.Item name="leaveTypeId" label="Leave type" rules={[{ required: true, message: 'Leave type is required' }]}>
          <TenantLeaveTypeSelect />
        </Form.Item>
      )}

      <Form.Item name="name" label="Name" rules={[{ required: true, message: 'Name is required' }]}>
        <Input />
      </Form.Item>

      <Form.Item name="id" hidden>
        <Input />
      </Form.Item>

      <Form.Item name="active" label="Active" valuePropName="checked">
        <Switch />
      </Form.Item>

      <Form.Item
        name="policyModel"
        label="Policy model"
        extra="Choose how entitlement is represented. Event-based leave does not create a conventional annual balance."
        rules={[{ required: true, message: 'Policy model is required' }]}
      >
        <Select options={policyModels} />
      </Form.Item>

      {eventBased ? (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 20 }}
          message="Event-based leave has no annual balance"
          description="This policy records the leave model only. Its duration and eligibility must be supplied by a qualifying-event workflow; annual accrual, proration, and carry-forward do not apply."
        />
      ) : null}

      <Form.Item
        name="priority"
        label="Priority (higher number wins)"
        extra="Determines which policy wins when more than one policy matches. Start with 10 for the default policy and use 20 or 30 for more specific rules. Avoid equal highest priorities for overlapping policies."
        rules={[{ required: true, message: 'Priority is required' }, { type: 'integer', min: 0, message: 'Priority must be a whole number of at least 0' }]}
      >
        <InputNumber min={0} step={1} precision={0} inputMode="numeric" onKeyDown={(event) => blockInvalidNumericKey(event, true)} style={{ width: '100%' }} />
      </Form.Item>

      <Space.Compact block style={{ alignItems: 'flex-start', marginBottom: 0 }}>
        <div style={{ flex: '1 1 auto', minWidth: 0 }}>
          <Form.Item
            name="entitlementAmount"
            label="Entitlement amount"
            extra={eventBased ? 'Not used for event-based policies.' : 'The total leave entitlement for the policy period. For example, enter 14 with unit DAYS for 14 days of annual leave.'}
            rules={[{ required: true, message: 'Entitlement amount is required' }, { type: 'number', min: 0, message: 'Entitlement amount must be 0 or greater' }]}
          >
            <InputNumber disabled={eventBased} min={0} step={0.5} inputMode="decimal" onKeyDown={(event) => blockInvalidNumericKey(event, false)} style={{ width: '100%' }} />
          </Form.Item>
        </div>
        <div style={{ flex: '0 0 118px' }}>
          <Form.Item
            name="entitlementUnit"
            label="Unit"
            extra="LeaveMaestro currently supports day-based entitlement only."
            rules={[{ required: true, message: 'Unit is required' }]}
          >
            <Select disabled={eventBased} options={[{ label: 'Days', value: 'DAYS' }]} />
          </Form.Item>
        </div>
      </Space.Compact>

      <Form.Item
        name="accrualMethod"
        label="Accrual method"
        extra="Controls when annual entitlement becomes available. Event-based policies do not use recurring accrual."
        rules={[{ required: true, message: 'Accrual method is required' }]}
      >
        <Select disabled={eventBased} options={accrualMethods} />
      </Form.Item>

      <Form.Item name="accrualRate" hidden>
        <Input />
      </Form.Item>

      {!eventBased && accrualMethod === 'MONTHLY' ? (
        <Form.Item
          label="Calculated monthly accrual"
          extra="Calculated automatically as entitlement amount ÷ 12. It cannot be edited directly. Monthly accrual already uses eligible months, so proration should not reduce the same period a second time."
        >
          <Input value={formatMonthlyAccrual(calculatedMonthlyRate)} readOnly aria-label="Calculated monthly accrual" />
        </Form.Item>
      ) : (
        <Form.Item label="Calculated accrual" extra={eventBased ? 'Event-based policies do not accrue an annual balance.' : 'Front-loaded policies do not use a periodic accrual rate.'}>
          <Input value="Not applicable" readOnly aria-label="Calculated accrual" />
        </Form.Item>
      )}

      <Form.Item
        name="prorationMethod"
        label="Proration method"
        extra="Proration adjusts annual entitlement when an employee is eligible for only part of the policy period. It does not apply to event-based policies."
        rules={[{ required: true, message: 'Proration method is required' }]}
      >
        <Select disabled={eventBased} options={prorationMethods} />
      </Form.Item>

      <Form.Item name="carryForwardAllowed" label="Carry forward allowed" valuePropName="checked">
        <Switch disabled={eventBased} />
      </Form.Item>

      <Form.Item
        name="carryForwardLimit"
        label="Carry forward limit"
        rules={[{ type: 'number', min: 0, message: 'Carry forward limit must be 0 or greater' }]}
      >
        <InputNumber disabled={eventBased} min={0} step={0.5} inputMode="decimal" onKeyDown={(event) => blockInvalidNumericKey(event, false)} style={{ width: '100%' }} />
      </Form.Item>

      <Form.Item
        name="carryForwardExpiryMonths"
        label="Carry forward expiry (months)"
        rules={[{ type: 'integer', min: 0, message: 'Carry forward expiry must be a whole number of at least 0' }]}
      >
        <InputNumber disabled={eventBased} min={0} step={1} precision={0} inputMode="numeric" onKeyDown={(event) => blockInvalidNumericKey(event, true)} style={{ width: '100%' }} />
      </Form.Item>

      <Form.Item name="effectiveFrom" label="Effective from" rules={[{ required: true, message: 'Effective from is required' }]}>
        <Input type="date" />
      </Form.Item>
      <Form.Item name="effectiveTo" label="Effective to">
        <Input type="date" />
      </Form.Item>
    </>
  );
};
