import { Alert, Form, Input, InputNumber, Select, Space, Switch } from 'antd';
import { useEffect, useRef } from 'react';

import { blockInvalidNumericKey, generateEntitlementPolicyId } from './entitlementPolicyForm.ts';
import { JurisdictionLeaveTypeSelect } from './JurisdictionLeaveTypeSelect.tsx';
import { JurisdictionSelect } from './JurisdictionSelect.tsx';

interface Props {
  editing?: boolean;
  platformAdmin?: boolean;
}

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
  const accrualMethod = Form.useWatch('accrualMethod', form);
  const entitlementAmount = Form.useWatch('entitlementAmount', form);
  const previousJurisdictionId = useRef<string | undefined>(undefined);

  useEffect(() => {
    if (editing) return;
    if (!form.getFieldValue('entitlementUnit')) form.setFieldValue('entitlementUnit', 'DAYS');
    if (!form.getFieldValue('priority')) form.setFieldValue('priority', 10);
    if (!form.getFieldValue('accrualMethod')) form.setFieldValue('accrualMethod', 'NONE');
  }, [editing, form]);

  useEffect(() => {
    if (!editing || form.getFieldValue('accrualMethod') !== 'ANNUAL') return;
    form.setFieldValue('accrualMethod', 'NONE');
  }, [editing, form]);

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

  const calculatedMonthlyRate = accrualMethod === 'MONTHLY' ? monthlyAccrualRate(entitlementAmount) : undefined;

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
        description="An entitlement policy defines how much leave an eligible employee receives, how the entitlement becomes available, whether it is prorated, and how unused leave may be carried forward. Eligibility rules determine which employees the policy applies to."
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
        <Form.Item name="leaveTypeId" label="Leave type ID" rules={[{ required: true, message: 'Leave type ID is required' }]}>
          <Input />
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
            extra="The total leave entitlement for the policy period. For example, enter 14 with unit DAYS for 14 days of annual leave."
            rules={[{ required: true, message: 'Entitlement amount is required' }, { type: 'number', min: 0, message: 'Entitlement amount must be 0 or greater' }]}
          >
            <InputNumber min={0} step={0.5} inputMode="decimal" onKeyDown={(event) => blockInvalidNumericKey(event, false)} style={{ width: '100%' }} />
          </Form.Item>
        </div>
        <div style={{ flex: '0 0 118px' }}>
          <Form.Item
            name="entitlementUnit"
            label="Unit"
            extra="LeaveMaestro currently supports day-based entitlement only."
            rules={[{ required: true, message: 'Unit is required' }]}
          >
            <Select options={[{ label: 'Days', value: 'DAYS' }]} />
          </Form.Item>
        </div>
      </Space.Compact>

      <Form.Item
        name="accrualMethod"
        label="Accrual method"
        extra="Controls when entitlement becomes available. Front-loaded makes the entitlement available without progressive monthly earning. Monthly accrual makes entitlement available progressively each eligible month."
        rules={[{ required: true, message: 'Accrual method is required' }]}
      >
        <Select options={accrualMethods} />
      </Form.Item>

      <Form.Item name="accrualRate" hidden>
        <Input />
      </Form.Item>

      {accrualMethod === 'MONTHLY' ? (
        <Form.Item
          label="Calculated monthly accrual"
          extra="Calculated automatically as entitlement amount ÷ 12. It cannot be edited directly. Monthly accrual already uses eligible months, so proration should not reduce the same period a second time."
        >
          <Input value={formatMonthlyAccrual(calculatedMonthlyRate)} readOnly aria-label="Calculated monthly accrual" />
        </Form.Item>
      ) : (
        <Form.Item label="Calculated accrual" extra="Front-loaded policies do not use a periodic accrual rate.">
          <Input value="Not applicable" readOnly aria-label="Calculated accrual" />
        </Form.Item>
      )}

      <Form.Item
        name="prorationMethod"
        label="Proration method"
        extra="Proration is separate from accrual. It adjusts entitlement when an employee is eligible for only part of the policy period. NONE: full entitlement once eligible. CALENDAR_DAYS: prorate by eligible calendar days. MONTHS: prorate by eligible months. Monthly accrual already accounts for eligible months and is not reduced again by these proration settings."
        rules={[{ required: true, message: 'Proration method is required' }]}
      >
        <Select options={prorationMethods} />
      </Form.Item>

      <Form.Item name="carryForwardAllowed" label="Carry forward allowed" valuePropName="checked">
        <Switch />
      </Form.Item>

      <Form.Item
        name="carryForwardLimit"
        label="Carry forward limit"
        rules={[{ type: 'number', min: 0, message: 'Carry forward limit must be 0 or greater' }]}
      >
        <InputNumber min={0} step={0.5} inputMode="decimal" onKeyDown={(event) => blockInvalidNumericKey(event, false)} style={{ width: '100%' }} />
      </Form.Item>

      <Form.Item
        name="carryForwardExpiryMonths"
        label="Carry forward expiry (months)"
        rules={[{ type: 'integer', min: 0, message: 'Carry forward expiry must be a whole number of at least 0' }]}
      >
        <InputNumber min={0} step={1} precision={0} inputMode="numeric" onKeyDown={(event) => blockInvalidNumericKey(event, true)} style={{ width: '100%' }} />
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
