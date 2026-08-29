import { useLogin } from '@refinedev/core';
import { Alert, Button, Card, Form, Input, Space, Typography, message } from 'antd';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import {
  lookupAccountActivation,
  requestAccountActivationPin,
  setInitialAccountPassword,
  verifyAccountActivationPin,
} from '../../api/accountActivation.ts';
import { ApiError } from '../../api/http.ts';

type Step = 'IDENTIFIER' | 'PASSWORD' | 'ACTIVATION' | 'PIN' | 'SET_PASSWORD' | 'COMPLETE';

const PIN_EXPIRY_MINUTES = 15;
const RESEND_COOLDOWN_SECONDS = 60;
const PLATFORM_REALM_ID = 'PLATFORM';

export const LoginPage = () => {
  const { mutate: login, isPending: loginPending } = useLogin();
  const [searchParams] = useSearchParams();
  const redirectPath = searchParams.get('to') ?? '/';
  const [step, setStep] = useState<Step>('IDENTIFIER');
  const [tenantId, setTenantId] = useState('');
  const [loginName, setLoginName] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();
  const [cooldownUntil, setCooldownUntil] = useState<number>(0);
  const [, forceRender] = useState(0);

  const cooldownRemaining = Math.max(0, Math.ceil((cooldownUntil - Date.now()) / 1000));

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setError(undefined);
    try {
      await action();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to complete the request. Please try again.');
    } finally {
      setBusy(false);
    }
  };

  const startCooldown = () => {
    const until = Date.now() + RESEND_COOLDOWN_SECONDS * 1000;
    setCooldownUntil(until);
    const timer = window.setInterval(() => {
      forceRender((value) => value + 1);
      if (Date.now() >= until) window.clearInterval(timer);
    }, 1000);
  };

  const restart = () => {
    setStep('IDENTIFIER');
    setTenantId('');
    setLoginName('');
    setError(undefined);
    setCooldownUntil(0);
  };

  return (
    <div className="login-page">
      <Card style={{ width: '100%', maxWidth: 420 }}>
        <Space direction="vertical" size={18} style={{ width: '100%' }}>
          <Typography.Title level={3} style={{ marginBottom: 0 }}>LeaveMaestro</Typography.Title>
          <Typography.Text type="secondary">
            {step === 'IDENTIFIER'
              ? 'Enter your tenant ID and login name to continue.'
              : 'Sign in or complete your account setup.'}
          </Typography.Text>
          {error ? <Alert type="error" showIcon message={error} /> : null}

          {step === 'IDENTIFIER' ? (
            <Form layout="vertical" onFinish={({ tenantId: tenantValue, loginName: loginValue }) => run(async () => {
              const normalizedTenantId = String(tenantValue).trim();
              const normalizedLoginName = String(loginValue).trim();
              const result = await lookupAccountActivation(normalizedLoginName);
              setTenantId(normalizedTenantId);
              setLoginName(normalizedLoginName);
              setStep(result.nextStep === 'ACTIVATION' ? 'ACTIVATION' : 'PASSWORD');
            })}>
              <Form.Item
                label="Tenant ID"
                name="tenantId"
                extra={`Platform administrators use ${PLATFORM_REALM_ID}.`}
                rules={[{ required: true, message: 'Enter your tenant ID.' }]}
              >
                <Input autoComplete="organization" autoFocus />
              </Form.Item>
              <Form.Item label="Login name" name="loginName" rules={[{ required: true, message: 'Enter your login name.' }]}>
                <Input autoComplete="username" />
              </Form.Item>
              <Button type="primary" htmlType="submit" block loading={busy}>Continue</Button>
            </Form>
          ) : null}

          {step === 'PASSWORD' ? (
            <Form layout="vertical" onFinish={({ password }) => login({ tenantId, loginName, password, redirectPath })}>
              <Typography.Text strong>{tenantId} / {loginName}</Typography.Text>
              <Form.Item label="Password" name="password" rules={[{ required: true, message: 'Enter your password.' }]}>
                <Input.Password autoComplete="current-password" autoFocus />
              </Form.Item>
              <Space direction="vertical" style={{ width: '100%' }}>
                <Button type="primary" htmlType="submit" block loading={loginPending}>Sign in</Button>
                <Button type="link" block onClick={restart}>Use a different account</Button>
              </Space>
            </Form>
          ) : null}

          {step === 'ACTIVATION' ? (
            <Space direction="vertical" style={{ width: '100%' }}>
              <Typography.Text>Your account needs to be set up before you can sign in.</Typography.Text>
              <Button type="primary" block loading={busy} onClick={() => run(async () => {
                await requestAccountActivationPin(loginName);
                startCooldown();
                setStep('PIN');
                message.success('If the account is eligible, a verification PIN has been sent.');
              })}>Send verification PIN</Button>
              <Button type="link" block onClick={restart}>Use a different account</Button>
            </Space>
          ) : null}

          {step === 'PIN' ? (
            <Form layout="vertical" onFinish={({ pin }) => run(async () => {
              await verifyAccountActivationPin(loginName, String(pin));
              setStep('SET_PASSWORD');
            })}>
              <Typography.Text>Enter the 6-digit verification PIN sent to your registered email. It expires after {PIN_EXPIRY_MINUTES} minutes.</Typography.Text>
              <Form.Item label="Verification PIN" name="pin" rules={[
                { required: true, message: 'Enter the verification PIN.' },
                { pattern: /^\d{6}$/, message: 'PIN must contain exactly 6 digits.' },
              ]}>
                <Input inputMode="numeric" autoComplete="one-time-code" maxLength={6} autoFocus />
              </Form.Item>
              <Space direction="vertical" style={{ width: '100%' }}>
                <Button type="primary" htmlType="submit" block loading={busy}>Verify PIN</Button>
                <Button block disabled={busy || cooldownRemaining > 0} onClick={() => run(async () => {
                  await requestAccountActivationPin(loginName);
                  startCooldown();
                  message.success('If the account is eligible, a new verification PIN has been sent.');
                })}>
                  {cooldownRemaining > 0 ? `Resend PIN in ${cooldownRemaining}s` : 'Resend PIN'}
                </Button>
              </Space>
            </Form>
          ) : null}

          {step === 'SET_PASSWORD' ? (
            <Form layout="vertical" onFinish={({ password }) => run(async () => {
              await setInitialAccountPassword(loginName, password);
              setStep('COMPLETE');
            })}>
              <Typography.Text>Choose your permanent password.</Typography.Text>
              <Form.Item name="password" label="New password" rules={[
                { required: true, message: 'Enter a new password.' },
                { min: 8, message: 'New password must be at least 8 characters long.' },
              ]}>
                <Input.Password autoComplete="new-password" autoFocus />
              </Form.Item>
              <Form.Item name="confirmPassword" label="Confirm new password" dependencies={['password']} rules={[
                { required: true, message: 'Confirm your new password.' },
                ({ getFieldValue }) => ({ validator(_, value) {
                  return !value || value === getFieldValue('password') ? Promise.resolve() : Promise.reject(new Error('Passwords must match.'));
                } }),
              ]}>
                <Input.Password autoComplete="new-password" />
              </Form.Item>
              <Button type="primary" htmlType="submit" block loading={busy}>Activate account</Button>
            </Form>
          ) : null}

          {step === 'COMPLETE' ? (
            <Space direction="vertical" style={{ width: '100%' }}>
              <Alert type="success" showIcon message="Account activated" description="Your password has been set. You can now sign in." />
              <Button type="primary" block onClick={() => setStep('PASSWORD')}>Continue to sign in</Button>
            </Space>
          ) : null}
        </Space>
      </Card>
    </div>
  );
};
