import { GithubOutlined, GoogleOutlined } from '@ant-design/icons';
import { useLogin } from '@refinedev/core';
import { Alert, Button, Card, Divider, Form, Input, Space, Typography, message } from 'antd';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import {
  lookupAccountActivation,
  requestAccountActivationPin,
  setInitialAccountPassword,
  verifyAccountActivationPin,
} from '../../api/accountActivation.ts';
import { ApiError, loginWithSession } from '../../api/http.ts';
import {
  clearRememberedOAuthProvider,
  getRememberedOAuthProvider,
  startOAuthLink,
  startOAuthLogin,
  type OAuthProvider,
} from '../../api/oauth.ts';

type Step = 'IDENTIFIER' | 'PASSWORD' | 'ACTIVATION' | 'PIN' | 'SET_PASSWORD' | 'COMPLETE';

const PIN_EXPIRY_MINUTES = 15;
const RESEND_COOLDOWN_SECONDS = 60;

const providerLabel = (provider?: OAuthProvider) => provider === 'google' ? 'Google' : provider === 'github' ? 'GitHub' : 'OAuth';

const oauthErrorMessage = (code: string | null, provider?: OAuthProvider) => {
  const label = providerLabel(provider);
  switch (code) {
    case 'not_linked':
    case 'link_context_invalid':
      return `This ${label} account is not linked to LeaveMaestro yet. Verify your existing LeaveMaestro account to set it up.`;
    case 'identity_in_use':
      return `This ${label} account is already linked to another LeaveMaestro account.`;
    case 'already_linked':
      return 'This LeaveMaestro account already has an OAuth provider linked.';
    case 'account_inactive':
    case 'account_not_eligible':
      return 'This LeaveMaestro account is not eligible for OAuth sign-in.';
    case 'access_denied':
      return `${label} sign-in was cancelled or denied. You can try again.`;
    case 'unsupported_provider':
      return 'That OAuth provider is not supported.';
    default:
      return code ? `${label} sign-in could not be completed. Please try again.` : undefined;
  }
};

export const LoginPage = () => {
  const { mutate: login, isPending: loginPending } = useLogin();
  const [searchParams] = useSearchParams();
  const redirectPath = searchParams.get('to') ?? '/';
  const oauthError = searchParams.get('oauthError');
  const rememberedProvider = getRememberedOAuthProvider();
  const initialSetupProvider = oauthError === 'not_linked' || oauthError === 'link_context_invalid'
    ? rememberedProvider
    : undefined;

  const [step, setStep] = useState<Step>('IDENTIFIER');
  const [tenantId, setTenantId] = useState('');
  const [loginName, setLoginName] = useState('');
  const [identifierFormKey, setIdentifierFormKey] = useState(0);
  const [oauthSetupProvider, setOAuthSetupProvider] = useState<OAuthProvider | undefined>(initialSetupProvider);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | undefined>(() => oauthErrorMessage(oauthError, rememberedProvider));
  const [cooldownUntil, setCooldownUntil] = useState<number>(0);
  const [, forceRender] = useState(0);

  const cooldownRemaining = Math.max(0, Math.ceil((cooldownUntil - Date.now()) / 1000));
  const accountIdentity = { tenantId, loginName };
  const oauthLabel = providerLabel(oauthSetupProvider);

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setError(undefined);
    try {
      await action();
    } catch (caught) {
      setError(caught instanceof ApiError || caught instanceof Error
        ? caught.message
        : 'Unable to complete the request. Please try again.');
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

  const restartAccount = () => {
    setStep('IDENTIFIER');
    setTenantId('');
    setLoginName('');
    setIdentifierFormKey((value) => value + 1);
    setError(undefined);
    setCooldownUntil(0);
  };

  const backToLogin = () => {
    restartAccount();
    setOAuthSetupProvider(undefined);
    clearRememberedOAuthProvider();
  };

  const continueOAuthSetup = async (password: string) => {
    if (!oauthSetupProvider) return;
    await loginWithSession(tenantId, loginName, password);
    await startOAuthLink(oauthSetupProvider);
  };

  const accountContext = step === 'IDENTIFIER' ? null : (
    <Typography.Text strong>{tenantId} / {loginName}</Typography.Text>
  );

  return (
    <div className="login-page">
      <Card style={{ width: '100%', maxWidth: 420 }}>
        <Space direction="vertical" size={18} style={{ width: '100%' }}>
          <Typography.Title level={3} style={{ marginBottom: 0 }}>LeaveMaestro</Typography.Title>
          <Typography.Text type="secondary">
            {oauthSetupProvider
              ? `Set up ${oauthLabel} sign-in by verifying your existing LeaveMaestro account.`
              : step === 'IDENTIFIER'
                ? 'Sign in with Google, GitHub, or your LeaveMaestro account.'
                : 'Sign in or complete your account setup.'}
          </Typography.Text>
          {error ? <Alert type="error" showIcon message={error} /> : null}

          {step === 'IDENTIFIER' && !oauthSetupProvider ? (
            <>
              <Space direction="vertical" style={{ width: '100%' }}>
                <Button icon={<GoogleOutlined />} block onClick={() => startOAuthLogin('google')}>
                  Continue with Google
                </Button>
                <Button icon={<GithubOutlined />} block onClick={() => startOAuthLogin('github')}>
                  Continue with GitHub
                </Button>
              </Space>
              <Divider plain>or use your LeaveMaestro account</Divider>
            </>
          ) : null}

          {step === 'IDENTIFIER' && oauthSetupProvider ? (
            <Alert
              type="info"
              showIcon
              message={`Set up ${oauthLabel} sign-in`}
              description={`Enter the tenant ID and login name of the LeaveMaestro account you want to link to this ${oauthLabel} account.`}
            />
          ) : null}

          {step === 'IDENTIFIER' ? (
            <Form key={identifierFormKey} layout="vertical" onFinish={({ tenantId: tenantValue, loginName: loginValue }) => run(async () => {
              const normalizedTenantId = String(tenantValue).trim();
              const normalizedLoginName = String(loginValue).trim();
              const identity = { tenantId: normalizedTenantId, loginName: normalizedLoginName };
              const result = await lookupAccountActivation(identity);
              setTenantId(normalizedTenantId);
              setLoginName(normalizedLoginName);
              setStep(result.nextStep === 'ACTIVATION' ? 'ACTIVATION' : 'PASSWORD');
            })}>
              <Form.Item
                label="Tenant ID"
                name="tenantId"
                rules={[{ required: true, message: 'Enter your tenant ID.' }]}
              >
                <Input autoComplete="organization" autoFocus />
              </Form.Item>
              <Form.Item label="Login name" name="loginName" rules={[{ required: true, message: 'Enter your login name.' }]}>
                <Input autoComplete="username" />
              </Form.Item>
              <Button type="primary" htmlType="submit" block loading={busy}>Continue</Button>
              {oauthSetupProvider ? <Button type="link" block onClick={backToLogin}>Back to login</Button> : null}
            </Form>
          ) : null}

          {step === 'PASSWORD' ? (
            <Form layout="vertical" onFinish={({ password }) => {
              if (oauthSetupProvider) {
                void run(() => continueOAuthSetup(password));
              } else {
                login({ tenantId, loginName, password, redirectPath });
              }
            }}>
              {accountContext}
              <Form.Item label="Password" name="password" rules={[{ required: true, message: 'Enter your password.' }]}>
                <Input.Password autoComplete="current-password" autoFocus />
              </Form.Item>
              <Space direction="vertical" style={{ width: '100%' }}>
                <Button type="primary" htmlType="submit" block loading={oauthSetupProvider ? busy : loginPending}>
                  {oauthSetupProvider ? `Verify and set up ${oauthLabel} sign-in` : 'Sign in'}
                </Button>
                <Button type="link" block onClick={restartAccount}>Use a different account</Button>
                {oauthSetupProvider ? <Button type="link" block onClick={backToLogin}>Back to login</Button> : null}
              </Space>
            </Form>
          ) : null}

          {step === 'ACTIVATION' ? (
            <Space direction="vertical" style={{ width: '100%' }}>
              {accountContext}
              <Typography.Text>Your account needs to be set up before you can sign in.</Typography.Text>
              <Button type="primary" block loading={busy} onClick={() => run(async () => {
                await requestAccountActivationPin(accountIdentity);
                startCooldown();
                setStep('PIN');
                message.success('If the account is eligible, a verification PIN has been sent.');
              })}>Send verification PIN</Button>
              <Button type="link" block onClick={restartAccount}>Use a different account</Button>
              {oauthSetupProvider ? <Button type="link" block onClick={backToLogin}>Back to login</Button> : null}
            </Space>
          ) : null}

          {step === 'PIN' ? (
            <Form layout="vertical" onFinish={({ pin }) => run(async () => {
              await verifyAccountActivationPin(accountIdentity, String(pin));
              setStep('SET_PASSWORD');
            })}>
              {accountContext}
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
                  await requestAccountActivationPin(accountIdentity);
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
              await setInitialAccountPassword(accountIdentity, password);
              if (oauthSetupProvider) {
                await continueOAuthSetup(password);
              } else {
                setStep('COMPLETE');
              }
            })}>
              {accountContext}
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
              <Button type="primary" htmlType="submit" block loading={busy}>
                {oauthSetupProvider ? `Activate and set up ${oauthLabel} sign-in` : 'Activate account'}
              </Button>
            </Form>
          ) : null}

          {step === 'COMPLETE' ? (
            <Space direction="vertical" style={{ width: '100%' }}>
              {accountContext}
              <Alert type="success" showIcon message="Account activated" description="Your password has been set. You can now sign in." />
              <Button type="primary" block onClick={() => setStep('PASSWORD')}>Continue to sign in</Button>
            </Space>
          ) : null}
        </Space>
      </Card>
    </div>
  );
};
