import { Alert, Card } from 'antd';

interface ErrorStateProps {
  title: string;
  description: string;
}

export const ErrorState = ({ title, description }: ErrorStateProps) => {
  return (
    <Card>
      <Alert type="error" message={title} description={description} showIcon />
    </Card>
  );
};
