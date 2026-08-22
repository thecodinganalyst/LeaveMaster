import { render, screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { AssistantMarkdown } from './AssistantMarkdown.tsx';

describe('AssistantMarkdown', () => {
  it('renders headings, emphasis, lists and inline code', () => {
    render(
      <AssistantMarkdown>{`### Annual Leave\n\nEmployees receive **7 days** initially.\n\n- First tier\n- Second tier\n\nUse \`SERVICE_MONTHS\` only for technical detail.`}</AssistantMarkdown>,
    );

    expect(screen.getByRole('heading', { level: 3, name: 'Annual Leave' })).toBeInTheDocument();
    expect(screen.getByText('7 days').tagName).toBe('STRONG');
    expect(screen.getByRole('list')).toBeInTheDocument();
    expect(screen.getByText('SERVICE_MONTHS').tagName).toBe('CODE');
  });

  it('renders GFM-style tables inside a horizontally scrollable wrapper', () => {
    const { container } = render(
      <AssistantMarkdown>{`| Service period | Entitlement |\n| --- | ---: |\n| 3–11 months | **7 days** |\n| 84+ months | 14 days |`}</AssistantMarkdown>,
    );

    const table = screen.getByRole('table');
    expect(within(table).getByRole('columnheader', { name: 'Service period' })).toBeInTheDocument();
    expect(within(table).getByText('3–11 months')).toBeInTheDocument();
    expect(within(table).getByText('7 days').tagName).toBe('STRONG');
    expect(table.parentElement).toHaveStyle({ overflowX: 'auto', maxWidth: '100%' });
    expect(container.textContent).not.toContain('| --- |');
  });

  it('renders fenced code and block quotes without executing HTML', () => {
    const { container } = render(
      <AssistantMarkdown>{`> Authoritative configuration\n\n\`\`\`json\n{"days": 7}\n\`\`\`\n\n<img src=x onerror=alert(1)>`}</AssistantMarkdown>,
    );

    expect(screen.getByText('Authoritative configuration').closest('blockquote')).toBeInTheDocument();
    expect(screen.getByText('{"days": 7}').closest('code')).toHaveAttribute('data-language', 'json');
    expect(container.querySelector('img')).toBeNull();
    expect(screen.getByText('<img src=x onerror=alert(1)>')).toBeInTheDocument();
  });

  it('allows safe links and suppresses unsafe link protocols', () => {
    render(
      <AssistantMarkdown>{`Read [the guide](https://example.com/guide) but not [this](javascript:alert(1)).`}</AssistantMarkdown>,
    );

    expect(screen.getByRole('link', { name: 'the guide' })).toHaveAttribute('href', 'https://example.com/guide');
    expect(screen.queryByRole('link', { name: 'this' })).not.toBeInTheDocument();
    expect(screen.getByText('this')).toBeInTheDocument();
  });
});
