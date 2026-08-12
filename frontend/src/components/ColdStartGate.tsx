import { type PropsWithChildren, useEffect, useState } from 'react';
import { Spin, Typography } from 'antd';

import { getCsrfToken } from '../api/http.ts';

const DEFAULT_RETRY_INTERVAL_MS = 2_000;

interface ColdStartGateProps extends PropsWithChildren {
  retryIntervalMs?: number;
}

export const ColdStartGate = ({
  children,
  retryIntervalMs = DEFAULT_RETRY_INTERVAL_MS,
}: ColdStartGateProps) => {
  const [isReady, setIsReady] = useState(false);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);

  useEffect(() => {
    let cancelled = false;
    let retryTimer: number | undefined;
    const startedAt = Date.now();

    const elapsedTimer = window.setInterval(() => {
      setElapsedSeconds(Math.floor((Date.now() - startedAt) / 1_000));
    }, 1_000);

    const checkReadiness = async () => {
      try {
        await getCsrfToken(true);
      } catch {
        if (!cancelled) {
          retryTimer = window.setTimeout(checkReadiness, retryIntervalMs);
        }
        return;
      }

      if (!cancelled) {
        window.clearInterval(elapsedTimer);
        setIsReady(true);
      }
    };

    void checkReadiness();

    return () => {
      cancelled = true;
      window.clearInterval(elapsedTimer);
      if (retryTimer !== undefined) {
        window.clearTimeout(retryTimer);
      }
    };
  }, [retryIntervalMs]);

  if (isReady) {
    return children;
  }

  return (
    <main className="cold-start-page" aria-live="polite" aria-busy="true">
      <section className="cold-start-card">
        <Spin size="large" />
        <Typography.Title level={2} className="cold-start-title">
          LeaveMaster is waking up after sleeping
        </Typography.Title>
        <Typography.Paragraph className="cold-start-message">
          The application was resting to save resources. It will open automatically as soon as it is ready.
        </Typography.Paragraph>
        <Typography.Text className="cold-start-timer" strong>
          Waiting for {elapsedSeconds} {elapsedSeconds === 1 ? 'second' : 'seconds'}…
        </Typography.Text>
      </section>
    </main>
  );
};
