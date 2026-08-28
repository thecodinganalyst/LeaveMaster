import { useCreate, useGetIdentity, useResource } from '@refinedev/core';
import { App, Button, Form, Space } from 'antd';
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

import { FormSection } from '../../components/common/FormSection.tsx';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import { getAdminResourceConfig, getAdminResourceInitialValues, normaliseFormValues } from './resourceConfigResolver.ts';
import { ResourceFormFields } from './ResourceFormFields.tsx';
import { syncStaffDependants, type StaffDependantValue } from './staffDependants.ts';
import { tenantOnboardingInitialValues } from './tenantOnboarding.ts';

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
      const dependants = config.name === 'employees' ? values.dependants as StaffDependantValue[] | undefined : undefined;
      const payload = normaliseFormValues(config, values);
      if (config.name === 'employees') delete payload.dependants;
      const result = await mutateAsync({ resource: config.name, values: payload });
      const createdId = String((result.data as Record<string, unknown> | undefined)?.[config.idField] ?? values[config.idField] ?? '');

      if (config.name === 'employees' && createdId) {
        try {
          await syncStaffDependants(createdId, dependants);
        } catch (error) {
          message.warning(`Staff created, but dependant changes were not fully saved: ${error instanceof Error ? error.message : 'unknown error'}`);
          navigate(`/${config.name}/edit/${encodeURIComponent(createdId)}`);
          return;
        }
      }

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
          initialValues={{
            active: true,
            used: false,
            status: 'ACTIVE',
            ...getAdminResourceInitialValues(config),
            ...tenantOnboardingInitialValues(config.name),
          }}
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
