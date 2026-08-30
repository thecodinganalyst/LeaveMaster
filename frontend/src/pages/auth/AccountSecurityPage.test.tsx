import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AccountSecurityPage } from './AccountSecurityPage.tsx';

const getOAuthLinkStatus = vi.fn();
const startOAuthLink = vi.fn();

vi.mock('../../api/oauth.ts', () => ({
  getOAuthLinkStatus: () => getOAuthLinkStatus(),
  startOAuthLink: (...args: unknown[]) => startOAuthLink(...args),
}));

const renderPage = (entry = '/account/security') => render(
  <MemoryRouter initialEntries={[entry]}>
    <AccountSecurityPage />
  </MemoryRouter>,
);

describe('AccountSecurityPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getOAuthLinkStatus.mockResolvedValue({ linked: false, provider: null });
    startOAuthLink.mockResolvedValue(undefined);
  });

  it('shows Google and GitHub setup actions for an unlinked account', async () => {
    renderPage();

    expect(await screen.findByRole('button', { name: /Set up Google sign-in/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Set up GitHub sign-in/i })).toBeInTheDocument();
  });

  it('starts Google linking from the authenticated settings page', async () => {
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: /Set up Google sign-in/i }));
    await waitFor(() => expect(startOAuthLink).toHaveBeenCalledWith('google'));
  });

  it('starts GitHub linking from the authenticated settings page', async () => {
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: /Set up GitHub sign-in/i }));
    await waitFor(() => expect(startOAuthLink).toHaveBeenCalledWith('github'));
  });

  it('shows the linked provider and hides additional setup actions', async () => {
    getOAuthLinkStatus.mockResolvedValue({ linked: true, provider: 'google' });
    renderPage();

    expect(await screen.findByText('Google connected')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Set up GitHub sign-in/i })).not.toBeInTheDocument();
  });

  it('shows a success state after an OAuth link redirect', async () => {
    getOAuthLinkStatus.mockResolvedValue({ linked: true, provider: 'github' });
    renderPage('/account/security?oauthLinked=true');

    expect(await screen.findByText('GitHub sign-in is now connected.')).toBeInTheDocument();
  });

  it('shows link errors returned from the provider redirect', async () => {
    renderPage('/account/security?oauthError=identity_in_use');

    expect(await screen.findByText(/already linked to another LeaveMaestro account/i)).toBeInTheDocument();
  });
});
