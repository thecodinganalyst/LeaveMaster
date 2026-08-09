import { Authenticated } from '@refinedev/core';
import { CatchAllNavigate, NavigateToResource } from '@refinedev/react-router-v6';
import { Navigate, Outlet, Route, Routes } from 'react-router-dom';

import { AppLayout } from '../components/layout/AppLayout.tsx';
import { DashboardPage } from '../pages/dashboard/DashboardPage.tsx';
import { LoginPage } from '../pages/auth/LoginPage.tsx';
import { ResourceCreatePage } from '../pages/resources/ResourceCreatePage.tsx';
import { ResourceEditPage } from '../pages/resources/ResourceEditPage.tsx';
import { ResourceListPage } from '../pages/resources/ResourceListPage.tsx';
import { ResourceShowPage } from '../pages/resources/ResourceShowPage.tsx';
import { ErrorState } from '../components/common/ErrorState.tsx';

export const AppRoutes = () => {
  return (
    <Routes>
      <Route
        element={
          <Authenticated key="authenticated" fallback={<CatchAllNavigate to="/login" />}>
            <AppLayout>
              <Outlet />
            </AppLayout>
          </Authenticated>
        }
      >
        <Route path="/" element={<DashboardPage />} />
        <Route path="/:resource">
          <Route index element={<ResourceListPage />} />
          <Route path="create" element={<ResourceCreatePage />} />
          <Route path="edit/:id" element={<ResourceEditPage />} />
          <Route path="show/:id" element={<ResourceShowPage />} />
        </Route>
      </Route>

      <Route
        element={
          <Authenticated key="public" fallback={<Outlet />}>
            <NavigateToResource resource="dashboard" />
          </Authenticated>
        }
      >
        <Route path="/login" element={<LoginPage />} />
      </Route>

      <Route path="*" element={<ErrorState title="Page not found" description="The page you requested does not exist." />} />
      <Route path="/error" element={<Navigate to="/" replace />} />
    </Routes>
  );
};
