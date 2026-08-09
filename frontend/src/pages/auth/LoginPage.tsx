import { useLogin } from '@refinedev/core';
import { Button, Card, Form, Input, Space, Typography } from 'antd';

interface LoginFormValues {
  email: string;
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
            Sign in to continue managing leave requests and workforce planning.
          </Typography.Text>
          <Typography.Text type="secondary">
            Demo credentials: admin@leavemaster.dev / LeaveMaster123!
          </Typography.Text>

          <Form<LoginFormValues>
            layout="vertical"
            initialValues={{ email: 'admin@leavemaster.dev', password: 'LeaveMaster123!' }}
            onFinish={({ email, password }) => {
              login({ email, password });
            }}
          >
            <Form.Item label="Work email" name="email" rules={[{ required: true }, { type: 'email' }]}>
              <Input type="email" autoComplete="email" />
            </Form.Item>
            <Form.Item label="Password" name="password" rules={[{ required: true, min: 8 }]}>
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
