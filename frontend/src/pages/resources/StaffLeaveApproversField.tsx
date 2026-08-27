import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Button, Card, Divider, Form, Grid, Input, Select, Space, Table, Typography } from 'antd';
import { useEffect } from 'react';

import { apiFetch } from '../../api/http.ts';

interface StaffOption {
  id: string;
  name?: string | null;
}

interface LeaveApproverRecord {
  id: string;
  approver?: StaffOption | null;
  effectiveFrom: string;
  effectiveTo?: string | null;
}

const loadApproverOptions = () => apiFetch<StaffOption[]>('/api/leave-approvers/approver-options');
const loadStaffApprovers = (staffId: string) => apiFetch<LeaveApproverRecord[]>(`/api/leave-approvers/staff/${encodeURIComponent(staffId)}`);
const today = () => {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const toOptions = (staff: StaffOption[]) => staff.map((item) => ({
  value: item.id,
  label: item.name ? `${item.name} (${item.id})` : item.id,
}));

interface Props {
  editing?: boolean;
  staffId?: string;
}

export const StaffLeaveApproversField = ({ editing = false, staffId }: Props) => {
  const form = Form.useFormInstance();
  const screens = Grid.useBreakpoint();
  const approverQuery = useQuery({
    queryKey: ['staff-leave-approvers', 'approver-options'],
    queryFn: loadApproverOptions,
    staleTime: 60_000,
  });
  const assignmentsQuery = useQuery({
    queryKey: ['staff-leave-approvers', 'assignments', staffId],
    queryFn: () => loadStaffApprovers(staffId!),
    enabled: Boolean(editing && staffId),
  });

  useEffect(() => {
    if (!editing || !assignmentsQuery.data || form.getFieldValue('leaveApprovers') !== undefined) return;
    form.setFieldValue('leaveApprovers', assignmentsQuery.data.map((record) => ({
      id: record.id,
      approverId: record.approver?.id,
      effectiveFrom: record.effectiveFrom,
      effectiveTo: record.effectiveTo ?? undefined,
    })));
  }, [assignmentsQuery.data, editing, form]);

  const approverOptions = toOptions(approverQuery.data ?? []);
  const validationRules = (index: number) => [({ getFieldValue }: { getFieldValue: (name: unknown) => unknown }) => ({
    validator(_: unknown, value?: string) {
      const start = (getFieldValue('leaveApprovers') as Array<{ effectiveFrom?: string }> | undefined)?.[index]?.effectiveFrom;
      if (!value || !start || value > start) return Promise.resolve();
      return Promise.reject(new Error('Effective end date must be after effective start date'));
    },
  })];

  return (
    <>
      <Divider orientation="left">Leave Approvers</Divider>
      <Typography.Paragraph type="secondary">
        Add current, future, or historical approver assignments. Changes are saved together with the staff record.
      </Typography.Paragraph>
      <Form.List name="leaveApprovers">
        {(fields, { add, remove }) => (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            {screens.md ? (
              <Table
                pagination={false}
                rowKey="key"
                dataSource={fields}
                locale={{ emptyText: 'No leave approver assignments' }}
                columns={[
                  {
                    title: 'Approver',
                    key: 'approverId',
                    render: (_, field) => (
                      <>
                        <Form.Item name={[field.name, 'id']} hidden><Input /></Form.Item>
                        <Form.Item name={[field.name, 'approverId']} rules={[{ required: true, message: 'Approver is required' }]} style={{ marginBottom: 0 }}>
                          <Select
                            showSearch
                            optionFilterProp="label"
                            options={approverOptions}
                            loading={approverQuery.isLoading}
                            disabled={approverQuery.isError}
                            placeholder={approverQuery.isError ? 'Unable to load approvers' : 'Select approver'}
                          />
                        </Form.Item>
                      </>
                    ),
                  },
                  {
                    title: 'Effective Start Date',
                    key: 'effectiveFrom',
                    render: (_, field) => (
                      <Form.Item name={[field.name, 'effectiveFrom']} rules={[{ required: true, message: 'Start date is required' }]} style={{ marginBottom: 0 }}>
                        <Input type="date" allowClear />
                      </Form.Item>
                    ),
                  },
                  {
                    title: 'Effective End Date',
                    key: 'effectiveTo',
                    render: (_, field) => (
                      <Form.Item name={[field.name, 'effectiveTo']} dependencies={[['leaveApprovers', field.name, 'effectiveFrom']]} rules={validationRules(field.name)} style={{ marginBottom: 0 }}>
                        <Input type="date" allowClear />
                      </Form.Item>
                    ),
                  },
                  {
                    title: 'Actions',
                    key: 'actions',
                    width: 90,
                    render: (_, field) => <Button type="text" danger icon={<DeleteOutlined />} onClick={() => remove(field.name)}>Remove</Button>,
                  },
                ]}
              />
            ) : (
              fields.map((field) => (
                <Card key={field.key} size="small">
                  <Form.Item name={[field.name, 'id']} hidden><Input /></Form.Item>
                  <Form.Item name={[field.name, 'approverId']} label="Approver" rules={[{ required: true, message: 'Approver is required' }]}>
                    <Select
                      showSearch
                      optionFilterProp="label"
                      options={approverOptions}
                      loading={approverQuery.isLoading}
                      disabled={approverQuery.isError}
                      placeholder={approverQuery.isError ? 'Unable to load approvers' : 'Select approver'}
                    />
                  </Form.Item>
                  <Form.Item name={[field.name, 'effectiveFrom']} label="Effective Start Date" rules={[{ required: true, message: 'Start date is required' }]}>
                    <Input type="date" allowClear />
                  </Form.Item>
                  <Form.Item name={[field.name, 'effectiveTo']} label="Effective End Date" dependencies={[['leaveApprovers', field.name, 'effectiveFrom']]} rules={validationRules(field.name)}>
                    <Input type="date" allowClear />
                  </Form.Item>
                  <Button danger icon={<DeleteOutlined />} onClick={() => remove(field.name)}>Remove</Button>
                </Card>
              ))
            )}
            <Button type="dashed" icon={<PlusOutlined />} onClick={() => add({ effectiveFrom: today() })}>
              Add Leave Approver
            </Button>
          </Space>
        )}
      </Form.List>
    </>
  );
};
