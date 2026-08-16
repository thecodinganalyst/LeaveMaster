import { useCreate, useGetIdentity, useResource } from '@refinedev/core';
import { App, Button, Form, Space } from 'antd';
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

import { FormSection } from '../../components/common/FormSection.tsx';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import { getAdminResourceConfig, getAdminResourceInitialValues, normaliseFormValues } from './adminResourceConfig.ts';
import { ResourceFormFields } from './ResourceFormFields.tsx';

interface LeaveMasterIdentity {
  id: string;
  name: string;
  country?: string | null;
  platformAdmin?: boolean;
}

export const ResourceCreatePage = () => {
  const { resource } = useResource();
  const config = getAdminResourceConfig(resource?.name);
  const { mutateAsync, isLoading } = useCreate();
  const { data: identity } = useGetIdentity<LeaveMasterIdentity>();
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const { message } = App.useApp();

  useEffect(() => {
    if (config?.name !== 'locations' || !identity?.country || form.getFieldValue('country')) return;
    form.setFieldValue('country', identity.country);
  }, [config?.name, form, identity?.country]);

  if (!config || config.creatable === false) return null;

  const submit = async (values: Record<string, unknown>) => {
    try {
      await mutateAsync({ resource: config.name, values: normaliseFormValues(config, values) });
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
        <Form
          form={form}
          layout="vertical"
          onFinish={submit}
          initialValues={{ active: true, used: false, status: 'ACTIVE', ...getAdminResourceInitialValues(config) }}
        >
          <ResourceFormFields config={config} preferredCountry={identity?.country} platformAdmin={Boolean(identity?.platformAdmin)} />
          <Space>
            <Button type="primary" htmlType="submit" loading={isLoading}>Save</Button>
            <Button htmlType="button" onClick={() => navigate(`/${config.name}`)}>Cancel</Button>
          </Space>
        </Form>
      </FormSection>
    </PageContainer>
  );
};
