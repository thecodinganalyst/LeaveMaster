import type { PropsWithChildren } from 'react';
import { useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useCan, useLogout } from '@refinedev/core';
import {
  AppstoreOutlined,
  AuditOutlined,
  BankOutlined,
  CalendarOutlined,
  CheckSquareOutlined,
  EnvironmentOutlined,
  LockOutlined,
  MenuOutlined,
  MessageOutlined,
  SafetyCertificateOutlined,
  TagsOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Button, Drawer, Grid, Layout, Menu, Space, Typography } from 'antd';

import { AssistantPanel } from '../../features/assistant/AssistantPanel.tsx';

const { Header, Sider, Content } = Layout;

export const AppLayout = ({ children }: PropsWithChildren) => {
  const screens = Grid.useBreakpoint();
  const isDesktop = Boolean(screens.lg);
  const [menuOpen, setMenuOpen] = useState(false);
  const [assistantOpen, setAssistantOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const { mutate: logout } = useLogout();
  const { data: leaveAccess } = useCan({ resource: 'leave-requests', action: 'list' });
  const { data: approvalAccess } = useCan({ resource: 'leave-requests', action: 'approve' });
  const { data: employeeAccess } = useCan({ resource: 'employees', action: 'list' });
  const { data: tenantAccess } = useCan({ resource: 'tenants', action: 'list' });
  const { data: userAccess } = useCan({ resource: 'users', action: 'list' });
  const { data: roleAccess } = useCan({ resource: 'roles', action: 'list' });
  const { data: locationAccess } = useCan({ resource: 'locations', action: 'list' });
  const { data: leaveTypeAccess } = useCan({ resource: 'leave-types', action: 'list' });
  const { data: calendarAccess } = useCan({ resource: 'leave-calendars', action: 'list' });
  const { data: approverAccess } = useCan({ resource: 'leave-approvers', action: 'list' });

  const menuItems = useMemo(
    () => [
      { key: '/', icon: <AppstoreOutlined />, label: <Link to="/">Dashboard</Link> },
      ...(leaveAccess?.can ? [{ key: '/leave-requests', icon: <CalendarOutlined />, label: <Link to="/leave-requests">Leave Requests</Link> }] : []),
      ...(approvalAccess?.can ? [{ key: '/approvals', icon: <CheckSquareOutlined />, label: <Link to="/approvals">Approval Inbox</Link> }] : []),
      ...(employeeAccess?.can ? [{ key: '/employees', icon: <TeamOutlined />, label: <Link to="/employees">Staff</Link> }] : []),
      ...(tenantAccess?.can ? [{ key: '/tenants', icon: <BankOutlined />, label: <Link to="/tenants">Tenants</Link> }] : []),
      ...(userAccess?.can ? [{ key: '/users', icon: <UserOutlined />, label: <Link to="/users">App Users</Link> }] : []),
      ...(roleAccess?.can ? [{ key: '/roles', icon: <SafetyCertificateOutlined />, label: <Link to="/roles">Roles</Link> }] : []),
      ...(locationAccess?.can ? [{ key: '/locations', icon: <EnvironmentOutlined />, label: <Link to="/locations">Locations</Link> }] : []),
      ...(leaveTypeAccess?.can ? [{ key: '/leave-types', icon: <TagsOutlined />, label: <Link to="/leave-types">Leave Types</Link> }] : []),
      ...(calendarAccess?.can ? [{ key: '/leave-calendars', icon: <CalendarOutlined />, label: <Link to="/leave-calendars">Leave Calendars</Link> }] : []),
      ...(approverAccess?.can ? [{ key: '/leave-approvers', icon: <AuditOutlined />, label: <Link to="/leave-approvers">Leave Approvers</Link> }] : []),
    ],
    [approvalAccess?.can, approverAccess?.can, calendarAccess?.can, employeeAccess?.can, leaveAccess?.can, leaveTypeAccess?.can, locationAccess?.can, roleAccess?.can, tenantAccess?.can, userAccess?.can],
  );

  const selectedKeys = useMemo(() => {
    const key = menuItems.find((item) => location.pathname === item.key || location.pathname.startsWith(`${item.key}/`))?.key;
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
        <Drawer placement="left" title="LeaveMaster" open={menuOpen} onClose={() => setMenuOpen(false)} styles={{ body: { padding: 0 } }}>
          {menu}
        </Drawer>
      )}

      <Layout>
        <Header className="app-header">
          <Space size="middle">
            {!isDesktop ? <Button type="text" icon={<MenuOutlined />} onClick={() => setMenuOpen(true)} aria-label="Open menu" /> : null}
            <Typography.Title level={4} style={{ color: '#eaf2ff', margin: 0 }}>LeaveMaster</Typography.Title>
          </Space>
          <Space>
            <Button icon={<MessageOutlined />} onClick={() => setAssistantOpen(true)} aria-label="Open Ask LeaveMaster assistant">
              {isDesktop ? 'Ask LeaveMaster' : null}
            </Button>
            <Button icon={<LockOutlined />} onClick={() => navigate('/account/change-password')} aria-label="Change password">
              {isDesktop ? 'Change password' : null}
            </Button>
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
        aria-label="Ask LeaveMaster"
      >
        <AssistantPanel onClose={() => setAssistantOpen(false)} />
      </Drawer>
    </Layout>
  );
};
