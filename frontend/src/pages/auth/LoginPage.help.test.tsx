import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { LoginPage } from './LoginPage.tsx';

const lookupAccountActivation = vi.fn();

vi.mock('@refinedev/core', () => ({ useLogin: () => ({ mutate: vi.fn(), isPending: false }) }));
vi.mock('../../api/accountActivation.ts', () => ({
  lookupAccountActivation: (...args: unknown[]) => lookupAccountActivation(...args),
  requestAccountActivationPin: vi.fn(),
  verifyAccountActivationPin: vi.fn(),
  setInitialAccountPassword: vi.fn(),
}));
vi.mock('../../api/oauth.ts', () => ({
  getRememberedOAuthProvider: () => undefined,
  startOAuthLogin: vi.fn(),
}));

const renderPage = () => render(<MemoryRouter><LoginPage /></MemoryRouter>);

describe('LoginPage help links', () => {
  it('opens Getting Started help externally without using SPA routing', () => {
    renderPage();
    const link = screen.getByRole('link', { name: 'Help signing in' });
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', expect.stringContaining('noopener'));
    expect(link).toHaveAttribute('href', expect.stringContaining('/user-guide/getting-started/'));
  });

  it('switches to account activation help for newly provisioned accounts', async () => {
    lookupAccountActivation.mockResolvedValueOnce({ nextStep: 'ACTIVATION' });
    renderPage();
    fireEvent.change(screen.getByLabelText('Tenant ID'), { target: { value: 'Demo' } });
    fireEvent.change(screen.getByLabelText('Login name'), { target: { value: 'demo-user' } });
    fireEvent.click(screen.getByRole('button', { name: 'Continue' }));
    await waitFor(() => expect(screen.getByRole('link', { name: 'Help with account activation' })).toBeInTheDocument());
    expect(screen.getByRole('link', { name: 'Help with account activation' }))
      .toHaveAttribute('href', expect.stringContaining('/user-guide/account-security/'));
  });
});
