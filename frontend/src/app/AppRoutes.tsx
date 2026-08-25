import { Authenticated } from '@refinedev/core';
import { NavigateToResource } from '@refinedev/react-router-v6';
import { Spin, Typography } from 'antd';
import { Outlet, Route, Routes } from 'react-router-dom';

import { AppLayout } from '../components/layout/AppLayout.tsx';
import { ErrorState } from '../components/common/ErrorState.tsx';
import { ChangePasswordPage } from '../pages/auth/ChangePasswordPage.tsx';
import { LoginPage } from '../pages/auth/LoginPage.tsx';
import { DashboardPage } from '../pages/dashboard/DashboardPage.tsx';
import { ApplyLeavePage } from '../pages/leave/ApplyLeavePage.tsx';
import { ApprovalInboxPage } from '../pages/leave/ApprovalInboxPage.tsx';
import { LeaveDetailsPage } from '../pages/leave/LeaveDetailsPage.tsx';
import { MyLeavePage } from '../pages/leave/MyLeavePage.tsx';
import { EntitlementWorkflowPage } from '../pages/resources/EntitlementWorkflowPage.tsx';
import { ResourceCreatePage } from '../pages/resources/ResourceCreatePage.tsx';
import { ResourceEditPage } from '../pages/resources/ResourceEditPage.tsx';
import { ResourceListPage } from '../pages/resources/ResourceListPage.tsx';
import { ResourceShowPage } from '../pages/resources/ResourceShowPage.tsx';

const AuthenticationLoadingState = () => (
  <main className="auth-loading-page" role="status" aria-live="polite" aria-busy="true">
    <Spin size="large" />
    <Typography.Text>Checking your session…</Typography.Text>
  </main>
);

export const AppRoutes = () => {
  return (
    <Routes>
      <Route
        element={
          <Authenticated
            key="authenticated"
            redirectOnFail="/login"
            appendCurrentPathToQuery
            loading={<AuthenticationLoadingState />}
          >
            <AppLayout>
              <Outlet />
            </AppLayout>
          </Authenticated>
        }
      >
        <Route path="/" element={<DashboardPage />} />
        <Route path="/account/change-password" element={<ChangePasswordPage />} />
        <Route path="/leave-requests" element={<MyLeavePage />} />
        <Route path="/leave-requests/apply" element={<ApplyLeavePage />} />
        <Route path="/leave-requests/create" element={<ApplyLeavePage />} />
        <Route path="/leave-requests/show/:id" element={<LeaveDetailsPage />} />
        <Route path="/leave-requests/edit/:id" element={<LeaveDetailsPage />} />
        <Route path="/approvals" element={<ApprovalInboxPage />} />
        <Route path="/leave-types/:leaveTypeId/entitlements/create" element={<EntitlementWorkflowPage />} />
        <Route path="/leave-types/:leaveTypeId/entitlements/:policyId/edit" element={<EntitlementWorkflowPage />} />
        <Route path="/:resource">
          <Route index element={<ResourceListPage />} />
          <Route path="create" element={<ResourceCreatePage />} />
          <Route path="edit/:id" element={<ResourceEditPage />} />
          <Route path="show/:id" element={<ResourceShowPage />} />
        </Route>
        <Route
          path="/error"
          element={<ErrorState title="Something went wrong" description="An unexpected application error occurred. Please try again." />}
        />
        <Route path="*" element={<ErrorState title="Page not found" description="The page you requested does not exist." />} />
      </Route>

      <Route
        element={
          <Authenticated key="public" fallback={<Outlet />} loading={<AuthenticationLoadingState />}>
            <NavigateToResource resource="dashboard" />
          </Authenticated>
        }
      >
        <Route path="/login" element={<LoginPage />} />
      </Route>
    </Routes>
  );
};
