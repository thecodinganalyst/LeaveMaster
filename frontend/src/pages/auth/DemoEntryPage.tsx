import { useEffect, useMemo, useState } from 'react';
import { Button, Card, Col, Row, Space, Typography, Alert } from 'antd';
import { useNavigate, useSearchParams } from 'react-router-dom';

import { type DemoPersona, loginWithDemoPersona } from '../../api/http.ts';

const personaContent: Record<DemoPersona, { title: string; description: string; destination: string }> = {
  employee: {
    title: 'Try as Employee',
    description: 'Apply for leave and review your personal balance and leave history.',
    destination: '/leave-requests',
  },
  manager: {
    title: 'Try as Manager',
    description: 'Review team leave and approve or reject requests using the configured approver rules.',
    destination: '/approvals',
  },
  hr: {
    title: 'Try as HR',
    description: 'Explore staff, leave types, calendars, approvers, users, and other tenant HR administration.',
    destination: '/employees',
  },
};

const isPersona = (value: string | null): value is DemoPersona =>
  value === 'employee' || value === 'manager' || value === 'hr';

export const DemoEntryPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const requestedPersona = useMemo(() => {
    const value = searchParams.get('persona');
    return isPersona(value) ? value : undefined;
  }, [searchParams]);
  const [loading, setLoading] = useState<DemoPersona | undefined>();
  const [error, setError] = useState<string>();
  const [autoStarted, setAutoStarted] = useState(false);

  const enterDemo = async (persona: DemoPersona) => {
    setError(undefined);
    setLoading(persona);
    try {
      await loginWithDemoPersona(persona);
      navigate(personaContent[persona].destination, { replace: true });
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to enter the public demo right now.');
      setLoading(undefined);
    }
  };

  useEffect(() => {
    if (requestedPersona && !autoStarted) {
      setAutoStarted(true);
      void enterDemo(requestedPersona);
    }
  }, [requestedPersona, autoStarted]);

  return (
    <main style={{ maxWidth: 1080, margin: '0 auto', padding: '48px 20px' }}>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <div>
          <Typography.Text strong style={{ color: '#0f766e' }}>PUBLIC DEMO</Typography.Text>
          <Typography.Title level={1}>Explore LeaveMaestro without signing up</Typography.Title>
          <Typography.Paragraph style={{ fontSize: 18 }}>
            Choose a fictional demo persona. The demo tenant is isolated from customer tenants and is reset periodically.
          </Typography.Paragraph>
        </div>

        <Alert
          type="warning"
          showIcon
          message="Demo data only"
          description="Do not enter real employee or confidential information. Changes made in this environment may be reset without notice."
        />

        {error ? <Alert type="error" showIcon message={error} /> : null}

        <Row gutter={[20, 20]}>
          {(Object.entries(personaContent) as Array<[DemoPersona, (typeof personaContent)[DemoPersona]]>).map(([persona, content]) => (
            <Col xs={24} md={8} key={persona}>
              <Card title={content.title} style={{ height: '100%' }}>
                <Typography.Paragraph>{content.description}</Typography.Paragraph>
                <Button
                  type="primary"
                  block
                  loading={loading === persona}
                  disabled={Boolean(loading && loading !== persona)}
                  onClick={() => void enterDemo(persona)}
                >
                  {content.title}
                </Button>
              </Card>
            </Col>
          ))}
        </Row>

        <Space wrap>
          <Button href="/login">Use a normal account</Button>
          <Button href="https://leavemaestro.com">Return to LeaveMaestro.com</Button>
        </Space>
      </Space>
    </main>
  );
};
