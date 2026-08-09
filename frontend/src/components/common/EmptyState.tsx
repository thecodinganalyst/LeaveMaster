import type { ReactNode } from 'react';
import { Card, Empty } from 'antd';

interface EmptyStateProps {
  title: string;
  description?: string;
  action?: ReactNode;
}

export const EmptyState = ({ title, description, action }: EmptyStateProps) => {
  return (
    <Card>
      <Empty description={description ?? title}>
        {action}
      </Empty>
    </Card>
  );
};
