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

const enterIdentifier = async (name = 'alice', tenantId = 'tenant-a') => {
  fireEvent.change(screen.getByLabelText('Tenant ID'), { target: { value: tenantId } });
  fireEvent.change(screen.getByLabelText('Login name'), { target: { value: name } });
  fireEvent.click(screen.getByRole('button', { name: 'Continue' }));
  await waitFor(() => expect(lookupAccountActivation).toHaveBeenCalledWith({ tenantId, loginName: name }));
};

describe('LoginPage account activation', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    lookupAccountActivation.mockResolvedValue({ nextStep: 'PASSWORD' });
    requestAccountActivationPin.mockResolvedValue({ message: 'accepted' });
    verifyAccountActivationPin.mockResolvedValue({ message: 'verified' });
    setInitialAccountPassword.mockResolvedValue(undefined);
  });

  it('requires a free-text tenant ID and keeps active accounts on password login', async () => {
    renderPage();
    expect(screen.getByLabelText('Tenant ID')).toBeInTheDocument();
    expect(screen.queryByRole('combobox', { name: 'Tenant ID' })).not.toBeInTheDocument();
    expect(screen.getByText(/Platform administrators use PLATFORM/i)).toBeInTheDocument();
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
    expect(requestAccountActivationPin).not.toHaveBeenCalled();
  });

  it('propagates tenant identity through PIN request, verification and password setup', async () => {
    lookupAccountActivation.mockResolvedValue({ nextStep: 'ACTIVATION' });
    renderPage();
    await enterIdentifier();

    expect(await screen.findByText('Your account needs to be set up before you can sign in.')).toBeInTheDocument();
    expect(screen.getByText('tenant-a / alice')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Send verification PIN' }));
    await waitFor(() => expect(requestAccountActivationPin).toHaveBeenCalledWith({ tenantId: 'tenant-a', loginName: 'alice' }));

    const pin = await screen.findByLabelText('Verification PIN');
    expect(screen.getByText('tenant-a / alice')).toBeInTheDocument();
    fireEvent.change(pin, { target: { value: '123456' } });
    fireEvent.click(screen.getByRole('button', { name: 'Verify PIN' }));
    await waitFor(() => expect(verifyAccountActivationPin).toHaveBeenCalledWith(
      { tenantId: 'tenant-a', loginName: 'alice' }, '123456',
    ));

    const password = await screen.findByLabelText('New password');
    fireEvent.change(password, { target: { value: 'strongpass' } });
    fireEvent.change(screen.getByLabelText('Confirm new password'), { target: { value: 'strongpass' } });
    fireEvent.click(screen.getByRole('button', { name: 'Activate account' }));

    await waitFor(() => expect(setInitialAccountPassword).toHaveBeenCalledWith(
      { tenantId: 'tenant-a', loginName: 'alice' }, 'strongpass',
    ));
    expect(await screen.findByText('Account activated')).toBeInTheDocument();
  });

  it('resets both tenant ID and login name when choosing a different account', async () => {
    renderPage();
    await enterIdentifier();
    fireEvent.click(await screen.findByRole('button', { name: 'Use a different account' }));

    expect(screen.getByLabelText('Tenant ID')).toHaveValue('');
    expect(screen.getByLabelText('Login name')).toHaveValue('');
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
