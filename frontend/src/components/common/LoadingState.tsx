import { Card, Skeleton } from 'antd';

export const LoadingState = () => {
  return (
    <Card>
      <Skeleton active paragraph={{ rows: 5 }} />
    </Card>
  );
};
