import { useCreate, useResource } from '@refinedev/core';
import { App, Button, Form, Space } from 'antd';
import { useNavigate } from 'react-router-dom';

import { FormSection } from '../../components/common/FormSection.tsx';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import { getAdminResourceConfig, normaliseFormValues } from './adminResourceConfig.ts';
import { ResourceFormFields } from './ResourceFormFields.tsx';

export const ResourceCreatePage = () => {
  const { resource } = useResource();
  const config = getAdminResourceConfig(resource?.name);
  const { mutateAsync, mutation: { isPending } } = useCreate();
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const { message } = App.useApp();

  if (!config) return null;

  const submit = async (values: Record<string, unknown>) => {
    try {
      await mutateAsync({ resource: config.name, values: normaliseFormValues(config, values), successNotification: false });
      message.success(`${config.singular} created`);
      navigate(`/${config.name}`);
    } catch {
      message.error(`Unable to create ${config.singular.toLowerCase()}`);
    }
  };

  return (
    <PageContainer>
      <PageHeader title={`Create ${config.singular}`} subtitle={`Add a new ${config.singular.toLowerCase()} record.`} />
      <FormSection title="Details">
        <Form form={form} layout="vertical" onFinish={submit} initialValues={{ active: true, used: false, status: 'ACTIVE' }}>
          <ResourceFormFields config={config} />
          <Space>
            <Button type="primary" htmlType="submit" loading={isPending}>Save</Button>
            <Button htmlType="button" onClick={() => navigate(`/${config.name}`)}>Cancel</Button>
          </Space>
        </Form>
      </FormSection>
    </PageContainer>
  );
};
