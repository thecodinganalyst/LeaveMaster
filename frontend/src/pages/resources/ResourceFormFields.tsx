import { Form, Input, Select, Switch } from 'antd';

import type { AdminResourceConfig } from './adminResourceConfig.ts';

interface Props {
  config: AdminResourceConfig;
  editing?: boolean;
}

export const ResourceFormFields = ({ config, editing = false }: Props) => (
  <>
    {config.fields.filter((field) => !field.hidden).map((field) => {
      const required = field.required || (!editing && field.requiredOnCreate);
      const rules = required ? [{ required: true, message: `${field.label} is required` }] : [];
      const disabled = Boolean(editing && field.readOnlyOnEdit);

      if (field.type === 'boolean') {
        return (
          <Form.Item key={field.name} name={field.name} label={field.label} valuePropName="checked">
            <Switch disabled={disabled} />
          </Form.Item>
        );
      }

      if (field.type === 'select') {
        return (
          <Form.Item key={field.name} name={field.name} label={field.label} rules={rules}>
            <Select options={field.options ?? []} disabled={disabled} />
          </Form.Item>
        );
      }

      if (field.type === 'json') {
        return (
          <Form.Item key={field.name} name={field.name} label={field.label} rules={rules}>
            <Input.TextArea rows={5} disabled={disabled} placeholder="[]" />
          </Form.Item>
        );
      }

      const inputType = field.type === 'date' ? 'date' : field.type === 'email' ? 'email' : 'text';
      const input = field.type === 'password'
        ? <Input.Password disabled={disabled} autoComplete="new-password" />
        : <Input type={inputType} disabled={disabled} />;

      return (
        <Form.Item key={field.name} name={field.name} label={field.label} rules={rules}>
          {input}
        </Form.Item>
      );
    })}
  </>
);
