import type { ThemeConfig } from 'antd';

export const appTheme: ThemeConfig = {
  token: {
    colorPrimary: '#2d9c8f',
    colorInfo: '#2d9c8f',
    colorSuccess: '#2f855a',
    colorWarning: '#dd8a28',
    colorError: '#d64545',
    colorBgLayout: '#f4f7fb',
    colorBorderSecondary: '#dce6ef',
    borderRadius: 12,
    borderRadiusLG: 16,
    fontFamily: 'Inter, system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif',
  },
  components: {
    Layout: {
      headerBg: '#0f2740',
      siderBg: '#112f4d',
      triggerBg: '#112f4d',
      triggerColor: '#eaf2ff',
      bodyBg: '#f4f7fb',
    },
    Menu: {
      darkItemBg: '#112f4d',
      darkItemHoverBg: '#1c4b73',
      darkItemSelectedBg: '#2d9c8f',
      darkItemSelectedColor: '#ffffff',
    },
    Card: {
      borderRadiusLG: 18,
      colorBorderSecondary: '#dce6ef',
    },
    Button: {
      borderRadius: 10,
      controlHeight: 40,
    },
  },
};
