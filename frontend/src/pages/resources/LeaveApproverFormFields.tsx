import { useQuery } from '@tanstack/react-query';
import { Form, Input, Select } from 'antd';
import { useEffect } from 'react';

import { apiFetch } from '../../api/http.ts';

interface StaffOption {
  id: string;
  name?: string | null;
}

const loadStaffOptions = () => apiFetch<StaffOption[]>('/api/leave-approvers/staff-options');
const loadApproverOptions = () => apiFetch<StaffOption[]>('/api/leave-approvers/approver-options');

const toOptions = (staff: StaffOption[]) => staff.map((item) => ({
  value: item.id,
  label: item.name ? `${item.name} (${item.id})` : item.id,
}));

export const LeaveApproverFormFields = ({ editing = false }: { editing?: boolean }) => {
  const form = Form.useFormInstance();
  const staffQuery = useQuery({
    queryKey: ['leave-approver-form', 'staff-options'],
    queryFn: loadStaffOptions,
    staleTime: 60_000,
  });
  const approverQuery = useQuery({
    queryKey: ['leave-approver-form', 'approver-options'],
    queryFn: loadApproverOptions,
    staleTime: 60_000,
  });

  useEffect(() => {
    if (editing) return;
    if (!form.getFieldValue('effectiveFrom')) {
      form.setFieldValue('effectiveFrom', new Date().toISOString().slice(0, 10));
    }
  }, [editing, form]);

  const staffOptions = toOptions(staffQuery.data ?? []);
  const approverOptions = toOptions(approverQuery.data ?? []);

  return (
    <>
      <Form.Item name="staffId" label="Staff" rules={[{ required: true, message: 'Staff is required' }]}>
        <Select
          showSearch
          optionFilterProp="label"
          options={staffOptions}
          loading={staffQuery.isLoading}
          disabled={staffQuery.isError}
          placeholder={staffQuery.isError ? 'Unable to load staff' : 'Select staff'}
        />
      </Form.Item>

      <Form.Item name="approverId" label="Approver" rules={[{ required: true, message: 'Approver is required' }]}>
        <Select
          showSearch
          optionFilterProp="label"
          options={approverOptions}
          loading={approverQuery.isLoading}
          disabled={approverQuery.isError}
          placeholder={approverQuery.isError ? 'Unable to load eligible approvers' : 'Select approver'}
        />
      </Form.Item>

      <Form.Item name="effectiveFrom" label="Effective from" rules={[{ required: true, message: 'Effective from is required' }]}>
        <Input type="date" allowClear />
      </Form.Item>

      <Form.Item
        name="effectiveTo"
        label="Effective to"
        dependencies={['effectiveFrom']}
        rules={[({ getFieldValue }) => ({
          validator(_, value) {
            const effectiveFrom = getFieldValue('effectiveFrom');
            if (!value || !effectiveFrom || value > effectiveFrom) return Promise.resolve();
            return Promise.reject(new Error('Effective to must be after effective from'));
          },
        })]}
      >
        <Input type="date" allowClear />
      </Form.Item>
    </>
  );
};
