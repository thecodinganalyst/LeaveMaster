import type { PropsWithChildren } from 'react';
import { Card } from 'antd';

interface FormSectionProps {
  title: string;
}

export const FormSection = ({ title, children }: PropsWithChildren<FormSectionProps>) => {
  return (
    <Card title={title}>
      {children}
    </Card>
  );
};
