import { Button, Form, Input, Space } from 'antd';

interface Props {
  name: string;
  label: string;
  description?: string;
  disabled?: boolean;
}

interface PublicHolidayValue {
  holidayDate?: string;
  holidayName?: string;
}

export const PublicHolidayListField = ({ name, label, description, disabled = false }: Props) => (
  <Form.Item label={label} extra={description}>
    <Form.List
      name={name}
      rules={[
        {
          validator: async (_, holidays: PublicHolidayValue[] | undefined) => {
            const dates = (holidays ?? [])
              .map((holiday) => holiday?.holidayDate)
              .filter((date): date is string => Boolean(date));
            if (new Set(dates).size !== dates.length) {
              throw new Error('Public holiday dates must be unique');
            }
          },
        },
      ]}
    >
      {(fields, { add, remove }, { errors }) => (
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          {fields.map((field) => (
            <Space key={field.key} align="start" wrap style={{ width: '100%' }}>
              <Form.Item
                {...field}
                name={[field.name, 'holidayDate']}
                label="Holiday date"
                rules={[{ required: true, message: 'Holiday date is required' }]}
              >
                <Input type="date" disabled={disabled} />
              </Form.Item>
              <Form.Item
                {...field}
                name={[field.name, 'holidayName']}
                label="Holiday name"
                rules={[{ required: true, message: 'Holiday name is required' }]}
              >
                <Input disabled={disabled} />
              </Form.Item>
              <Button danger disabled={disabled} onClick={() => remove(field.name)}>
                Remove
              </Button>
            </Space>
          ))}
          <Button type="dashed" disabled={disabled} onClick={() => add({ holidayDate: '', holidayName: '' })}>
            Add holiday
          </Button>
          <Form.ErrorList errors={errors} />
        </Space>
      )}
    </Form.List>
  </Form.Item>
);
