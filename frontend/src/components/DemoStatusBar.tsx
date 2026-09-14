import { useGetIdentity, useLogout } from '@refinedev/core';
import { Alert, Button, Space } from 'antd';
import { useNavigate } from 'react-router-dom';

interface DemoIdentity {
  demo?: boolean;
  loginName?: string;
}

const personaLabel = (loginName?: string) => {
  if (loginName === 'demo.hr') return 'HR';
  if (loginName === 'demo.manager') return 'Manager';
  if (loginName === 'demo.staff') return 'Employee';
  return 'Demo user';
};

export const DemoStatusBar = () => {
  const { data: identity } = useGetIdentity<DemoIdentity>();
  const { mutate: logout, isPending } = useLogout();
  const navigate = useNavigate();

  if (!identity?.demo) return null;

  return (
    <Alert
      banner
      showIcon
      type="warning"
      message={
        <Space wrap>
          <strong>Public demo environment</strong>
          <span>You are exploring LeaveMaestro as {personaLabel(identity.loginName)} using fictional data.</span>
          <Button
            size="small"
            loading={isPending}
            onClick={() => logout(undefined, { onSuccess: () => navigate('/demo') })}
          >
            Switch persona
          </Button>
        </Space>
      }
    />
  );
};
