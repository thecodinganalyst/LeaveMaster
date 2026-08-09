import { Alert } from 'antd';

import { ResourceCreatePage } from './ResourceCreatePage.tsx';

export const ResourceEditPage = () => {
  return (
    <>
      <Alert type="info" showIcon message="Edit mode" description="This screen reuses the same form shell." style={{ marginBottom: 16 }} />
      <ResourceCreatePage />
    </>
  );
};
