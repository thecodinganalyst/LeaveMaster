import { RobotOutlined, SendOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Divider, Empty, Input, Space, Spin, Tag, Typography } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';

import { ApiError } from '../../api/http.ts';
import { AssistantMarkdown } from './AssistantMarkdown.tsx';
import { confirmAssistantAction, sendAssistantMessage, type PendingAction, type StructuredResult } from './assistantApi.ts';
import { EntitlementStructuredData, isEntitlementStructuredResult } from './EntitlementStructuredData.tsx';
import {
  actionEntries,
  actionTitle,
  canConfirmAction,
  dataEntries,
  printableValue,
  resultTitle,
  type AssistantActionItem,
  type AssistantMessageItem,
} from './assistantView.ts';

const { TextArea } = Input;

const newId = () => `${Date.now()}-${Math.random().toString(36).slice(2)}`;

const toAction = (action: PendingAction): AssistantActionItem => ({
  ...action,
  id: newId(),
  state: 'pending',
});

const errorMessage = (error: unknown) => {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return 'The assistant request failed. Please try again.';
};

const conversationIdFromError = (error: unknown) => {
  if (!(error instanceof ApiError) || !error.details || typeof error.details !== 'object') return undefined;
  if (!('conversationId' in error.details) || typeof error.details.conversationId !== 'string') return undefined;
  return error.details.conversationId.trim() || undefined;
};

const StructuredData = ({ result }: { result: StructuredResult }) => {
  if (isEntitlementStructuredResult(result)) {
    return <EntitlementStructuredData result={result} />;
  }

  const items = Array.isArray(result.data) ? result.data : [result.data];

  return (
    <Card size="small" title={resultTitle(result.toolName)} style={{ marginTop: 8 }}>
      <Space direction="vertical" size="small" style={{ width: '100%' }}>
        <Typography.Text type="secondary">Authoritative LeaveMaestro data</Typography.Text>
        {items.length === 0 ? <Typography.Text>No records found.</Typography.Text> : null}
        {items.map((item, index) => {
          const entries = dataEntries(item);
          if (entries.length === 0) {
            return <Typography.Text key={`${result.toolName}-${index}`}>{printableValue(item)}</Typography.Text>;
          }
          return (
            <Card key={`${result.toolName}-${index}`} size="small" bordered={items.length > 1}>
              <Space direction="vertical" size={4} style={{ width: '100%' }}>
                {entries.map(([key, value]) => (
                  <div key={key} style={{ display: 'flex', justifyContent: 'space-between', gap: 16 }}>
                    <Typography.Text type="secondary">{key}</Typography.Text>
                    <Typography.Text style={{ textAlign: 'right', overflowWrap: 'anywhere' }}>{printableValue(value)}</Typography.Text>
                  </div>
                ))}
              </Space>
            </Card>
          );
        })}
      </Space>
    </Card>
  );
};

const StructuredSources = ({ results }: { results: StructuredResult[] }) => {
  const [expanded, setExpanded] = useState(false);
  if (results.length === 0) return null;

  return (
    <div style={{ marginTop: 4 }}>
      <Button
        type="link"
        size="small"
        aria-expanded={expanded}
        onClick={() => setExpanded((current) => !current)}
        style={{ paddingInline: 0 }}
      >
        {expanded ? 'Hide source data' : 'View source data'}
      </Button>
      {expanded ? results.map((result, index) => (
        <StructuredData key={`${result.toolName}-${index}`} result={result} />
      )) : null}
    </div>
  );
};

interface AssistantPanelProps {
  onClose?: () => void;
}

export const AssistantPanel = ({ onClose }: AssistantPanelProps) => {
  const [messages, setMessages] = useState<AssistantMessageItem[]>([]);
  const [conversationId, setConversationId] = useState<string>();
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [requestError, setRequestError] = useState<string>();
  const [requestErrorConversationId, setRequestErrorConversationId] = useState<string>();
  const endRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [messages, sending]);

  const hasMessages = messages.length > 0;
  const canSend = useMemo(() => input.trim().length > 0 && !sending, [input, sending]);

  const send = async () => {
    const text = input.trim();
    if (!text || sending) return;

    const userMessage: AssistantMessageItem = { id: newId(), role: 'user', text };
    setMessages((current) => [...current, userMessage]);
    setInput('');
    setRequestError(undefined);
    setRequestErrorConversationId(undefined);
    setSending(true);

    try {
      const response = await sendAssistantMessage(text, conversationId);
      setConversationId(response.conversationId);
      setMessages((current) => [
        ...current,
        {
          id: newId(),
          role: 'assistant',
          text: response.message,
          actions: (response.pendingActions ?? []).map(toAction),
          results: response.structuredResults ?? [],
        },
      ]);
    } catch (error) {
      const message = errorMessage(error);
      const failedConversationId = conversationIdFromError(error);
      setRequestError(message);
      setRequestErrorConversationId(failedConversationId);
      if (failedConversationId) setConversationId(failedConversationId);
      setMessages((current) => [...current, { id: newId(), role: 'system', text: `Assistant error: ${message}` }]);
    } finally {
      setSending(false);
    }
  };

  const updateAction = (id: string, update: Partial<AssistantActionItem>) => {
    setMessages((current) =>
      current.map((message) => {
        if (!message.actions) return message;
        return {
          ...message,
          actions: message.actions.map((action) => (action.id === id ? { ...action, ...update } : action)),
        };
      }),
    );
  };

  const confirm = async (action: AssistantActionItem) => {
    if (!canConfirmAction(action) || !action.confirmationToken) return;
    updateAction(action.id, { state: 'confirming', error: '' });
    try {
      const response = await confirmAssistantAction(action.confirmationToken);
      updateAction(action.id, {
        state: 'confirmed',
        executionResult: response.result,
        replayed: response.replayed,
      });
      setMessages((current) => [
        ...current,
        {
          id: newId(),
          role: 'system',
          text: response.replayed
            ? `${actionTitle(response.toolName)} was already completed. LeaveMaestro returned the original result.`
            : `${actionTitle(response.toolName)} completed successfully.`,
        },
      ]);
    } catch (error) {
      updateAction(action.id, { state: 'failed', error: errorMessage(error) });
    }
  };

  const cancel = (action: AssistantActionItem) => {
    if (action.state !== 'pending') return;
    updateAction(action.id, { state: 'cancelled' });
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }} aria-label="Ask LeaveMaestro assistant">
      <Space style={{ justifyContent: 'space-between', width: '100%' }}>
        <Space>
          <RobotOutlined />
          <Typography.Title level={4} style={{ margin: 0 }}>Ask LeaveMaestro</Typography.Title>
        </Space>
        {onClose ? <Button type="text" onClick={onClose} aria-label="Close assistant">Close</Button> : null}
      </Space>
      <Typography.Paragraph type="secondary" style={{ marginTop: 8 }}>
        Ask about leave, staff, approvals or configuration. Application records and confirmed results remain authoritative.
      </Typography.Paragraph>
      <Divider style={{ margin: '8px 0 12px' }} />

      <div style={{ flex: 1, overflowY: 'auto', paddingRight: 4 }} aria-live="polite">
        {!hasMessages ? (
          <Empty description="Ask a question to get started" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        ) : null}

        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          {messages.map((message) => (
            <div key={message.id} style={{ alignSelf: message.role === 'user' ? 'flex-end' : 'stretch', width: '100%' }}>
              <Card size="small" bordered={message.role !== 'user'} style={{ marginLeft: message.role === 'user' ? 48 : 0, marginRight: message.role === 'user' ? 0 : 24 }}>
                <Typography.Text strong>{message.role === 'user' ? 'You' : message.role === 'assistant' ? 'LeaveMaestro' : 'Status'}</Typography.Text>
                {message.role === 'assistant' ? (
                  <AssistantMarkdown>{message.text}</AssistantMarkdown>
                ) : (
                  <Typography.Paragraph style={{ margin: '6px 0 0', whiteSpace: 'pre-wrap' }}>{message.text}</Typography.Paragraph>
                )}
              </Card>

              <StructuredSources results={message.results ?? []} />

              {message.actions?.map((action) => (
                <Card key={action.id} size="small" title={actionTitle(action.toolName)} style={{ marginTop: 8 }}>
                  <Space direction="vertical" size="small" style={{ width: '100%' }}>
                    <Alert
                      type="warning"
                      showIcon
                      message="Confirmation required"
                      description="Review these server-proposed arguments before allowing this write action."
                    />
                    {actionEntries(action).map(([key, value]) => (
                      <div key={key} style={{ display: 'flex', justifyContent: 'space-between', gap: 16 }}>
                        <Typography.Text type="secondary">{key}</Typography.Text>
                        <Typography.Text style={{ textAlign: 'right', overflowWrap: 'anywhere' }}>{printableValue(value)}</Typography.Text>
                      </div>
                    ))}
                    <Typography.Text type="secondary">Required permission: {action.requiredAuthority}</Typography.Text>
                    {action.expiresAt ? <Typography.Text type="secondary">Confirmation expires: {new Date(action.expiresAt).toLocaleString()}</Typography.Text> : null}

                    {action.state === 'confirmed' ? (
                      <Space direction="vertical" size={4}>
                        <Tag color="success">{action.replayed ? 'Already executed' : 'Confirmed'}</Tag>
                        {action.executionResult ? (
                          <Alert type="success" showIcon message="Authoritative server result" description={action.executionResult} />
                        ) : null}
                      </Space>
                    ) : null}
                    {action.state === 'cancelled' ? <Tag>Cancelled</Tag> : null}
                    {action.error ? <Alert type="error" showIcon message={action.error} description="The action was not confirmed. Ask LeaveMaestro to propose it again if needed." /> : null}

                    {action.state === 'pending' ? (
                      <Space wrap>
                        <Button
                          type="primary"
                          onClick={() => void confirm(action)}
                          disabled={!action.confirmationToken}
                          aria-label={`Confirm ${actionTitle(action.toolName)}`}
                        >
                          Confirm
                        </Button>
                        <Button onClick={() => cancel(action)} aria-label={`Cancel ${actionTitle(action.toolName)}`}>Cancel</Button>
                        {!action.confirmationToken ? (
                          <Typography.Text type="secondary">Secure confirmation is unavailable for this proposal. Ask LeaveMaestro to generate it again.</Typography.Text>
                        ) : null}
                      </Space>
                    ) : null}
                    {action.state === 'confirming' ? <Spin size="small" tip="Confirming securely..." /> : null}
                  </Space>
                </Card>
              ))}
            </div>
          ))}
          {sending ? <Spin tip="LeaveMaestro is thinking..." /> : null}
          <div ref={endRef} />
        </Space>
      </div>

      {requestError ? (
        <Alert
          type="error"
          showIcon
          message={requestError}
          description={requestErrorConversationId ? `Conversation ID: ${requestErrorConversationId}` : undefined}
          closable
          onClose={() => {
            setRequestError(undefined);
            setRequestErrorConversationId(undefined);
          }}
          style={{ marginTop: 12 }}
        />
      ) : null}
      <Divider style={{ margin: '12px 0' }} />
      <Space.Compact style={{ width: '100%' }}>
        <TextArea
          value={input}
          onChange={(event) => setInput(event.target.value)}
          autoSize={{ minRows: 1, maxRows: 4 }}
          placeholder="Ask LeaveMaestro..."
          aria-label="Message Ask LeaveMaestro"
          onKeyDown={(event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
              event.preventDefault();
              void send();
            }
          }}
        />
        <Button type="primary" icon={<SendOutlined />} disabled={!canSend} loading={sending} onClick={() => void send()} aria-label="Send message" />
      </Space.Compact>
    </div>
  );
};
