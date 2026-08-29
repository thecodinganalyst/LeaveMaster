import { App } from 'antd';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiFetch } from '../../api/http.ts';
import { getCurrentUser } from '../../auth/session.ts';
import { ContactEnquiriesPage } from './ContactEnquiriesPage.tsx';

vi.mock('../../api/http.ts', () => ({ apiFetch: vi.fn() }));
vi.mock('../../auth/session.ts', () => ({ getCurrentUser: vi.fn() }));

const mockedApiFetch = vi.mocked(apiFetch);
const mockedGetCurrentUser = vi.mocked(getCurrentUser);

const enquiry = {
  id: 'e1',
  name: 'Alice',
  company: 'Acme',
  email: 'alice@example.com',
  phone: null,
  companySize: '21-100',
  country: 'Singapore',
  enquiryType: 'PRODUCT_DEMO',
  message: 'Please show me the product.',
  status: 'NEW' as const,
  createdAt: '2026-08-29T00:00:00Z',
  firstReadAt: null,
  replies: [],
};

const renderPage = () => render(<App><ContactEnquiriesPage /></App>);

describe('ContactEnquiriesPage', () => {
  beforeEach(() => {
    mockedApiFetch.mockReset();
    mockedGetCurrentUser.mockReset();
    mockedGetCurrentUser.mockResolvedValue({
      loginName: 'platformadmin',
      staffId: null,
      tenantId: null,
      active: true,
      platformAdmin: true,
      authorities: [],
    });
  });

  it('shows the newest platform contact enquiries and opens detail', async () => {
    mockedApiFetch
      .mockResolvedValueOnce([enquiry])
      .mockResolvedValueOnce({ ...enquiry, status: 'READ', firstReadAt: '2026-08-29T00:05:00Z' });

    renderPage();

    expect(await screen.findByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Acme')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Open' }));

    expect(await screen.findByText('Original message')).toBeInTheDocument();
    expect(screen.getByText('Please show me the product.')).toBeInTheDocument();
    expect(mockedApiFetch).toHaveBeenCalledWith('/api/platform/contact-enquiries/e1');
  });

  it('sends a reply and renders reply history', async () => {
    mockedApiFetch
      .mockResolvedValueOnce([enquiry])
      .mockResolvedValueOnce({ ...enquiry, status: 'READ' })
      .mockResolvedValueOnce({
        ...enquiry,
        status: 'REPLIED',
        replies: [{ id: 'r1', replyBody: 'Happy to help.', repliedBy: 'platformadmin', createdAt: '2026-08-29T01:00:00Z' }],
      });

    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Open' }));
    const textarea = await screen.findByPlaceholderText('Write a reply to the person who submitted this enquiry.');
    fireEvent.change(textarea, { target: { value: 'Happy to help.' } });
    fireEvent.click(screen.getByRole('button', { name: 'Send email reply' }));

    await waitFor(() => expect(mockedApiFetch).toHaveBeenCalledWith(
      '/api/platform/contact-enquiries/e1/reply',
      expect.objectContaining({ method: 'POST', body: JSON.stringify({ body: 'Happy to help.' }) }),
    ));
    expect(await screen.findByText('Happy to help.')).toBeInTheDocument();
  });

  it('blocks non-platform users from the private inbox', async () => {
    mockedGetCurrentUser.mockResolvedValue({
      loginName: 'tenantadmin',
      staffId: 'S1',
      tenantId: 'T1',
      active: true,
      platformAdmin: false,
      authorities: [],
    });

    renderPage();

    expect(await screen.findByText('Platform administrator access required')).toBeInTheDocument();
    expect(mockedApiFetch).not.toHaveBeenCalled();
  });

  it('surfaces API errors', async () => {
    mockedApiFetch.mockRejectedValue(new Error('Request failed'));
    renderPage();
    expect(await screen.findByText('Request failed')).toBeInTheDocument();
  });
});
