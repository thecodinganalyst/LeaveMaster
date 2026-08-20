import { Button, Card, Form, Input, Select, Space, Switch, Typography } from 'antd';
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';

import { JurisdictionSelect } from './JurisdictionSelect.tsx';

const tenantStatus = [
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Dormant', value: 'DORMANT' },
  { label: 'Terminated', value: 'TERMINATED' },
];

export const TenantOnboardingFormFields = () => (
  <>
    <Form.Item name="id" label="ID" rules={[{ required: true, message: 'ID is required' }]}>
      <Input />
    </Form.Item>
    <Form.Item name="name" label="Name" rules={[{ required: true, message: 'Name is required' }]}>
      <Input />
    </Form.Item>
    <Form.Item name="startDate" label="Tenant start date" rules={[{ required: true, message: 'Start date is required' }]}>
      <Input type="date" />
    </Form.Item>
    <Form.Item name="endDate" label="Tenant end date">
      <Input type="date" />
    </Form.Item>
    <Form.Item name="status" label="Status" rules={[{ required: true, message: 'Status is required' }]}>
      <Select options={tenantStatus} />
    </Form.Item>

    <Typography.Title level={5}>Jurisdictions and template setup</Typography.Title>
    <Typography.Paragraph type="secondary">
      Select every jurisdiction the tenant will operate in. Template choices are independent for each jurisdiction and the copied records become tenant-owned configuration.
    </Typography.Paragraph>

    <Form.List
      name="jurisdictions"
      rules={[{
        validator: async (_, value) => {
          if (!Array.isArray(value) || value.length === 0) throw new Error('Select at least one jurisdiction');
        },
      }]}
    >
      {(fields, { add, remove }, { errors }) => (
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          {fields.map((field, index) => (
            <Card
              key={field.key}
              size="small"
              title={`Jurisdiction ${index + 1}`}
              extra={fields.length > 1 ? <Button type="text" danger icon={<MinusCircleOutlined />} onClick={() => remove(field.name)} /> : null}
            >
              <Form.Item
                name={[field.name, 'jurisdictionId']}
                label="Jurisdiction"
                rules={[{ required: true, message: 'Jurisdiction is required' }]}
              >
                <JurisdictionSelect />
              </Form.Item>
              <Form.Item name={[field.name, 'includePublicHolidays']} label="Add public holidays from template" valuePropName="checked">
                <Switch />
              </Form.Item>
              <Form.Item
                name={[field.name, 'includeLeaveConfiguration']}
                label="Add leave types, entitlement policies and eligibility rules from template"
                valuePropName="checked"
              >
                <Switch />
              </Form.Item>
            </Card>
          ))}
          <Button
            type="dashed"
            icon={<PlusOutlined />}
            onClick={() => add({ includePublicHolidays: true, includeLeaveConfiguration: true })}
            block
          >
            Add jurisdiction
          </Button>
          <Form.ErrorList errors={errors} />
        </Space>
      )}
    </Form.List>

    <Typography.Title level={5} style={{ marginTop: 24 }}>Initial leave calendar</Typography.Title>
    <Typography.Paragraph type="secondary">
      These dates are used when public holidays are imported. They default to the current calendar year and can be changed before the tenant is created.
    </Typography.Paragraph>
    <Space size="middle" style={{ width: '100%' }} align="start">
      <Form.Item name="calendarStart" label="Calendar start" rules={[{ required: true, message: 'Calendar start is required' }]} style={{ flex: 1 }}>
        <Input type="date" />
      </Form.Item>
      <Form.Item name="calendarEnd" label="Calendar end" rules={[{ required: true, message: 'Calendar end is required' }]} style={{ flex: 1 }}>
        <Input type="date" />
      </Form.Item>
    </Space>
  </>
);
