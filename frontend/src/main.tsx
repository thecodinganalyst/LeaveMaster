import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App as AntdApp, ConfigProvider } from 'antd';
import { notificationProvider } from '@refinedev/antd';
import { Refine } from '@refinedev/core';
import routerBindings, { DocumentTitleHandler, UnsavedChangesNotifier } from '@refinedev/react-router-v6';
import dataProvider from '@refinedev/simple-rest';

import { AppRoutes } from './app/AppRoutes.tsx';
import { authProvider } from './providers/authProvider.ts';
import { resources } from './providers/resources.ts';
import { appTheme } from './theme/tokens.ts';
import { env } from './config/env.ts';
import './styles/app.css';

const queryClient = new QueryClient();

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        <ConfigProvider theme={appTheme}>
          <AntdApp>
            <Refine
              authProvider={authProvider}
              dataProvider={dataProvider(env.apiUrl)}
              routerProvider={routerBindings}
              notificationProvider={notificationProvider}
              resources={resources}
              options={{
                syncWithLocation: true,
                warnWhenUnsavedChanges: true,
                useNewQueryKeys: true,
                projectId: 'LeaveMaster',
              }}
            >
              <AppRoutes />
              <DocumentTitleHandler handler={() => 'LeaveMaster'} />
              <UnsavedChangesNotifier />
            </Refine>
          </AntdApp>
        </ConfigProvider>
      </QueryClientProvider>
    </BrowserRouter>
  </StrictMode>,
);
