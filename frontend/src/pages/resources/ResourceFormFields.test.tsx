import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { Button, Form } from 'antd';
import { describe, expect, it, vi } from 'vitest';

import type { AdminResourceConfig } from './adminResourceConfig.ts';
import { ResourceFormFields } from './ResourceFormFields.tsx';

const priorityConfig: AdminResourceConfig = {
  name: 'test-policies',
  label: 'Test policies',
  singular: 'Test policy',
  idField: 'id',
  fields: [
    {
      name: 'priority',
      label: 'Priority (higher number wins)',
      type: 'number',
      required: true,
      min: 0,
      step: 1,
      defaultValue: 10,
      description: 'Start with 10, then use 20 or 30 for more specific rules. Equal highest priorities are ambiguous.',
    },
  ],
};

const leaveTypeConfig: AdminResourceConfig = {
  name: 'leave-types',
  label: 'Leave Types',
  singular: 'Leave type',
  idField: 'id',
  fields: [
    { name: 'id', label: 'ID', required: true, readOnlyOnEdit: true, list: true },
    { name: 'name', label: 'Name', required: true, list: true },
  ],
};

describe('ResourceFormFields numeric fields', () => {
  it('renders integer number input with configured minimum, step, default, and guidance', () => {
    render(
      <Form initialValues={{ priority: 10 }}>
        <ResourceFormFields config={priorityConfig} />
      </Form>,
    );

    const priority = screen.getByRole('spinbutton', { name: /Priority \(higher number wins\)/i });
    expect(priority).toHaveValue('10');
    expect(priority).toHaveAttribute('aria-valuemin', '0');
    expect(priority).toHaveAttribute('step', '1');
    expect(screen.getByText(/Equal highest priorities are ambiguous/)).toBeInTheDocument();
  });

  it('rejects negative priority values at form validation', async () => {
    const onFinish = vi.fn();
    render(
      <Form initialValues={{ priority: -1 }} onFinish={onFinish}>
        <ResourceFormFields config={priorityConfig} />
        <Button htmlType="submit">Save</Button>
      </Form>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    await waitFor(() => expect(screen.getByText(/must be a whole number of at least 0/)).toBeInTheDocument());
    expect(onFinish).not.toHaveBeenCalled();
  });
});

describe('ResourceFormFields leave type ids', () => {
  it('hides the leave type id from tenant edit forms while keeping editable fields', () => {
    render(
      <Form initialValues={{ id: 'ANNUAL_LEAVE', name: 'Annual Leave' }}>
        <ResourceFormFields config={leaveTypeConfig} editing />
      </Form>,
    );

    expect(screen.queryByLabelText('ID')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Name')).toHaveValue('Annual Leave');
  });

  it('hides the leave type id during tenant creation', () => {
    render(
      <Form>
        <ResourceFormFields config={leaveTypeConfig} />
      </Form>,
    );

    expect(screen.queryByLabelText('ID')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Name')).toBeInTheDocument();
  });

  it('hides the leave type id from platform-admin edit forms', () => {
    render(
      <Form initialValues={{ id: 'ANNUAL_LEAVE', name: 'Annual Leave' }}>
        <ResourceFormFields config={leaveTypeConfig} editing platformAdmin />
      </Form>,
    );

    expect(screen.queryByLabelText('ID')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Name')).toHaveValue('Annual Leave');
  });
});
