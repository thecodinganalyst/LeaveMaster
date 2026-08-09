import type { PropsWithChildren } from 'react';
import { Space } from 'antd';

export const PageContainer = ({ children }: PropsWithChildren) => {
  return (
    <Space direction="vertical" size={20} style={{ width: '100%' }}>
      {children}
    </Space>
  );
};
