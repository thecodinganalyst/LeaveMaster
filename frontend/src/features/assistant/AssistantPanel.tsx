import { RobotOutlined, SendOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Divider, Empty, Input, Space, Spin, Tag, Typography } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';

import { ApiError } from '../../api/http.ts';
import { confirmAssistantAction, sendAssistantMessage, type PendingAction, type StructuredResult } from './assistantApi.ts';
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

const StructuredData = ({ result }: { result: StructuredResult }) => {
  const items = Array.isArray(result.data) ? result.data : [result.data];

  return (
    <Card size="small" title={resultTitle(result.toolName)} style={{ marginTop: 8 }}>
      <Space direction="vertical" size="small" style={{ width: '100%' }}>
        <Typography.Text type="secondary">Authoritative LeaveMaster data</Typography.Text>
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

interface AssistantPanelProps {
  onClose?: () => void;
}

export const AssistantPanel = ({ onClose }: AssistantPanelProps) => {
  const [messages, setMessages] = useState<AssistantMessageItem[]>([]);
  const [conversationId, setConversationId] = useState<string>();
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [requestError, setRequestError] = useState<string>();
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
      setRequestError(message);
      setMessages((current) => [...current, { id: newId(), role: 'system', text: `Assistant error: ${message}` }]);
    } finally {
      setSending(false);
    }
  };

  const updateAction = (id: string, update: Partial<AssistantActionItem>) => {
    setMessages((current) =>
      current.map((message) => ({
        ...message,
        actions: message.actions?.map((action) => (action.id === id ? { ...action, ...update } : action)),
      })),
    );
  };

  const confirm = async (action: AssistantActionItem) => {
    if (!canConfirmAction(action) || !action.confirmationToken) return;
    updateAction(action.id, { state: 'confirming', error: undefined });
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
            ? `${actionTitle(response.toolName)} was already completed. LeaveMaster returned the original result.`
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
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }} aria-label="Ask LeaveMaster assistant">
      <Space style={{ justifyContent: 'space-between', width: '100%' }}>
        <Space>
          <RobotOutlined />
          <Typography.Title level={4} style={{ margin: 0 }}>Ask LeaveMaster</Typography.Title>
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
                <Typography.Text strong>{message.role === 'user' ? 'You' : message.role === 'assistant' ? 'LeaveMaster' : 'Status'}</Typography.Text>
                <Typography.Paragraph style={{ margin: '6px 0 0', whiteSpace: 'pre-wrap' }}>{message.text}</Typography.Paragraph>
              </Card>

              {message.results?.map((result, index) => (
                <StructuredData key={`${message.id}-${result.toolName}-${index}`} result={result} />
              ))}

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
                    {action.error ? <Alert type="error" showIcon message={action.error} description="The action was not confirmed. Ask LeaveMaster to propose it again if needed." /> : null}

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
                          <Typography.Text type="secondary">Secure confirmation is unavailable for this proposal. Ask LeaveMaster to generate it again.</Typography.Text>
                        ) : null}
                      </Space>
                    ) : null}
                    {action.state === 'confirming' ? <Spin size="small" tip="Confirming securely..." /> : null}
                  </Space>
                </Card>
              ))}
            </div>
          ))}
          {sending ? <Spin tip="LeaveMaster is thinking..." /> : null}
          <div ref={endRef} />
        </Space>
      </div>

      {requestError ? <Alert type="error" showIcon message={requestError} closable onClose={() => setRequestError(undefined)} style={{ marginTop: 12 }} /> : null}
      <Divider style={{ margin: '12px 0' }} />
      <Space.Compact style={{ width: '100%' }}>
        <TextArea
          value={input}
          onChange={(event) => setInput(event.target.value)}
          autoSize={{ minRows: 1, maxRows: 4 }}
          placeholder="Ask LeaveMaster..."
          aria-label="Message Ask LeaveMaster"
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
