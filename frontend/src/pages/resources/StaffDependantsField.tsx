import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Card, Col, Divider, Form, Input, Row, Select, Space, Switch, Typography } from 'antd';
import { useEffect } from 'react';

import { loadStaffDependants } from './staffDependants.ts';

const RELATIONSHIP_OPTIONS = [
  { value: 'CHILD', label: 'Child' },
  { value: 'SPOUSE', label: 'Spouse' },
  { value: 'PARENT', label: 'Parent' },
  { value: 'OTHER', label: 'Other' },
];

interface Props {
  editing?: boolean;
  staffId?: string;
}

export const StaffDependantsField = ({ editing = false, staffId }: Props) => {
  const form = Form.useFormInstance();
  const dependantsQuery = useQuery({
    queryKey: ['staff-dependants', staffId],
    queryFn: () => loadStaffDependants(staffId!),
    enabled: Boolean(editing && staffId),
  });

  useEffect(() => {
    if (!editing || !dependantsQuery.data || form.getFieldValue('dependants') !== undefined) return;
    form.setFieldValue('dependants', dependantsQuery.data.map((dependant) => ({
      ...dependant,
      dateOfBirth: dependant.dateOfBirth ?? undefined,
      citizenshipCode: dependant.citizenshipCode ?? undefined,
      residencyCode: dependant.residencyCode ?? undefined,
      adoptionDate: dependant.adoptionDate ?? undefined,
      effectiveFrom: dependant.effectiveFrom ?? undefined,
      effectiveTo: dependant.effectiveTo ?? undefined,
      active: dependant.active ?? true,
    })));
  }, [dependantsQuery.data, editing, form]);

  return (
    <>
      <Divider orientation="left">Dependants</Divider>
      <Typography.Paragraph type="secondary">
        Maintain dependant facts used by leave eligibility rules such as childcare and extended childcare leave. Changes are saved with the staff record.
      </Typography.Paragraph>
      {dependantsQuery.isError ? (
        <Alert type="error" showIcon message="Unable to load dependant records" style={{ marginBottom: 16 }} />
      ) : null}
      <Form.List name="dependants">
        {(fields, { add, remove }) => (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            {fields.map((field, index) => (
              <Card key={field.key} size="small" title={`Dependant ${index + 1}`}>
                <Form.Item name={[field.name, 'id']} hidden><Input /></Form.Item>
                <Row gutter={12}>
                  <Col xs={24} md={12}>
                    <Form.Item name={[field.name, 'name']} label="Name" rules={[{ required: true, message: 'Dependant name is required' }]}>
                      <Input />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item name={[field.name, 'relationshipCode']} label="Relationship" rules={[{ required: true, message: 'Relationship is required' }]}>
                      <Select options={RELATIONSHIP_OPTIONS} showSearch optionFilterProp="label" />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item name={[field.name, 'dateOfBirth']} label="Date of birth">
                      <Input type="date" allowClear />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item name={[field.name, 'citizenshipCode']} label="Citizenship code" extra="Use the ISO country code used by eligibility rules, e.g. SG.">
                      <Input maxLength={64} placeholder="SG" />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item name={[field.name, 'residencyCode']} label="Residency code" extra="Optional jurisdiction-neutral residency code.">
                      <Input maxLength={64} />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item
                      name={[field.name, 'adoptionDate']}
                      label="Adoption date"
                      dependencies={[['dependants', field.name, 'dateOfBirth']]}
                      rules={[({ getFieldValue }) => ({
                        validator(_, value?: string) {
                          const birthDate = (getFieldValue('dependants') as Array<{ dateOfBirth?: string }> | undefined)?.[field.name]?.dateOfBirth;
                          return !value || !birthDate || value >= birthDate
                            ? Promise.resolve()
                            : Promise.reject(new Error('Adoption date must not be before date of birth'));
                        },
                      })]}
                    >
                      <Input type="date" allowClear />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item name={[field.name, 'effectiveFrom']} label="Effective from">
                      <Input type="date" allowClear />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item
                      name={[field.name, 'effectiveTo']}
                      label="Effective to"
                      dependencies={[['dependants', field.name, 'effectiveFrom']]}
                      rules={[({ getFieldValue }) => ({
                        validator(_, value?: string) {
                          const from = (getFieldValue('dependants') as Array<{ effectiveFrom?: string }> | undefined)?.[field.name]?.effectiveFrom;
                          return !value || !from || value >= from
                            ? Promise.resolve()
                            : Promise.reject(new Error('Effective to must not be before effective from'));
                        },
                      })]}
                    >
                      <Input type="date" allowClear />
                    </Form.Item>
                  </Col>
                </Row>
                <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
                  <Form.Item name={[field.name, 'active']} label="Active" valuePropName="checked" style={{ marginBottom: 0 }}>
                    <Switch />
                  </Form.Item>
                  <Button danger icon={<DeleteOutlined />} onClick={() => remove(field.name)}>Remove dependant</Button>
                </Space>
              </Card>
            ))}
            <Button type="dashed" icon={<PlusOutlined />} onClick={() => add({ active: true })}>
              Add dependant
            </Button>
          </Space>
        )}
      </Form.List>
    </>
  );
};
