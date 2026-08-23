import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { tenantLeaveTypeSourceLink } from './ResourceShowPage.tsx';

describe('tenantLeaveTypeSourceLink', () => {
  it('renders a safe external link that opens in a new tab', () => {
    render(tenantLeaveTypeSourceLink('  https://www.mom.gov.sg/annual-leave  '));

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', 'https://www.mom.gov.sg/annual-leave');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('renders a placeholder when there is no source URL', () => {
    render(tenantLeaveTypeSourceLink(null));

    expect(screen.getByText('—')).toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });
});
