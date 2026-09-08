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
const requestPasswordResetPin = vi.fn();
const verifyPasswordResetPin = vi.fn();
const setResetPassword = vi.fn();
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

vi.mock('../../api/passwordReset.ts', () => ({
  requestPasswordResetPin: (...args: unknown[]) => requestPasswordResetPin(...args),
  verifyPasswordResetPin: (...args: unknown[]) => verifyPasswordResetPin(...args),
  setResetPassword: (...args: unknown[]) => setResetPassword(...args),
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

describe('LoginPage account activation, password reset and OAuth sign-in', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.sessionStorage.clear();
    lookupAccountActivation.mockResolvedValue({ nextStep: 'PASSWORD' });
    requestAccountActivationPin.mockResolvedValue({ message: 'accepted' });
    verifyAccountActivationPin.mockResolvedValue({ message: 'verified' });
    setInitialAccountPassword.mockResolvedValue(undefined);
    requestPasswordResetPin.mockResolvedValue({ message: 'accepted' });
    verifyPasswordResetPin.mockResolvedValue({ message: 'verified' });
    setResetPassword.mockResolvedValue(undefined);
  });

  it('shows Google and GitHub as sign-in choices', () => {
    renderPage();

    fireEvent.click(screen.getByRole('button', { name: /Continue with Google/i }));
    expect(startOAuthLogin).toHaveBeenCalledWith('google');

    fireEvent.click(screen.getByRole('button', { name: /Continue with GitHub/i }));
    expect(startOAuthLogin).toHaveBeenCalledWith('github');
  });

  it('renders account identifiers before OAuth choices and the evaluation notice last', () => {
    renderPage();

    const tenantId = screen.getByLabelText('Tenant ID');
    const loginName = screen.getByLabelText('Login name');
    const google = screen.getByRole('button', { name: /Continue with Google/i });
    const github = screen.getByRole('button', { name: /Continue with GitHub/i });
    const evaluationNotice = screen.getByText('Hosted evaluation — test data only');

    expect(tenantId.compareDocumentPosition(loginName) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(loginName.compareDocumentPosition(google) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(google.compareDocumentPosition(github) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(github.compareDocumentPosition(evaluationNotice) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
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

  it('runs the forgot-password request, PIN verification, password reset and success journey', async () => {
    renderPage();
    await enterIdentifier('alice', 'tenant-a');

    fireEvent.click(await screen.findByRole('button', { name: 'Forgot password?' }));
    await waitFor(() => expect(requestPasswordResetPin).toHaveBeenCalledWith({
      tenantId: 'tenant-a', loginName: 'alice',
    }));
    expect(await screen.findByText(/If this account is eligible for password recovery/i)).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('Password reset PIN'), { target: { value: '123456' } });
    fireEvent.click(screen.getByRole('button', { name: 'Verify PIN' }));
    await waitFor(() => expect(verifyPasswordResetPin).toHaveBeenCalledWith(
      { tenantId: 'tenant-a', loginName: 'alice' }, '123456',
    ));

    fireEvent.change(await screen.findByLabelText('New password'), { target: { value: 'new-password' } });
    fireEvent.change(screen.getByLabelText('Confirm new password'), { target: { value: 'new-password' } });
    fireEvent.click(screen.getByRole('button', { name: 'Reset password' }));
    await waitFor(() => expect(setResetPassword).toHaveBeenCalledWith(
      { tenantId: 'tenant-a', loginName: 'alice' }, 'new-password',
    ));
    expect(await screen.findByText('Password reset')).toBeInTheDocument();
  });

  it('uses the platform realm unchanged during forgot-password recovery', async () => {
    renderPage();
    await enterIdentifier('PlatformAdmin', 'PLATFORM');

    fireEvent.click(await screen.findByRole('button', { name: 'Forgot password?' }));

    await waitFor(() => expect(requestPasswordResetPin).toHaveBeenCalledWith({
      tenantId: 'PLATFORM', loginName: 'PlatformAdmin',
    }));
  });

  it('keeps forgot-password request messaging generic when no email can be delivered', async () => {
    requestPasswordResetPin.mockResolvedValue({ message: 'accepted' });
    renderPage();
    await enterIdentifier('PlatformAdmin', 'PLATFORM');

    fireEvent.click(await screen.findByRole('button', { name: 'Forgot password?' }));

    expect(await screen.findByText(/If this account is eligible for password recovery/i)).toBeInTheDocument();
    expect(screen.queryByText(/no email/i)).not.toBeInTheDocument();
  });

  it('returns from reset PIN entry to password sign-in without losing account context', async () => {
    renderPage();
    await enterIdentifier();
    fireEvent.click(await screen.findByRole('button', { name: 'Forgot password?' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Back to password sign in' }));

    expect(await screen.findByLabelText('Password')).toBeInTheDocument();
    expect(screen.getByText('tenant-a / alice')).toBeInTheDocument();
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
