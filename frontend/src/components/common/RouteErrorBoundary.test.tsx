import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { RouteErrorBoundary } from './RouteErrorBoundary.tsx';

const BrokenPage = () => {
  throw new Error('render failed');
};

describe('RouteErrorBoundary', () => {
  it('shows a visible fallback when a route throws during rendering', () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);

    render(
      <RouteErrorBoundary>
        <BrokenPage />
      </RouteErrorBoundary>,
    );

    expect(screen.getByText('Unable to display this page')).toBeInTheDocument();
    expect(screen.getByText('An unexpected application error occurred while rendering this page. Please try again.')).toBeInTheDocument();

    consoleError.mockRestore();
  });
});
