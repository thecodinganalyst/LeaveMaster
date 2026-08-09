import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { DashboardPage } from '../pages/dashboard/DashboardPage.tsx';

describe('DashboardPage', () => {
  it('renders dashboard heading', () => {
    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Operations Dashboard' })).toBeInTheDocument();
  });
});
