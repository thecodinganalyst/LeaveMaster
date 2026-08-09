import { useLogin } from '@refinedev/core';
import { Button, Card, Form, Input, Space, Typography } from 'antd';

interface LoginFormValues {
  loginName: string;
  password: string;
}

export const LoginPage = () => {
  const { mutate: login, isPending } = useLogin();

  return (
    <div className="login-page">
      <Card style={{ width: '100%', maxWidth: 420 }}>
        <Space direction="vertical" size={18} style={{ width: '100%' }}>
          <Typography.Title level={3} style={{ marginBottom: 0 }}>
            LeaveMaster
          </Typography.Title>
          <Typography.Text type="secondary">
            Sign in with your LeaveMaster account to continue.
          </Typography.Text>

          <Form<LoginFormValues>
            layout="vertical"
            onFinish={({ loginName, password }) => {
              login({ loginName, password });
            }}
          >
            <Form.Item label="Login name" name="loginName" rules={[{ required: true }]}> 
              <Input autoComplete="username" />
            </Form.Item>
            <Form.Item label="Password" name="password" rules={[{ required: true }]}> 
              <Input.Password autoComplete="current-password" />
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
