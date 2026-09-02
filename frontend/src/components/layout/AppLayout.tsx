import type { PropsWithChildren } from 'react';
import { useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useCan, useGetIdentity, useLogout } from '@refinedev/core';
import {
  AppstoreOutlined,
  AuditOutlined,
  BankOutlined,
  CalendarOutlined,
  CheckSquareOutlined,
  GlobalOutlined,
  LockOutlined,
  MailOutlined,
  MenuOutlined,
  MessageOutlined,
  QuestionCircleOutlined,
  SafetyCertificateOutlined,
  SolutionOutlined,
  TagsOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Button, Drawer, Grid, Layout, Menu, Space, Typography } from 'antd';

import { docsLinks, contextualHelpForPath } from '../../config/docsLinks.ts';
import { AssistantPanel } from '../../features/assistant/AssistantPanel.tsx';

const { Header, Sider, Content } = Layout;

interface LeaveMasterIdentity {
  platformAdmin?: boolean;
}

export const AppLayout = ({ children }: PropsWithChildren) => {
  const screens = Grid.useBreakpoint();
  const isDesktop = Boolean(screens.lg);
  const [menuOpen, setMenuOpen] = useState(false);
  const [assistantOpen, setAssistantOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const { mutate: logout } = useLogout();
  const { data: identity } = useGetIdentity<LeaveMasterIdentity>();
  const platformAdmin = Boolean(identity?.platformAdmin);
  const { data: leaveAccess } = useCan({ resource: 'leave-requests', action: 'list' });
  const { data: approvalAccess } = useCan({ resource: 'leave-requests', action: 'approve' });
  const { data: employeeAccess } = useCan({ resource: 'employees', action: 'list' });
  const { data: tenantAccess } = useCan({ resource: 'tenants', action: 'list' });
  const { data: jurisdictionAccess } = useCan({ resource: 'jurisdictions', action: 'list' });
  const { data: jurisdictionLeaveTypeAccess } = useCan({ resource: 'jurisdiction-leave-types', action: 'list' });
  const { data: publicHolidayAccess } = useCan({ resource: 'public-holidays', action: 'list' });
  const { data: userAccess } = useCan({ resource: 'users', action: 'list' });
  const { data: roleAccess } = useCan({ resource: 'roles', action: 'list' });
  const { data: leaveTypeAccess } = useCan({ resource: 'leave-types', action: 'list' });
  const { data: entitlementPolicyAccess } = useCan({ resource: 'leave-entitlement-policies', action: 'list' });
  const { data: eligibilityRuleAccess } = useCan({ resource: 'leave-entitlement-policy-eligibility-rules', action: 'list' });
  const { data: calendarAccess } = useCan({ resource: 'leave-calendars', action: 'list' });
  const { data: calendarEditAccess } = useCan({ resource: 'leave-calendars', action: 'edit' });
  const { data: approverAccess } = useCan({ resource: 'leave-approvers', action: 'list' });
  const contextualHelp = contextualHelpForPath(location.pathname, {
    canApproveLeave: Boolean(approvalAccess?.can),
    canEditLeaveCalendars: Boolean(calendarEditAccess?.can),
  });

  const menuItems = useMemo(
    () => [
      { key: '/', icon: <AppstoreOutlined />, label: <Link to="/">Dashboard</Link> },
      ...(leaveAccess?.can ? [{ key: '/leave-requests', icon: <CalendarOutlined />, label: <Link to="/leave-requests">Leave Requests</Link> }] : []),
      ...(approvalAccess?.can ? [{ key: '/approvals', icon: <CheckSquareOutlined />, label: <Link to="/approvals">Approval Inbox</Link> }] : []),
      ...(employeeAccess?.can ? [{ key: '/employees', icon: <TeamOutlined />, label: <Link to="/employees">Staff</Link> }] : []),
      ...(tenantAccess?.can ? [{ key: '/tenants', icon: <BankOutlined />, label: <Link to="/tenants">Tenants</Link> }] : []),
      ...(platformAdmin ? [{ key: '/contact-enquiries', icon: <MailOutlined />, label: <Link to="/contact-enquiries">Contact Enquiries</Link> }] : []),
      ...(jurisdictionAccess?.can ? [{ key: '/jurisdictions', icon: <GlobalOutlined />, label: <Link to="/jurisdictions">Jurisdictions</Link> }] : []),
      ...(jurisdictionLeaveTypeAccess?.can ? [{ key: '/jurisdiction-leave-types', icon: <TagsOutlined />, label: <Link to="/jurisdiction-leave-types">Jurisdiction Leave Types</Link> }] : []),
      ...(publicHolidayAccess?.can ? [{ key: '/public-holidays', icon: <CalendarOutlined />, label: <Link to="/public-holidays">Public Holiday Templates</Link> }] : []),
      ...(userAccess?.can ? [{ key: '/users', icon: <UserOutlined />, label: <Link to="/users">App Users</Link> }] : []),
      ...(roleAccess?.can ? [{ key: '/roles', icon: <SafetyCertificateOutlined />, label: <Link to="/roles">Roles</Link> }] : []),
      ...(leaveTypeAccess?.can ? [{ key: '/leave-types', icon: <TagsOutlined />, label: <Link to="/leave-types">Leave Types</Link> }] : []),
      ...(platformAdmin && entitlementPolicyAccess?.can ? [{ key: '/leave-entitlement-policies', icon: <SolutionOutlined />, label: <Link to="/leave-entitlement-policies">Entitlement Policies</Link> }] : []),
      ...(platformAdmin && eligibilityRuleAccess?.can ? [{ key: '/leave-entitlement-policy-eligibility-rules', icon: <SolutionOutlined />, label: <Link to="/leave-entitlement-policy-eligibility-rules">Eligibility Rules</Link> }] : []),
      ...(calendarAccess?.can ? [{ key: '/leave-calendars', icon: <CalendarOutlined />, label: <Link to="/leave-calendars">Leave Calendars</Link> }] : []),
      ...(approverAccess?.can ? [{ key: '/leave-approvers', icon: <AuditOutlined />, label: <Link to="/leave-approvers">Leave Approvers</Link> }] : []),
      ...(!isDesktop ? [
        { key: 'user-guide', icon: <QuestionCircleOutlined />, label: <a href={docsLinks.userGuide} target="_blank" rel="noopener noreferrer">User Guide</a> },
        { key: '/account/security', icon: <SafetyCertificateOutlined />, label: <Link to="/account/security">Security</Link> },
        { key: '/account/change-password', icon: <LockOutlined />, label: <Link to="/account/change-password">Change Password</Link> },
      ] : []),
    ],
    [approvalAccess?.can, approverAccess?.can, calendarAccess?.can, eligibilityRuleAccess?.can, employeeAccess?.can, entitlementPolicyAccess?.can, isDesktop, jurisdictionAccess?.can, jurisdictionLeaveTypeAccess?.can, leaveAccess?.can, leaveTypeAccess?.can, platformAdmin, publicHolidayAccess?.can, roleAccess?.can, tenantAccess?.can, userAccess?.can],
  );

  const selectedKeys = useMemo(() => {
    const key = menuItems.find((item) => typeof item.key === 'string' && item.key.startsWith('/') && (location.pathname === item.key || location.pathname.startsWith(`${item.key}/`)))?.key;
    return key ? [key] : ['/'];
  }, [location.pathname, menuItems]);

  const menu = (
    <Menu
      mode="inline"
      theme="dark"
      selectedKeys={selectedKeys}
      items={menuItems}
      onClick={() => {
        if (!isDesktop) setMenuOpen(false);
      }}
    />
  );

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {isDesktop ? (
        <Sider width={250}>{menu}</Sider>
      ) : (
        <Drawer placement="left" title="LeaveMaestro" open={menuOpen} onClose={() => setMenuOpen(false)} styles={{ body: { padding: 0 } }}>
          {menu}
        </Drawer>
      )}

      <Layout>
        <Header className="app-header">
          <Space size="middle">
            {!isDesktop ? (
              <Button
                className="mobile-menu-button"
                icon={<MenuOutlined />}
                onClick={() => setMenuOpen(true)}
                aria-label="Open menu"
              />
            ) : null}
            <Typography.Title level={4} style={{ color: '#eaf2ff', margin: 0 }}>LeaveMaestro</Typography.Title>
          </Space>
          <Space>
            {contextualHelp ? (
              <Button
                icon={<QuestionCircleOutlined />}
                href={contextualHelp.href}
                target="_blank"
                rel="noopener noreferrer"
                aria-label={contextualHelp.label}
              >
                {isDesktop ? contextualHelp.label : null}
              </Button>
            ) : null}
            {isDesktop ? (
              <Button
                icon={<QuestionCircleOutlined />}
                href={docsLinks.userGuide}
                target="_blank"
                rel="noopener noreferrer"
                aria-label="Open LeaveMaestro User Guide"
              >
                User Guide
              </Button>
            ) : null}
            <Button icon={<MessageOutlined />} onClick={() => setAssistantOpen(true)} aria-label="Open Ask LeaveMaestro assistant">
              {isDesktop ? 'Ask LeaveMaestro' : null}
            </Button>
            {isDesktop ? (
              <>
                <Button icon={<SafetyCertificateOutlined />} onClick={() => navigate('/account/security')} aria-label="Security">
                  Security
                </Button>
                <Button icon={<LockOutlined />} onClick={() => navigate('/account/change-password')} aria-label="Change password">
                  Change password
                </Button>
              </>
            ) : null}
            <Button onClick={() => logout(undefined, { onSuccess: () => navigate('/login') })}>Sign out</Button>
          </Space>
        </Header>
        <Content style={{ margin: isDesktop ? 24 : 12 }}>{children}</Content>
      </Layout>

      <Drawer
        placement="right"
        width={isDesktop ? 480 : '100%'}
        open={assistantOpen}
        onClose={() => setAssistantOpen(false)}
        closable={false}
        destroyOnHidden={false}
        styles={{ body: { padding: 20 } }}
        aria-label="Ask LeaveMaestro"
      >
        <AssistantPanel onClose={() => setAssistantOpen(false)} />
      </Drawer>
    </Layout>
  );
};
