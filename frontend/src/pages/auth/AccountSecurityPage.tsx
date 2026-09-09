import { GithubOutlined, GoogleOutlined, MailOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { useGetIdentity } from '@refinedev/core';
import { Alert, Button, Card, Form, Input, Space, Spin, Tag, Typography, message } from 'antd';
import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import { ApiError } from '../../api/http.ts';
import { getOAuthLinkStatus, startOAuthLink, type OAuthLinkStatus, type OAuthProvider } from '../../api/oauth.ts';
import { updatePlatformAdminRecoveryEmail } from '../../api/platformAdmin.ts';

interface LeaveMasterIdentity {
  platformAdmin?: boolean;
}

const providerLabel = (provider: string | null | undefined) => provider === 'google'
  ? 'Google'
  : provider === 'github'
    ? 'GitHub'
    : 'OAuth';

const linkingErrorMessage = (code: string | null) => {
  switch (code) {
    case 'identity_in_use':
      return 'That Google or GitHub account is already linked to another LeaveMaestro account.';
    case 'already_linked':
      return 'This LeaveMaestro account already has an OAuth provider linked.';
    case 'account_inactive':
    case 'account_not_eligible':
      return 'This LeaveMaestro account is not eligible for OAuth sign-in.';
    case 'link_context_invalid':
      return 'The OAuth setup session expired or was invalid. Please start the setup again.';
    case 'access_denied':
      return 'OAuth authorization was cancelled or denied. You can try again.';
    case 'unsupported_provider':
      return 'That OAuth provider is not supported.';
    default:
      return code ? 'OAuth sign-in setup could not be completed. Please try again.' : undefined;
  }
};

export const AccountSecurityPage = () => {
  const [searchParams] = useSearchParams();
  const { data: identity } = useGetIdentity<LeaveMasterIdentity>();
  const [status, setStatus] = useState<OAuthLinkStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [startingProvider, setStartingProvider] = useState<OAuthProvider | null>(null);
  const [savingRecoveryEmail, setSavingRecoveryEmail] = useState(false);
  const [error, setError] = useState<string | undefined>(() => linkingErrorMessage(searchParams.get('oauthError')));

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getOAuthLinkStatus()
      .then((result) => {
        if (!cancelled) setStatus(result);
      })
      .catch((caught) => {
        if (!cancelled) {
          setError(caught instanceof ApiError || caught instanceof Error
            ? caught.message
            : 'Unable to load sign-in methods.');
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const startLink = async (provider: OAuthProvider) => {
    setStartingProvider(provider);
    setError(undefined);
    try {
      await startOAuthLink(provider);
    } catch (caught) {
      setError(caught instanceof ApiError || caught instanceof Error
        ? caught.message
        : 'Unable to start OAuth sign-in setup.');
      setStartingProvider(null);
    }
  };

  const saveRecoveryEmail = async ({ email }: { email: string }) => {
    setSavingRecoveryEmail(true);
    setError(undefined);
    try {
      await updatePlatformAdminRecoveryEmail(email.trim());
      message.success('Recovery email updated.');
    } catch (caught) {
      setError(caught instanceof ApiError || caught instanceof Error
        ? caught.message
        : 'Unable to update the recovery email.');
    } finally {
      setSavingRecoveryEmail(false);
    }
  };

  const linkedProvider = status?.linked ? providerLabel(status.provider) : null;
  const linkedSuccess = searchParams.get('oauthLinked') === 'true' && linkedProvider;

  return (
    <Space direction="vertical" size={20} style={{ width: '100%', maxWidth: 720 }}>
      <div>
        <Typography.Title level={2} style={{ marginBottom: 4 }}>Security</Typography.Title>
        <Typography.Text type="secondary">Manage how you sign in to your LeaveMaestro account.</Typography.Text>
      </div>

      {linkedSuccess ? <Alert type="success" showIcon message={`${linkedProvider} sign-in is now connected.`} /> : null}
      {error ? <Alert type="error" showIcon message={error} /> : null}

      {identity?.platformAdmin ? (
        <Card title={<Space><MailOutlined /><span>Recovery email</span></Space>}>
          <Space direction="vertical" size={14} style={{ width: '100%' }}>
            <Typography.Text>
              Add an email address so the PlatformAdmin account can use the Forgot password flow. If no recovery email is configured, the Secret Manager break-glass recovery procedure remains available.
            </Typography.Text>
            <Form layout="vertical" onFinish={saveRecoveryEmail}>
              <Form.Item
                label="Recovery email"
                name="email"
                rules={[
                  { required: true, message: 'Enter a recovery email address.' },
                  { type: 'email', message: 'Enter a valid recovery email address.' },
                ]}
              >
                <Input type="email" autoComplete="email" placeholder="admin@example.com" />
              </Form.Item>
              <Button type="primary" htmlType="submit" loading={savingRecoveryEmail}>Save recovery email</Button>
            </Form>
          </Space>
        </Card>
      ) : null}

      <Card title={<Space><SafetyCertificateOutlined /><span>Sign-in methods</span></Space>}>
        {loading ? (
          <Space><Spin size="small" /><Typography.Text>Loading sign-in methods…</Typography.Text></Space>
        ) : status?.linked ? (
          <Space direction="vertical" size={12}>
            <Typography.Text>Your LeaveMaestro account is connected to:</Typography.Text>
            <Tag color="green">{linkedProvider} connected</Tag>
            <Typography.Text type="secondary">
              You can use {linkedProvider} from the LeaveMaestro login page. One OAuth provider can be linked to an account at a time.
            </Typography.Text>
          </Space>
        ) : (
          <Space direction="vertical" size={14} style={{ width: '100%' }}>
            <Typography.Text>
              Connect one provider to use it as an alternative to your LeaveMaestro password when signing in.
            </Typography.Text>
            <Button
              icon={<GoogleOutlined />}
              block
              loading={startingProvider === 'google'}
              disabled={startingProvider !== null && startingProvider !== 'google'}
              onClick={() => void startLink('google')}
            >
              Set up Google sign-in
            </Button>
            <Button
              icon={<GithubOutlined />}
              block
              loading={startingProvider === 'github'}
              disabled={startingProvider !== null && startingProvider !== 'github'}
              onClick={() => void startLink('github')}
            >
              Set up GitHub sign-in
            </Button>
          </Space>
        )}
      </Card>
    </Space>
  );
};
