import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  testMatch: 'marketing-calculator.e2e.ts',
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  reporter: 'list',
  use: { baseURL: 'http://127.0.0.1:3000', trace: 'retain-on-failure', screenshot: 'only-on-failure' },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'npm --prefix ../../marketing run build && python3 -m http.server 3000 --bind 127.0.0.1 --directory ../../marketing/out',
    url: 'http://127.0.0.1:3000/tools/singapore-annual-leave-calculator',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
