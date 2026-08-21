import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { PublicHolidayTable } from './PublicHolidayTable.tsx';

describe('PublicHolidayTable', () => {
  it('renders public holidays as table rows', () => {
    render(
      <PublicHolidayTable
        value={[
          { holidayDate: '2026-01-01', holidayName: "New Year's Day" },
          { holidayDate: '2026-08-09', holidayName: 'National Day' },
        ]}
      />,
    );

    expect(screen.getByRole('columnheader', { name: 'Date' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Holiday name' })).toBeInTheDocument();
    expect(screen.getByText('2026-01-01')).toBeInTheDocument();
    expect(screen.getByText("New Year's Day")).toBeInTheDocument();
    expect(screen.getByText('2026-08-09')).toBeInTheDocument();
    expect(screen.getByText('National Day')).toBeInTheDocument();
    expect(screen.queryByText(/holidayDate/)).not.toBeInTheDocument();
  });

  it('renders a clear empty state for missing holidays', () => {
    render(<PublicHolidayTable value={[]} />);
    expect(screen.getByText('No public holidays')).toBeInTheDocument();
  });

  it('handles null or malformed values as an empty holiday list', () => {
    const { rerender } = render(<PublicHolidayTable value={null} />);
    expect(screen.getByText('No public holidays')).toBeInTheDocument();

    rerender(<PublicHolidayTable value={{ holidayDate: '2026-01-01' }} />);
    expect(screen.getByText('No public holidays')).toBeInTheDocument();
  });
});
