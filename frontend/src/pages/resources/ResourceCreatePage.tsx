import { Button, Form, Input, Select, Space } from 'antd';

import { FormSection } from '../../components/common/FormSection.tsx';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import { useResourceMeta } from './resourceMeta.ts';

export const ResourceCreatePage = () => {
  const { label } = useResourceMeta();

  return (
    <PageContainer>
      <PageHeader title={`Create ${label}`} subtitle="Reusable form scaffold for new resources." />
      <FormSection title="Details">
        <Form layout="vertical">
          <Form.Item label="Name" name="name" required>
            <Input />
          </Form.Item>
          <Form.Item label="Status" name="status" initialValue="pending">
            <Select
              options={[
                { value: 'pending', label: 'Pending' },
                { value: 'approved', label: 'Approved' },
              ]}
            />
          </Form.Item>
          <Space>
            <Button type="primary">Save</Button>
            <Button htmlType="button">Cancel</Button>
          </Space>
        </Form>
      </FormSection>
    </PageContainer>
  );
};
