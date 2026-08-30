import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiError } from '../../api/http.ts';
import { LoginPage } from './LoginPage.tsx';

const login = vi.fn();
const lookupAccountActivation = vi.fn();
const requestAccountActivationPin = vi.fn();
const verifyAccountActivationPin = vi.fn();
const setInitialAccountPassword = vi.fn();
const startOAuthLogin = vi.fn();

vi.mock('@refinedev/core', () => ({
  useLogin: () => ({ mutate: login, isPending: false }),
}));

vi.mock('../../api/accountActivation.ts', () => ({
  lookupAccountActivation: (...args: unknown[]) => lookupAccountActivation(...args),
  requestAccountActivationPin: (...args: unknown[]) => requestAccountActivationPin(...args),
  verifyAccountActivationPin: (...args: unknown[]) => verifyAccountActivationPin(...args),
  setInitialAccountPassword: (...args: unknown[]) => setInitialAccountPassword(...args),
}));

vi.mock('../../api/oauth.ts', () => ({
  getRememberedOAuthProvider: () => {
    const value = window.sessionStorage.getItem('leavemaster.oauthProvider');
    return value === 'google' || value === 'github' ? value : undefined;
  },
  startOAuthLogin: (...args: unknown[]) => startOAuthLogin(...args),
}));

const renderPage = (entry = '/login?to=%2Fleave') => render(
  <MemoryRouter initialEntries={[entry]}>
    <LoginPage />
  </MemoryRouter>,
);

const enterIdentifier = async (name = 'alice', tenantId = 'tenant-a') => {
  fireEvent.change(screen.getByLabelText('Tenant ID'), { target: { value: tenantId } });
  fireEvent.change(screen.getByLabelText('Login name'), { target: { value: name } });
  fireEvent.click(screen.getByRole('button', { name: 'Continue' }));
  await waitFor(() => expect(lookupAccountActivation).toHaveBeenCalledWith({ tenantId, loginName: name }));
};

describe('LoginPage account activation and OAuth sign-in', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.sessionStorage.clear();
    lookupAccountActivation.mockResolvedValue({ nextStep: 'PASSWORD' });
    requestAccountActivationPin.mockResolvedValue({ message: 'accepted' });
    verifyAccountActivationPin.mockResolvedValue({ message: 'verified' });
    setInitialAccountPassword.mockResolvedValue(undefined);
  });

  it('shows Google and GitHub as sign-in choices', () => {
    renderPage();

    fireEvent.click(screen.getByRole('button', { name: /Continue with Google/i }));
    expect(startOAuthLogin).toHaveBeenCalledWith('google');

    fireEvent.click(screen.getByRole('button', { name: /Continue with GitHub/i }));
    expect(startOAuthLogin).toHaveBeenCalledWith('github');
  });

  it('keeps unlinked OAuth users on normal login and directs setup to Security', () => {
    window.sessionStorage.setItem('leavemaster.oauthProvider', 'google');
    renderPage('/login?oauthError=not_linked');

    expect(screen.getByText(/not linked to LeaveMaestro yet/i)).toBeInTheDocument();
    expect(screen.getByText(/open Security to set up Google sign-in/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Continue with Google/i })).toBeInTheDocument();
    expect(screen.getByLabelText('Tenant ID')).toBeInTheDocument();
    expect(screen.queryByText('Set up Google sign-in')).not.toBeInTheDocument();
  });

  it('requires a free-text tenant ID and signs in with LeaveMaestro credentials', async () => {
    renderPage();
    expect(screen.getByLabelText('Tenant ID')).toBeInTheDocument();
    expect(screen.queryByRole('combobox', { name: 'Tenant ID' })).not.toBeInTheDocument();
    expect(screen.queryByText(/Platform administrators/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/\bPLATFORM\b/)).not.toBeInTheDocument();
    await enterIdentifier();

    expect(await screen.findByText('tenant-a / alice')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'secret123' } });
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => expect(login).toHaveBeenCalledWith({
      tenantId: 'tenant-a',
      loginName: 'alice',
      password: 'secret123',
      redirectPath: '/leave',
    }));
  });

  it('routes a newly provisioned account to verification PIN activation', async () => {
    lookupAccountActivation.mockResolvedValue({ nextStep: 'ACTIVATION' });
    renderPage();

    await enterIdentifier('Bravo_Admin', 'Bravo');
    expect(await screen.findByText('Bravo / Bravo_Admin')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Send verification PIN' }));

    await waitFor(() => expect(requestAccountActivationPin).toHaveBeenCalledWith({
      tenantId: 'Bravo',
      loginName: 'Bravo_Admin',
    }));
  });

  it('propagates tenant identity through PIN verification and activation', async () => {
    lookupAccountActivation.mockResolvedValue({ nextStep: 'ACTIVATION' });
    renderPage();
    await enterIdentifier();

    fireEvent.click(await screen.findByRole('button', { name: 'Send verification PIN' }));
    fireEvent.change(await screen.findByLabelText('Verification PIN'), { target: { value: '123456' } });
    fireEvent.click(screen.getByRole('button', { name: 'Verify PIN' }));
    await waitFor(() => expect(verifyAccountActivationPin).toHaveBeenCalledWith(
      { tenantId: 'tenant-a', loginName: 'alice' }, '123456',
    ));

    fireEvent.change(await screen.findByLabelText('New password'), { target: { value: 'strongpass' } });
    fireEvent.change(screen.getByLabelText('Confirm new password'), { target: { value: 'strongpass' } });
    fireEvent.click(screen.getByRole('button', { name: 'Activate account' }));

    await waitFor(() => expect(setInitialAccountPassword).toHaveBeenCalledWith(
      { tenantId: 'tenant-a', loginName: 'alice' }, 'strongpass',
    ));
    expect(await screen.findByText('Account activated')).toBeInTheDocument();
  });

  it('shows a privacy-safe message when the external identity is already linked elsewhere', () => {
    window.sessionStorage.setItem('leavemaster.oauthProvider', 'github');
    renderPage('/login?oauthError=identity_in_use');

    expect(screen.getByText('This GitHub account is already linked to another LeaveMaestro account.')).toBeInTheDocument();
  });

  it('shows a recoverable cancellation message', () => {
    window.sessionStorage.setItem('leavemaster.oauthProvider', 'google');
    renderPage('/login?oauthError=access_denied');

    expect(screen.getByText('Google sign-in was cancelled or denied. You can try again.')).toBeInTheDocument();
  });

  it('resets tenant ID and login name when choosing a different account', async () => {
    renderPage();
    await enterIdentifier();
    fireEvent.click(await screen.findByRole('button', { name: 'Use a different account' }));

    expect(screen.getByLabelText('Tenant ID')).toHaveValue('');
    expect(screen.getByLabelText('Login name')).toHaveValue('');
  });

  it('shows lookup errors without advancing to password login', async () => {
    lookupAccountActivation.mockRejectedValue(
      new ApiError('Unexpected response from authentication service. Please try again.', 502),
    );
    renderPage();

    await enterIdentifier('Bravo_Admin', 'Bravo');

    expect(await screen.findByText('Unexpected response from authentication service. Please try again.')).toBeInTheDocument();
    expect(screen.queryByLabelText('Password')).not.toBeInTheDocument();
  });
});
