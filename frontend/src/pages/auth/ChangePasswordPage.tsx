import { LockOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Form, Input, Space, Typography, message } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { ApiError, apiFetch } from '../../api/http.ts';

type ChangePasswordValues = {
  currentPassword: string;
  newPassword: string;
  confirmNewPassword: string;
};

export const ChangePasswordPage = () => {
  const [form] = Form.useForm<ChangePasswordValues>();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string>();
  const navigate = useNavigate();

  const onFinish = async (values: ChangePasswordValues) => {
    setSubmitting(true);
    setError(undefined);
    try {
      await apiFetch<void>('/auth/change-password', {
        method: 'PUT',
        body: JSON.stringify(values),
      });
      message.success('Password changed successfully.');
      form.resetFields();
      navigate('/');
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to change password.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card style={{ maxWidth: 560, margin: '0 auto' }}>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <div>
          <Typography.Title level={3} style={{ marginBottom: 4 }}>
            <LockOutlined /> Change password
          </Typography.Title>
          <Typography.Text type="secondary">Enter your current password and choose a new password of at least 8 characters.</Typography.Text>
        </div>

        {error ? <Alert type="error" showIcon message={error} /> : null}

        <Form form={form} layout="vertical" onFinish={onFinish} autoComplete="off">
          <Form.Item name="currentPassword" label="Current password" rules={[{ required: true, message: 'Enter your current password.' }]}>
            <Input.Password autoComplete="current-password" />
          </Form.Item>

          <Form.Item
            name="newPassword"
            label="New password"
            rules={[
              { required: true, message: 'Enter a new password.' },
              { min: 8, message: 'New password must be at least 8 characters long.' },
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>

          <Form.Item
            name="confirmNewPassword"
            label="Confirm new password"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: 'Confirm your new password.' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  return !value || getFieldValue('newPassword') === value
                    ? Promise.resolve()
                    : Promise.reject(new Error('New password and confirmation must match.'));
                },
              }),
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>

          <Space>
            <Button type="primary" htmlType="submit" loading={submitting}>Change password</Button>
            <Button onClick={() => navigate(-1)} disabled={submitting}>Cancel</Button>
          </Space>
        </Form>
      </Space>
    </Card>
  );
};
