import { useOne, useResource, useUpdate } from '@refinedev/core';
import { App, Button, Form, Space, Spin } from 'antd';
import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { FormSection } from '../../components/common/FormSection.tsx';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import { getAdminResourceConfig, normaliseFormValues, toFormValues } from './adminResourceConfig.ts';
import { ResourceFormFields } from './ResourceFormFields.tsx';

export const ResourceEditPage = () => {
  const { resource } = useResource();
  const { id } = useParams();
  const config = getAdminResourceConfig(resource?.name);
  const recordQuery = useOne({ resource: config?.name ?? '', id: id ?? '', queryOptions: { enabled: Boolean(config && id) } });
  const { mutateAsync, isLoading: isUpdating } = useUpdate();
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const { message } = App.useApp();

  useEffect(() => {
    if (config && recordQuery.data?.data) form.setFieldsValue(toFormValues(config, recordQuery.data.data as Record<string, unknown>));
  }, [config, form, recordQuery.data]);

  if (!config || !id || config.editable === false) return null;
  if (recordQuery.isLoading) return <Spin />;

  const submit = async (values: Record<string, unknown>) => {
    try {
      const payload = normaliseFormValues(config, values);
      for (const field of config.fields.filter((item) => item.readOnlyOnEdit)) delete payload[field.name];
      await mutateAsync({ resource: config.name, id, values: payload });
      message.success(`${config.singular} updated`);
      navigate(`/${config.name}/show/${encodeURIComponent(id)}`);
    } catch {
      message.error(`Unable to update ${config.singular.toLowerCase()}`);
    }
  };

  return (
    <PageContainer>
      <PageHeader title={`Edit ${config.singular}`} subtitle={`Update ${id}.`} />
      <FormSection title="Details">
        <Form form={form} layout="vertical" onFinish={submit}>
          <ResourceFormFields config={config} editing />
          <Space>
            <Button type="primary" htmlType="submit" loading={isUpdating}>Save changes</Button>
            <Button htmlType="button" onClick={() => navigate(`/${config.name}/show/${encodeURIComponent(id)}`)}>Cancel</Button>
          </Space>
        </Form>
      </FormSection>
    </PageContainer>
  );
};
