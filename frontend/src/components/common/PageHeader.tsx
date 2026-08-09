import type { ReactNode } from 'react';
import { Typography } from 'antd';

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  extra?: ReactNode;
}

export const PageHeader = ({ title, subtitle, extra }: PageHeaderProps) => {
  return (
    <div className="page-header">
      <div>
        <Typography.Title level={2} style={{ margin: 0 }}>
          {title}
        </Typography.Title>
        {subtitle ? <Typography.Text type="secondary">{subtitle}</Typography.Text> : null}
      </div>
      {extra}
    </div>
  );
};
