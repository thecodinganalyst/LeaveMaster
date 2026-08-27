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

vi.mock('@refinedev/core', () => ({
  useLogin: () => ({ mutate: login, isPending: false }),
}));

vi.mock('../../api/accountActivation.ts', () => ({
  lookupAccountActivation: (...args: unknown[]) => lookupAccountActivation(...args),
  requestAccountActivationPin: (...args: unknown[]) => requestAccountActivationPin(...args),
  verifyAccountActivationPin: (...args: unknown[]) => verifyAccountActivationPin(...args),
  setInitialAccountPassword: (...args: unknown[]) => setInitialAccountPassword(...args),
}));

const renderPage = () => render(
  <MemoryRouter initialEntries={['/login?to=%2Fleave']}>
    <LoginPage />
  </MemoryRouter>,
);

const enterIdentifier = async (name = 'alice') => {
  fireEvent.change(screen.getByLabelText('Login name'), { target: { value: name } });
  fireEvent.click(screen.getByRole('button', { name: 'Continue' }));
  await waitFor(() => expect(lookupAccountActivation).toHaveBeenCalledWith(name));
};

describe('LoginPage account activation', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    lookupAccountActivation.mockResolvedValue({ nextStep: 'PASSWORD' });
    requestAccountActivationPin.mockResolvedValue({ message: 'accepted' });
    verifyAccountActivationPin.mockResolvedValue({ message: 'verified' });
    setInitialAccountPassword.mockResolvedValue(undefined);
  });

  it('keeps existing active accounts on the password login flow', async () => {
    renderPage();
    await enterIdentifier();

    expect(await screen.findByLabelText('Password')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'secret123' } });
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => expect(login).toHaveBeenCalledWith({
      loginName: 'alice',
      password: 'secret123',
      redirectPath: '/leave',
    }));
    expect(requestAccountActivationPin).not.toHaveBeenCalled();
  });

  it('only requests a PIN after the user explicitly chooses activation', async () => {
    lookupAccountActivation.mockResolvedValue({ nextStep: 'ACTIVATION' });
    renderPage();
    await enterIdentifier();

    expect(await screen.findByText('Your account needs to be set up before you can sign in.')).toBeInTheDocument();
    expect(requestAccountActivationPin).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'Send verification PIN' }));
    await waitFor(() => expect(requestAccountActivationPin).toHaveBeenCalledWith('alice'));

    expect(await screen.findByLabelText('Verification PIN')).toBeInTheDocument();
    expect(screen.getByText(/expires after 15 minutes/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Resend PIN in/i })).toBeDisabled();
  });

  it('verifies the PIN and completes initial password setup', async () => {
    lookupAccountActivation.mockResolvedValue({ nextStep: 'ACTIVATION' });
    renderPage();
    await enterIdentifier();
    fireEvent.click(await screen.findByRole('button', { name: 'Send verification PIN' }));

    const pin = await screen.findByLabelText('Verification PIN');
    fireEvent.change(pin, { target: { value: '123456' } });
    fireEvent.click(screen.getByRole('button', { name: 'Verify PIN' }));
    await waitFor(() => expect(verifyAccountActivationPin).toHaveBeenCalledWith('alice', '123456'));

    const password = await screen.findByLabelText('New password');
    const confirmation = screen.getByLabelText('Confirm new password');
    fireEvent.change(password, { target: { value: 'strongpass' } });
    fireEvent.change(confirmation, { target: { value: 'strongpass' } });
    fireEvent.click(screen.getByRole('button', { name: 'Activate account' }));

    await waitFor(() => expect(setInitialAccountPassword).toHaveBeenCalledWith('alice', 'strongpass'));
    expect(await screen.findByText('Account activated')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Continue to sign in' }));
    expect(await screen.findByLabelText('Password')).toBeInTheDocument();
  });

  it('shows safe API errors without advancing the activation flow', async () => {
    lookupAccountActivation.mockResolvedValue({ nextStep: 'ACTIVATION' });
    requestAccountActivationPin.mockRejectedValue(new ApiError('Unable to send a verification PIN. Please try again later.', 503));
    renderPage();
    await enterIdentifier();

    fireEvent.click(await screen.findByRole('button', { name: 'Send verification PIN' }));

    expect(await screen.findByText('Unable to send a verification PIN. Please try again later.')).toBeInTheDocument();
    expect(screen.queryByLabelText('Verification PIN')).not.toBeInTheDocument();
  });
});
