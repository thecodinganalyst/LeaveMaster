import { Form, Input, InputNumber, Select, Switch } from 'antd';
import type { Rule } from 'antd/es/form';

import type { AdminResourceConfig } from './adminResourceConfig.ts';
import { isAdminFieldVisible } from './adminResourceConfig.ts';
import { getCountryOptions } from './countries.ts';
import { JurisdictionSelect } from './JurisdictionSelect.tsx';
import { RolePermissionCheckboxList } from './RolePermissionCheckboxList.tsx';

interface Props {
  config: AdminResourceConfig;
  editing?: boolean;
  preferredCountry?: string | null | undefined;
  platformAdmin?: boolean;
}

export const ResourceFormFields = ({ config, editing = false, preferredCountry, platformAdmin = false }: Props) => (
  <>
    {config.fields.filter((field) => !field.hidden && !field.formHidden && isAdminFieldVisible(field, platformAdmin)).map((field) => {
      const required = field.required || (!editing && field.requiredOnCreate);
      const rules: Rule[] = required ? [{ required: true, message: `${field.label} is required` }] : [];
      if (field.type === 'number') {
        rules.push({
          type: 'integer',
          min: field.min,
          message: `${field.label} must be a whole number${field.min !== undefined ? ` of at least ${field.min}` : ''}`,
        });
      }
      const disabled = Boolean(editing && field.readOnlyOnEdit);
      const itemProps = { key: field.name, name: field.name, label: field.label, rules, extra: field.description };

      if (field.type === 'boolean') {
        return (
          <Form.Item {...itemProps} valuePropName="checked">
            <Switch disabled={disabled} />
          </Form.Item>
        );
      }

      if (field.name === 'jurisdictionId') {
        return (
          <Form.Item {...itemProps}>
            <JurisdictionSelect disabled={disabled} />
          </Form.Item>
        );
      }

      if (field.type === 'select') {
        return (
          <Form.Item {...itemProps}>
            <Select options={field.options ?? []} disabled={disabled} />
          </Form.Item>
        );
      }

      if (field.type === 'country') {
        return (
          <Form.Item {...itemProps}>
            <Select
              options={getCountryOptions(preferredCountry)}
              disabled={disabled}
              showSearch
              optionFilterProp="label"
              placeholder="Select a country"
            />
          </Form.Item>
        );
      }

      if (field.type === 'permissions') {
        return (
          <Form.Item {...itemProps}>
            <RolePermissionCheckboxList disabled={disabled} />
          </Form.Item>
        );
      }

      if (field.type === 'json') {
        return (
          <Form.Item {...itemProps}>
            <Input.TextArea rows={5} disabled={disabled} placeholder="[]" />
          </Form.Item>
        );
      }

      if (field.type === 'number') {
        return (
          <Form.Item {...itemProps}>
            <InputNumber
              disabled={disabled}
              min={field.min}
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
        <Form.Item {...itemProps}>
          {input}
        </Form.Item>
      );
    })}
  </>
);
