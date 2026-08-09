import { App, Button, Card, Input, Space } from 'antd';
import { useState } from 'react';

import { apiFetch } from '../../api/http.ts';

export const RoleMembershipCard = ({ roleId }: { roleId: string }) => {
  const [loginName, setLoginName] = useState('');
  const [pending, setPending] = useState(false);
  const { message } = App.useApp();

  const updateMembership = async (method: 'PUT' | 'DELETE') => {
    const user = loginName.trim();
    if (!user) return;
    setPending(true);
    try {
      await apiFetch(`/roles/${encodeURIComponent(roleId)}/users/${encodeURIComponent(user)}`, { method });
      message.success(method === 'PUT' ? 'User added to role' : 'User removed from role');
      setLoginName('');
    } catch {
      message.error('Unable to update role membership');
    } finally {
      setPending(false);
    }
  };

  return (
    <Card title="Role membership">
      <Space wrap>
        <Input value={loginName} onChange={(event) => setLoginName(event.target.value)} placeholder="Login name" style={{ minWidth: 240 }} />
        <Button type="primary" loading={pending} onClick={() => updateMembership('PUT')}>Add user</Button>
        <Button danger loading={pending} onClick={() => updateMembership('DELETE')}>Remove user</Button>
      </Space>
    </Card>
  );
};
