import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Card, Descriptions, Empty, Flex, Input, Select, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';

import { apiFetch } from '../../api/http.ts';
import { getCurrentUser } from '../../auth/session.ts';

type Status = 'NEW' | 'READ' | 'REPLIED' | 'CLOSED';

type Reply = {
  id: string;
  replyBody: string;
  repliedBy: string;
  createdAt: string;
};

type Enquiry = {
  id: string;
  name: string;
  company: string;
  email: string;
  phone?: string | null;
  companySize?: string | null;
  country?: string | null;
  enquiryType: string;
  message: string;
  status: Status;
  createdAt: string;
  firstReadAt?: string | null;
  replies: Reply[];
};

const statusColor: Record<Status, string> = {
  NEW: 'blue',
  READ: 'gold',
  REPLIED: 'green',
  CLOSED: 'default',
};

export function ContactEnquiriesPage() {
  const [items, setItems] = useState<Enquiry[]>([]);
  const [selected, setSelected] = useState<Enquiry | null>(null);
  const [status, setStatus] = useState<Status | undefined>();
  const [replyBody, setReplyBody] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [platformAdmin, setPlatformAdmin] = useState<boolean | null>(null);

  const load = async (nextStatus = status) => {
    setLoading(true);
    setError(null);
    try {
      const user = await getCurrentUser();
      setPlatformAdmin(Boolean(user.platformAdmin));
      if (!user.platformAdmin) return;
      const query = nextStatus ? `?status=${nextStatus}` : '';
      setItems(await apiFetch<Enquiry[]>(`/api/platform/contact-enquiries${query}`));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unable to load contact enquiries.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const open = async (id: string) => {
    setError(null);
    try {
      const enquiry = await apiFetch<Enquiry>(`/api/platform/contact-enquiries/${id}`);
      setSelected(enquiry);
      setItems((current) => current.map((item) => (item.id === id ? enquiry : item)));
      setReplyBody('');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unable to open the enquiry.');
    }
  };

  const sendReply = async () => {
    if (!selected || !replyBody.trim()) {
      setError('Reply body is required.');
      return;
    }
    setSending(true);
    setError(null);
    try {
      const updated = await apiFetch<Enquiry>(`/api/platform/contact-enquiries/${selected.id}/reply`, {
        method: 'POST',
        body: JSON.stringify({ body: replyBody.trim() }),
      });
      setSelected(updated);
      setItems((current) => current.map((item) => (item.id === updated.id ? updated : item)));
      setReplyBody('');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unable to send the reply.');
    } finally {
      setSending(false);
    }
  };

  const columns = useMemo<ColumnsType<Enquiry>>(() => [
    {
      title: 'Status',
      dataIndex: 'status',
      width: 110,
      render: (value: Status) => <Tag color={statusColor[value]}>{value}</Tag>,
    },
    {
      title: 'Received',
      dataIndex: 'createdAt',
      width: 180,
      render: (value: string) => new Date(value).toLocaleString(),
    },
    { title: 'Name', dataIndex: 'name' },
    { title: 'Company', dataIndex: 'company' },
    { title: 'Email', dataIndex: 'email' },
    { title: 'Type', dataIndex: 'enquiryType', render: (value: string) => value.replaceAll('_', ' ') },
    {
      title: 'Message',
      dataIndex: 'message',
      render: (value: string) => value.length > 90 ? `${value.slice(0, 90)}…` : value,
    },
    {
      title: '',
      key: 'action',
      width: 90,
      render: (_, item) => <Button type="link" onClick={() => void open(item.id)}>Open</Button>,
    },
  ], []);

  if (platformAdmin === false) {
    return <Alert type="error" showIcon message="Platform administrator access required" description="Contact enquiries are private platform-level data." />;
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Flex justify="space-between" align="center" wrap="wrap" gap={12}>
        <div>
          <Typography.Title level={2} style={{ marginBottom: 4 }}>Contact Enquiries</Typography.Title>
          <Typography.Text type="secondary">Messages submitted through the LeaveMaestro marketing site.</Typography.Text>
        </div>
        <Select
          allowClear
          placeholder="All statuses"
          style={{ minWidth: 180 }}
          value={status}
          options={(['NEW', 'READ', 'REPLIED', 'CLOSED'] as Status[]).map((value) => ({ value, label: value }))}
          onChange={(value) => {
            setStatus(value);
            setSelected(null);
            void load(value);
          }}
        />
      </Flex>

      {error ? <Alert type="error" showIcon message={error} closable onClose={() => setError(null)} /> : null}

      <Card>
        <Table<Enquiry>
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={items}
          pagination={{ pageSize: 20, showSizeChanger: false }}
          rowClassName={(record) => record.status === 'NEW' ? 'contact-enquiry-unread' : ''}
          scroll={{ x: 900 }}
        />
      </Card>

      {selected ? (
        <Card title={<Space><span>{selected.name}</span><Tag color={statusColor[selected.status]}>{selected.status}</Tag></Space>}>
          <Descriptions bordered column={{ xs: 1, sm: 2 }} size="small">
            <Descriptions.Item label="Company">{selected.company}</Descriptions.Item>
            <Descriptions.Item label="Email">{selected.email}</Descriptions.Item>
            <Descriptions.Item label="Phone">{selected.phone || '—'}</Descriptions.Item>
            <Descriptions.Item label="Company size">{selected.companySize || '—'}</Descriptions.Item>
            <Descriptions.Item label="Country">{selected.country || '—'}</Descriptions.Item>
            <Descriptions.Item label="Enquiry type">{selected.enquiryType.replaceAll('_', ' ')}</Descriptions.Item>
            <Descriptions.Item label="Received">{new Date(selected.createdAt).toLocaleString()}</Descriptions.Item>
            <Descriptions.Item label="First read">{selected.firstReadAt ? new Date(selected.firstReadAt).toLocaleString() : '—'}</Descriptions.Item>
          </Descriptions>

          <Typography.Title level={4} style={{ marginTop: 24 }}>Original message</Typography.Title>
          <Typography.Paragraph style={{ whiteSpace: 'pre-wrap' }}>{selected.message}</Typography.Paragraph>

          <Typography.Title level={4} style={{ marginTop: 24 }}>Conversation</Typography.Title>
          {selected.replies.length ? selected.replies.map((reply) => (
            <Card key={reply.id} size="small" style={{ marginBottom: 12 }}>
              <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 8 }}>{reply.replyBody}</Typography.Paragraph>
              <Typography.Text type="secondary">Sent by {reply.repliedBy} · {new Date(reply.createdAt).toLocaleString()}</Typography.Text>
            </Card>
          )) : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No replies sent yet" />}

          <Typography.Title level={4} style={{ marginTop: 24 }}>Reply by email</Typography.Title>
          <Input.TextArea
            rows={6}
            maxLength={4000}
            showCount
            value={replyBody}
            placeholder="Write a reply to the person who submitted this enquiry."
            onChange={(event) => setReplyBody(event.target.value)}
          />
          <Button type="primary" loading={sending} disabled={!replyBody.trim()} onClick={() => void sendReply()} style={{ marginTop: 12 }}>
            Send email reply
          </Button>
        </Card>
      ) : null}
    </Space>
  );
}
