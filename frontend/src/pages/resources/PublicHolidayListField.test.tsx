import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { Button, Form } from 'antd';
import { describe, expect, it, vi } from 'vitest';

import { PublicHolidayListField } from './PublicHolidayListField.tsx';

const renderField = (initialValues?: Record<string, unknown>, onFinish = vi.fn()) => {
  render(
    <Form initialValues={initialValues} onFinish={onFinish}>
      <PublicHolidayListField name="publicHolidays" label="Public holidays" />
      <Button htmlType="submit">Save</Button>
    </Form>,
  );
  return onFinish;
};

describe('PublicHolidayListField', () => {
  it('loads existing public holidays as editable rows', () => {
    renderField({
      publicHolidays: [
        { holidayDate: '2026-01-01', holidayName: "New Year's Day" },
        { holidayDate: '2026-08-09', holidayName: 'National Day' },
      ],
    });

    expect(screen.getAllByLabelText('Holiday date')).toHaveLength(2);
    expect(screen.getAllByLabelText('Holiday name')).toHaveLength(2);
    expect(screen.getByDisplayValue('2026-01-01')).toBeInTheDocument();
    expect(screen.getByDisplayValue("New Year's Day")).toBeInTheDocument();
  });

  it('adds and removes holiday rows', () => {
    renderField();

    fireEvent.click(screen.getByRole('button', { name: 'Add holiday' }));
    expect(screen.getByLabelText('Holiday date')).toBeInTheDocument();
    expect(screen.getByLabelText('Holiday name')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Remove' }));
    expect(screen.queryByLabelText('Holiday date')).not.toBeInTheDocument();
  });

  it('requires a date and name for each holiday', async () => {
    const onFinish = renderField();
    fireEvent.click(screen.getByRole('button', { name: 'Add holiday' }));
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    await waitFor(() => {
      expect(screen.getByText('Holiday date is required')).toBeInTheDocument();
      expect(screen.getByText('Holiday name is required')).toBeInTheDocument();
    });
    expect(onFinish).not.toHaveBeenCalled();
  });

  it('rejects duplicate holiday dates', async () => {
    const onFinish = renderField({
      publicHolidays: [
        { holidayDate: '2026-01-01', holidayName: "New Year's Day" },
        { holidayDate: '2026-01-01', holidayName: 'Duplicate holiday' },
      ],
    });

    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    await waitFor(() => expect(screen.getByText('Public holiday dates must be unique')).toBeInTheDocument());
    expect(onFinish).not.toHaveBeenCalled();
  });
});
