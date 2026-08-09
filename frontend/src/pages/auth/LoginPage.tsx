import { useState } from 'react';
import { useLogin } from '@refinedev/core';
import { Button, Card, Form, Input, Space, Typography } from 'antd';

export const LoginPage = () => {
  const { mutate: login, isPending } = useLogin();
  const [email, setEmail] = useState('admin@leavemaster.dev');

  return (
    <div className="login-page">
      <Card style={{ width: '100%', maxWidth: 420 }}>
        <Space direction="vertical" size={18} style={{ width: '100%' }}>
          <Typography.Title level={3} style={{ marginBottom: 0 }}>
            LeaveMaster
          </Typography.Title>
          <Typography.Text type="secondary">
            Sign in to continue managing leave requests and workforce planning.
          </Typography.Text>

          <Form
            layout="vertical"
            onFinish={() => {
              login({ email });
            }}
          >
            <Form.Item label="Work email" name="email" initialValue={email}>
              <Input
                type="email"
                autoComplete="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={isPending}>
              Sign in
            </Button>
          </Form>
        </Space>
      </Card>
    </div>
  );
};
