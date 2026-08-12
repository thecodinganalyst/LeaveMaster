import { act, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { getCsrfToken } from '../api/http.ts';
import { ColdStartGate } from './ColdStartGate.tsx';

vi.mock('../api/http.ts', () => ({
  getCsrfToken: vi.fn(),
}));

const mockedGetCsrfToken = vi.mocked(getCsrfToken);

describe('ColdStartGate', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-12T00:00:00Z'));
    mockedGetCsrfToken.mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows the waking-up screen and updates the elapsed wait duration', async () => {
    mockedGetCsrfToken.mockImplementation(() => new Promise(() => undefined));

    render(
      <ColdStartGate>
        <div>Application ready</div>
      </ColdStartGate>,
    );

    expect(screen.getByText('LeaveMaster is waking up after sleeping')).toBeInTheDocument();
    expect(screen.getByText('Waiting for 0 seconds…')).toBeInTheDocument();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3_000);
    });

    expect(screen.getByText('Waiting for 3 seconds…')).toBeInTheDocument();
    expect(screen.queryByText('Application ready')).not.toBeInTheDocument();
  });

  it('retries readiness and automatically renders the application when the backend responds', async () => {
    mockedGetCsrfToken.mockRejectedValueOnce(new Error('backend sleeping')).mockResolvedValue({
      token: 'token',
      headerName: 'X-CSRF-TOKEN',
      parameterName: '_csrf',
    });

    render(
      <ColdStartGate retryIntervalMs={2_000}>
        <div>Application ready</div>
      </ColdStartGate>,
    );

    await act(async () => {
      await Promise.resolve();
    });

    expect(mockedGetCsrfToken).toHaveBeenCalledTimes(1);

    await act(async () => {
      await vi.advanceTimersByTimeAsync(2_000);
    });

    expect(mockedGetCsrfToken).toHaveBeenCalledTimes(2);
    expect(await screen.findByText('Application ready')).toBeInTheDocument();
    expect(screen.queryByText('LeaveMaster is waking up after sleeping')).not.toBeInTheDocument();
  });

  it('cleans up timers when it is unmounted', async () => {
    mockedGetCsrfToken.mockRejectedValue(new Error('backend sleeping'));

    const { unmount } = render(
      <ColdStartGate retryIntervalMs={2_000}>
        <div>Application ready</div>
      </ColdStartGate>,
    );

    await act(async () => {
      await Promise.resolve();
    });

    unmount();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(5_000);
    });

    expect(mockedGetCsrfToken).toHaveBeenCalledTimes(1);
  });
});
