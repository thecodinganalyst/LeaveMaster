import type { ReactNode } from 'react';

const inlinePattern = /(\[([^\]]+)\]\(([^)\s]+)\)|\*\*([^*]+)\*\*|__([^_]+)__|`([^`]+)`|\*([^*\n]+)\*|_([^_\n]+)_)/g;

const safeHref = (href: string) => {
  try {
    const parsed = new URL(href, window.location.origin);
    return ['http:', 'https:', 'mailto:'].includes(parsed.protocol) ? href : undefined;
  } catch {
    return undefined;
  }
};

const renderInline = (text: string): ReactNode[] => {
  const nodes: ReactNode[] = [];
  let cursor = 0;
  let match: RegExpExecArray | null;
  let key = 0;

  inlinePattern.lastIndex = 0;
  while ((match = inlinePattern.exec(text)) !== null) {
    if (match.index > cursor) nodes.push(text.slice(cursor, match.index));

    const [raw, , linkText, href, strongA, strongB, code, emphasisA, emphasisB] = match;
    if (linkText && href) {
      const safe = safeHref(href);
      nodes.push(safe
        ? <a key={`inline-${key++}`} href={safe} target="_blank" rel="noopener noreferrer">{linkText}</a>
        : <span key={`inline-${key++}`}>{linkText}</span>);
    } else if (strongA || strongB) {
      nodes.push(<strong key={`inline-${key++}`}>{strongA ?? strongB}</strong>);
    } else if (code) {
      nodes.push(
        <code key={`inline-${key++}`} style={{ padding: '1px 4px', borderRadius: 4, background: 'rgba(0,0,0,0.06)' }}>
          {code}
        </code>,
      );
    } else if (emphasisA || emphasisB) {
      nodes.push(<em key={`inline-${key++}`}>{emphasisA ?? emphasisB}</em>);
    } else {
      nodes.push(raw);
    }
    cursor = match.index + raw.length;
  }
  if (cursor < text.length) nodes.push(text.slice(cursor));
  return nodes;
};

const isTableDivider = (line: string) => {
  const cells = line.trim().replace(/^\||\|$/g, '').split('|').map((cell) => cell.trim());
  return cells.length > 0 && cells.every((cell) => /^:?-{3,}:?$/.test(cell));
};

const tableCells = (line: string) => line.trim().replace(/^\||\|$/g, '').split('|').map((cell) => cell.trim());

interface Block {
  type: 'heading' | 'paragraph' | 'unordered-list' | 'ordered-list' | 'code' | 'blockquote' | 'table';
  level?: number;
  text?: string;
  items?: string[];
  language?: string | undefined;
  headers?: string[];
  rows?: string[][];
}

const parseBlocks = (markdown: string): Block[] => {
  const lines = markdown.replace(/\r\n?/g, '\n').split('\n');
  const blocks: Block[] = [];
  let index = 0;

  while (index < lines.length) {
    const line = lines[index]!;
    if (!line.trim()) {
      index += 1;
      continue;
    }

    const fence = line.match(/^\s*```([^`]*)$/);
    if (fence) {
      const language = fence[1]?.trim() || undefined;
      const codeLines: string[] = [];
      index += 1;
      while (index < lines.length && !/^\s*```\s*$/.test(lines[index]!)) {
        codeLines.push(lines[index]!);
        index += 1;
      }
      if (index < lines.length) index += 1;
      blocks.push({ type: 'code', text: codeLines.join('\n'), language });
      continue;
    }

    const heading = line.match(/^(#{1,6})\s+(.+)$/);
    if (heading) {
      blocks.push({ type: 'heading', level: heading[1]!.length, text: heading[2]!.trim() });
      index += 1;
      continue;
    }

    if (index + 1 < lines.length && line.includes('|') && isTableDivider(lines[index + 1]!)) {
      const headers = tableCells(line);
      const rows: string[][] = [];
      index += 2;
      while (index < lines.length && lines[index]!.includes('|') && lines[index]!.trim()) {
        rows.push(tableCells(lines[index]!));
        index += 1;
      }
      blocks.push({ type: 'table', headers, rows });
      continue;
    }

    const unordered = line.match(/^\s*[-*+]\s+(.+)$/);
    if (unordered) {
      const items: string[] = [];
      while (index < lines.length) {
        const item = lines[index]!.match(/^\s*[-*+]\s+(.+)$/);
        if (!item) break;
        items.push(item[1]!);
        index += 1;
      }
      blocks.push({ type: 'unordered-list', items });
      continue;
    }

    const ordered = line.match(/^\s*\d+[.)]\s+(.+)$/);
    if (ordered) {
      const items: string[] = [];
      while (index < lines.length) {
        const item = lines[index]!.match(/^\s*\d+[.)]\s+(.+)$/);
        if (!item) break;
        items.push(item[1]!);
        index += 1;
      }
      blocks.push({ type: 'ordered-list', items });
      continue;
    }

    if (/^\s*>\s?/.test(line)) {
      const quoted: string[] = [];
      while (index < lines.length && /^\s*>\s?/.test(lines[index]!)) {
        quoted.push(lines[index]!.replace(/^\s*>\s?/, ''));
        index += 1;
      }
      blocks.push({ type: 'blockquote', text: quoted.join(' ') });
      continue;
    }

    const paragraph: string[] = [line.trim()];
    index += 1;
    while (index < lines.length && lines[index]!.trim()) {
      const next = lines[index]!;
      if (/^(#{1,6})\s+/.test(next)
          || /^\s*```/.test(next)
          || /^\s*[-*+]\s+/.test(next)
          || /^\s*\d+[.)]\s+/.test(next)
          || /^\s*>\s?/.test(next)
          || (index + 1 < lines.length && next.includes('|') && isTableDivider(lines[index + 1]!))) break;
      paragraph.push(next.trim());
      index += 1;
    }
    blocks.push({ type: 'paragraph', text: paragraph.join(' ') });
  }

  return blocks;
};

const headingStyle = (level: number) => ({
  margin: level <= 2 ? '14px 0 8px' : '12px 0 6px',
  fontSize: level === 1 ? 22 : level === 2 ? 19 : level === 3 ? 17 : 15,
  lineHeight: 1.35,
});

export const AssistantMarkdown = ({ children }: { children: string }) => {
  const blocks = parseBlocks(children);

  return (
    <div data-testid="assistant-markdown" style={{ marginTop: 6, lineHeight: 1.55, overflowWrap: 'anywhere' }}>
      {blocks.map((block, index) => {
        const key = `block-${index}`;
        if (block.type === 'heading') {
          const level = block.level ?? 3;
          if (level === 1) return <h1 key={key} style={headingStyle(level)}>{renderInline(block.text ?? '')}</h1>;
          if (level === 2) return <h2 key={key} style={headingStyle(level)}>{renderInline(block.text ?? '')}</h2>;
          if (level === 3) return <h3 key={key} style={headingStyle(level)}>{renderInline(block.text ?? '')}</h3>;
          if (level === 4) return <h4 key={key} style={headingStyle(level)}>{renderInline(block.text ?? '')}</h4>;
          if (level === 5) return <h5 key={key} style={headingStyle(level)}>{renderInline(block.text ?? '')}</h5>;
          return <h6 key={key} style={headingStyle(level)}>{renderInline(block.text ?? '')}</h6>;
        }
        if (block.type === 'unordered-list' || block.type === 'ordered-list') {
          const Tag = block.type === 'ordered-list' ? 'ol' : 'ul';
          return (
            <Tag key={key} style={{ margin: '6px 0 10px', paddingLeft: 24 }}>
              {(block.items ?? []).map((item, itemIndex) => <li key={`${key}-${itemIndex}`}>{renderInline(item)}</li>)}
            </Tag>
          );
        }
        if (block.type === 'code') {
          return (
            <pre key={key} style={{ margin: '8px 0 10px', padding: 10, overflowX: 'auto', whiteSpace: 'pre', borderRadius: 6, background: 'rgba(0,0,0,0.06)' }}>
              <code data-language={block.language}>{block.text}</code>
            </pre>
          );
        }
        if (block.type === 'blockquote') {
          return (
            <blockquote key={key} style={{ margin: '8px 0 10px', paddingLeft: 12, borderLeft: '3px solid rgba(0,0,0,0.15)' }}>
              {renderInline(block.text ?? '')}
            </blockquote>
          );
        }
        if (block.type === 'table') {
          return (
            <div key={key} style={{ overflowX: 'auto', maxWidth: '100%', margin: '8px 0 12px' }}>
              <table style={{ borderCollapse: 'collapse', width: '100%', minWidth: 420 }}>
                <thead>
                  <tr>
                    {(block.headers ?? []).map((header, cellIndex) => (
                      <th key={`${key}-head-${cellIndex}`} style={{ textAlign: 'left', padding: '7px 9px', borderBottom: '1px solid rgba(0,0,0,0.18)', whiteSpace: 'nowrap' }}>
                        {renderInline(header)}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {(block.rows ?? []).map((row, rowIndex) => (
                    <tr key={`${key}-row-${rowIndex}`}>
                      {(block.headers ?? []).map((_, cellIndex) => (
                        <td key={`${key}-cell-${rowIndex}-${cellIndex}`} style={{ padding: '7px 9px', borderBottom: '1px solid rgba(0,0,0,0.08)', verticalAlign: 'top' }}>
                          {renderInline(row[cellIndex] ?? '')}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          );
        }
        return <p key={key} style={{ margin: '6px 0 10px' }}>{renderInline(block.text ?? '')}</p>;
      })}
    </div>
  );
};
