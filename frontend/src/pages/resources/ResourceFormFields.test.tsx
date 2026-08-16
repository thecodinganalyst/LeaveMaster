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

describe('ResourceFormFields numeric fields', () => {
  it('renders integer number input with configured minimum, step, default, and guidance', () => {
    render(
      <Form initialValues={{ priority: 10 }}>
        <ResourceFormFields config={priorityConfig} />
      </Form>,
    );

    const priority = screen.getByRole('spinbutton', { name: /Priority \(higher number wins\)/i });
    expect(priority).toHaveValue('10');
    expect(priority).toHaveAttribute('min', '0');
    expect(priority).toHaveAttribute('step', '1');
    expect(screen.getByText(/Equal highest priorities are ambiguous/)).toBeInTheDocument();
  });

  it('rejects negative priority values at form validation', async () => {
    const onFinish = vi.fn();
    render(
      <Form onFinish={onFinish}>
        <ResourceFormFields config={priorityConfig} />
        <Button htmlType="submit">Save</Button>
      </Form>,
    );

    fireEvent.change(screen.getByRole('spinbutton', { name: /Priority \(higher number wins\)/i }), { target: { value: '-1' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    await waitFor(() => expect(screen.getByText(/must be a whole number of at least 0/)).toBeInTheDocument());
    expect(onFinish).not.toHaveBeenCalled();
  });
});
