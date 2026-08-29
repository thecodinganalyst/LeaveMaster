import { act, render, screen } from '@testing-library/react';
import { Form, type FormInstance } from 'antd';
import { describe, expect, it } from 'vitest';

import { WorkScheduleEditor } from './StaffCreationFields.tsx';
import {
  DEFAULT_STAFF_WORK_SCHEDULE,
  updateWorkSchedule,
  type WorkScheduleDayValue,
} from './staffFormHelpers.ts';

const selectedLabel = (day: string) => {
  const selectInput = screen.getByLabelText(`${day} work schedule`);
  return selectInput.closest('.ant-select')?.querySelector('.ant-select-selection-item')?.textContent;
};

const renderEditor = () => {
  let formInstance: FormInstance | undefined;

  const Harness = () => {
    const [form] = Form.useForm();
    formInstance = form;
    return (
      <Form
        form={form}
        initialValues={{
          workSchedule: DEFAULT_STAFF_WORK_SCHEDULE.map((entry) => ({ ...entry })),
        }}
      >
        <WorkScheduleEditor />
      </Form>
    );
  };

  render(<Harness />);
  if (!formInstance) throw new Error('Form instance was not initialized');
  return formInstance;
};

describe('WorkScheduleEditor', () => {
  it('shows full days on weekdays and not working on weekends by default', () => {
    renderEditor();

    expect(selectedLabel('Monday')).toBe('Full day');
    expect(selectedLabel('Tuesday')).toBe('Full day');
    expect(selectedLabel('Wednesday')).toBe('Full day');
    expect(selectedLabel('Thursday')).toBe('Full day');
    expect(selectedLabel('Friday')).toBe('Full day');
    expect(selectedLabel('Saturday')).toBe('Not working');
    expect(selectedLabel('Sunday')).toBe('Not working');
  });

  it('reacts to work schedule changes and keeps the changed value in form state', () => {
    const form = renderEditor();
    const initialSchedule = form.getFieldValue('workSchedule') as WorkScheduleDayValue[];

    act(() => {
      form.setFieldValue('workSchedule', updateWorkSchedule(initialSchedule, 'SATURDAY', 'FULL'));
    });

    expect(selectedLabel('Saturday')).toBe('Full day');
    expect(form.getFieldValue('workSchedule')).toEqual([
      ...DEFAULT_STAFF_WORK_SCHEDULE.map((entry) => ({ ...entry })),
      { dayOfWeek: 'SATURDAY', daySchedule: 'FULL' },
    ]);

    act(() => {
      const schedule = form.getFieldValue('workSchedule') as WorkScheduleDayValue[];
      form.setFieldValue('workSchedule', updateWorkSchedule(schedule, 'MONDAY', 'NONE'));
    });

    expect(selectedLabel('Monday')).toBe('Not working');
    expect((form.getFieldValue('workSchedule') as WorkScheduleDayValue[]).some((entry) => entry.dayOfWeek === 'MONDAY')).toBe(false);
  });
});
