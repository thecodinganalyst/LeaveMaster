import { render, screen } from '@testing-library/react';
import { Form } from 'antd';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { describe, expect, it, vi } from 'vitest';

import { StaffFormFields } from './StaffFormFields.tsx';
import { DEFAULT_STAFF_WORK_SCHEDULE } from './staffFormHelpers.ts';

vi.mock('../../api/http.ts', () => ({
  apiFetch: vi.fn((path: string) => {
    if (path === '/api/leave-calendars') return Promise.resolve([]);
    if (path === '/api/jurisdictions') return Promise.resolve([]);
    if (path === '/api/leave-types') return Promise.resolve([]);
    return Promise.resolve([]);
  }),
}));

describe('Staff work schedule layout', () => {
  it('renders each day with its selector in the same horizontal row', () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

    render(
      <QueryClientProvider client={queryClient}>
        <Form initialValues={{ workSchedule: DEFAULT_STAFF_WORK_SCHEDULE }}>
          <StaffFormFields />
        </Form>
      </QueryClientProvider>,
    );

    const mondaySelect = screen.getByLabelText('Monday work schedule');
    const mondayLabel = screen.getByText('Monday');
    expect(mondayLabel.parentElement).toBe(mondaySelect.closest('.ant-select')?.parentElement);
  });
});
