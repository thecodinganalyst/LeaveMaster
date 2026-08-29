import { useCreate, useGetIdentity, useResource } from '@refinedev/core';
import { App, Button, Form, Space } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { FormSection } from '../../components/common/FormSection.tsx';
import { PageContainer } from '../../components/common/PageContainer.tsx';
import { PageHeader } from '../../components/common/PageHeader.tsx';
import { getAdminResourceConfig, getAdminResourceInitialValues, normaliseFormValues } from './resourceConfigResolver.ts';
import { ResourceFormFields } from './ResourceFormFields.tsx';
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
  const [staffCreationStep, setStaffCreationStep] = useState<0 | 1>(0);
  const navigate = useNavigate();
  const { message } = App.useApp();

  useEffect(() => {
    if (config?.name !== 'locations' || !identity?.country || form.getFieldValue('country')) return;
    form.setFieldValue('country', identity.country);
  }, [config?.name, form, identity?.country]);

  if (!config || config.creatable === false) return null;
  const isStaffCreation = config.name === 'employees';

  const submit = async (values: Record<string, unknown>) => {
    if (isStaffCreation && staffCreationStep === 0) return;
    try {
      const payload = normaliseFormValues(config, values);
      await mutateAsync({ resource: config.name, values: payload });
      message.success(`${config.singular} created`);
      navigate(`/${config.name}`);
    } catch {
      message.error(`Unable to create ${config.singular.toLowerCase()}`);
    }
  };

  const nextStaffStep = async () => {
    try {
      await form.validateFields();
      form.setFieldValue('leaveEntitlements', []);
      setStaffCreationStep(1);
    } catch {
      // Ant Design displays field-level validation feedback.
    }
  };

  return (
    <PageContainer>
      <PageHeader title={`Create ${config.singular}`} subtitle={`Add a new ${config.singular.toLowerCase()} record.`} />
      <FormSection title={isStaffCreation ? (staffCreationStep === 0 ? 'Staff details' : 'Review and confirm') : 'Details'}>
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
          <ResourceFormFields
            config={config}
            preferredCountry={identity?.country}
            platformAdmin={Boolean(identity?.platformAdmin)}
            {...(isStaffCreation ? { staffCreationStep } : {})}
          />
          <Space>
            {isStaffCreation && staffCreationStep === 0 ? (
              <Button type="primary" htmlType="button" onClick={() => void nextStaffStep()}>Next: Review Entitlements</Button>
            ) : null}
            {isStaffCreation && staffCreationStep === 1 ? (
              <>
                <Button htmlType="button" disabled={isLoading} onClick={() => setStaffCreationStep(0)}>Back</Button>
                <Button type="primary" htmlType="submit" loading={isLoading}>Confirm &amp; Create Staff</Button>
              </>
            ) : null}
            {!isStaffCreation ? <Button type="primary" htmlType="submit" loading={isLoading}>Save</Button> : null}
            <Button htmlType="button" disabled={isLoading} onClick={() => navigate(`/${config.name}`)}>Cancel</Button>
          </Space>
        </Form>
      </FormSection>
    </PageContainer>
  );
};
