import { Form, Input, InputNumber, Select, Switch } from 'antd';
import type { Rule } from 'antd/es/form';

import type { AdminResourceConfig } from './adminResourceConfig.ts';
import { isAdminFieldVisible } from './adminResourceConfig.ts';
import { EligibilityRuleFormFields } from './EligibilityRuleFormFields.tsx';
import { EntitlementPolicyFormFields } from './EntitlementPolicyFormFields.tsx';
import { JurisdictionSelect } from './JurisdictionSelect.tsx';
import { PublicHolidayListField } from './PublicHolidayListField.tsx';
import { RolePermissionCheckboxList } from './RolePermissionCheckboxList.tsx';
import { TenantOnboardingFormFields } from './TenantOnboardingFormFields.tsx';

interface Props {
  config: AdminResourceConfig;
  editing?: boolean;
  preferredCountry?: string | null | undefined;
  platformAdmin?: boolean;
}

export const ResourceFormFields = ({ config, editing = false, platformAdmin = false }: Props) => {
  if (config.name === 'tenants' && !editing) {
    return <TenantOnboardingFormFields />;
  }

  if (config.name === 'leave-entitlement-policies') {
    return <EntitlementPolicyFormFields editing={editing} platformAdmin={platformAdmin} />;
  }

  if (config.name === 'leave-entitlement-policy-eligibility-rules') {
    return <EligibilityRuleFormFields editing={editing} />;
  }

  return (
    <>
      {config.fields.filter((field) => !field.hidden && !field.formHidden && isAdminFieldVisible(field, platformAdmin)).map((field) => {
        const required = field.required || (!editing && field.requiredOnCreate);
        const rules: Rule[] = required ? [{ required: true, message: `${field.label} is required` }] : [];
        if (field.type === 'number') {
          rules.push({
            type: 'integer',
            ...(field.min !== undefined ? { min: field.min } : {}),
            message: `${field.label} must be a whole number${field.min !== undefined ? ` of at least ${field.min}` : ''}`,
          });
        }
        const disabled = Boolean(editing && field.readOnlyOnEdit);
        const itemProps = { name: field.name, label: field.label, rules, extra: field.description };

        if (field.type === 'boolean') {
          return (
            <Form.Item key={field.name} {...itemProps} valuePropName="checked">
              <Switch disabled={disabled} />
            </Form.Item>
          );
        }

        if (field.name === 'jurisdictionId') {
          return (
            <Form.Item key={field.name} {...itemProps}>
              <JurisdictionSelect disabled={disabled} />
            </Form.Item>
          );
        }

        if (field.type === 'select') {
          return (
            <Form.Item key={field.name} {...itemProps}>
              <Select options={field.options ?? []} disabled={disabled} />
            </Form.Item>
          );
        }

        if (field.type === 'permissions') {
          return (
            <Form.Item key={field.name} {...itemProps}>
              <RolePermissionCheckboxList disabled={disabled} />
            </Form.Item>
          );
        }

        if (field.type === 'holiday-list') {
          return (
            <PublicHolidayListField
              key={field.name}
              name={field.name}
              label={field.label}
              {...(field.description !== undefined ? { description: field.description } : {})}
              disabled={disabled}
            />
          );
        }

        if (field.type === 'json') {
          return (
            <Form.Item key={field.name} {...itemProps}>
              <Input.TextArea rows={5} disabled={disabled} placeholder="[]" />
            </Form.Item>
          );
        }

        if (field.type === 'number') {
          return (
            <Form.Item key={field.name} {...itemProps}>
              <InputNumber
                disabled={disabled}
                {...(field.min !== undefined ? { min: field.min } : {})}
                step={field.step ?? 1}
                precision={0}
                style={{ width: '100%' }}
              />
            </Form.Item>
          );
        }

        const inputType = field.type === 'date' ? 'date' : field.type === 'email' ? 'email' : 'text';
        const input = field.type === 'password'
          ? <Input.Password disabled={disabled} autoComplete="new-password" />
          : <Input type={inputType} disabled={disabled} />;

        return (
          <Form.Item key={field.name} {...itemProps}>
            {input}
          </Form.Item>
        );
      })}
    </>
  );
};
