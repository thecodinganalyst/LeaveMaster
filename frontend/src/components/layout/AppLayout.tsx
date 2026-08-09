import type { PropsWithChildren } from 'react';
import { useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useCan, useLogout } from '@refinedev/core';
import { AppstoreOutlined, CalendarOutlined, MenuOutlined, TeamOutlined } from '@ant-design/icons';
import { Button, Drawer, Grid, Layout, Menu, Space, Typography } from 'antd';

const { Header, Sider, Content } = Layout;

export const AppLayout = ({ children }: PropsWithChildren) => {
  const screens = Grid.useBreakpoint();
  const isDesktop = Boolean(screens.lg);
  const [open, setOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const { mutate: logout } = useLogout();
  const { data: leaveAccess } = useCan({ resource: 'leave-requests', action: 'list' });
  const { data: employeeAccess } = useCan({ resource: 'employees', action: 'list' });

  const menuItems = useMemo(
    () => [
      { key: '/', icon: <AppstoreOutlined />, label: <Link to="/">Dashboard</Link> },
      ...(leaveAccess?.can
        ? [
            {
              key: '/leave-requests',
              icon: <CalendarOutlined />,
              label: <Link to="/leave-requests">Leave Requests</Link>,
            },
          ]
        : []),
      ...(employeeAccess?.can
        ? [
            {
              key: '/employees',
              icon: <TeamOutlined />,
              label: <Link to="/employees">Employees</Link>,
            },
          ]
        : []),
    ],
    [employeeAccess?.can, leaveAccess?.can],
  );

  const selectedKeys = useMemo(() => {
    const key = menuItems.find(
      (item) => location.pathname === item.key || location.pathname.startsWith(`${item.key}/`),
    )?.key;

    return key ? [key] : ['/'];
  }, [location.pathname, menuItems]);

  const menu = (
    <Menu
      mode="inline"
      theme="dark"
      selectedKeys={selectedKeys}
      items={menuItems}
      onClick={() => {
        if (!isDesktop) {
          setOpen(false);
        }
      }}
    />
  );

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {isDesktop ? (
        <Sider width={250}>{menu}</Sider>
      ) : (
        <Drawer
          placement="left"
          title="LeaveMaster"
          open={open}
          onClose={() => setOpen(false)}
          styles={{ body: { padding: 0 } }}
        >
          {menu}
        </Drawer>
      )}

      <Layout>
        <Header className="app-header">
          <Space size="middle">
            {!isDesktop ? (
              <Button type="text" icon={<MenuOutlined />} onClick={() => setOpen(true)} aria-label="Open menu" />
            ) : null}
            <Typography.Title level={4} style={{ color: '#eaf2ff', margin: 0 }}>
              LeaveMaster
            </Typography.Title>
          </Space>
          <Button
            onClick={() =>
              logout(undefined, {
                onSuccess: () => {
                  navigate('/login');
                },
              })
            }
          >
            Sign out
          </Button>
        </Header>

        <Content style={{ margin: 24 }}>{children}</Content>
      </Layout>
    </Layout>
  );
};
