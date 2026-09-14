import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { DemoEntryPage } from './DemoEntryPage.tsx';

const mocks = vi.hoisted(() => ({ loginWithDemoPersona: vi.fn() }));

vi.mock('../../api/http.ts', async () => {
  const actual = await vi.importActual<typeof import('../../api/http.ts')>('../../api/http.ts');
  return {
    ...actual,
    loginWithDemoPersona: (...args: unknown[]) => mocks.loginWithDemoPersona(...args),
  };
});

const renderPage = (path = '/demo') => render(
  <MemoryRouter initialEntries={[path]}>
    <DemoEntryPage />
  </MemoryRouter>,
);

describe('DemoEntryPage', () => {
  it('offers Employee, Manager, and HR personas', () => {
    renderPage();
    expect(screen.getByRole('button', { name: 'Try as Employee' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Try as Manager' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Try as HR' })).toBeInTheDocument();
  });

  it('enters the selected persona without credentials', async () => {
    mocks.loginWithDemoPersona.mockResolvedValueOnce(undefined);
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: 'Try as Manager' }));
    await waitFor(() => expect(mocks.loginWithDemoPersona).toHaveBeenCalledWith('manager'));
  });

  it('auto-enters a persona requested by the marketing demo link', async () => {
    mocks.loginWithDemoPersona.mockResolvedValueOnce(undefined);
    renderPage('/demo?persona=hr');
    await waitFor(() => expect(mocks.loginWithDemoPersona).toHaveBeenCalledWith('hr'));
  });
});
