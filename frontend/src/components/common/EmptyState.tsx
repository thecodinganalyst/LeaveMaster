import type { ReactNode } from 'react';
import { Card, Empty, Typography } from 'antd';

interface EmptyStateProps {
  title: string;
  description?: string;
  action?: ReactNode;
}

export const EmptyState = ({ title, description, action }: EmptyStateProps) => {
  return (
    <Card>
      <Typography.Title level={4}>{title}</Typography.Title>
      <Empty description={description}>
        {action}
      </Empty>
    </Card>
  );
};
