import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

describe('frontend API route namespace', () => {
  it('keeps resource screen paths separate from backend API paths', () => {
    const dataProviderSource = readFileSync(resolve(process.cwd(), 'src/providers/dataProvider.ts'), 'utf8');

    expect(dataProviderSource).toContain("tenants: '/api/tenants'");
    expect(dataProviderSource).toContain("users: '/api/users'");
    expect(dataProviderSource).toContain("roles: '/api/roles'");
    expect(dataProviderSource).not.toContain("locations: '/api/locations'");
    expect(dataProviderSource).toContain("'leave-types': '/api/leave-types'");
    expect(dataProviderSource).toContain("'leave-approvers': '/api/leave-approvers'");
    expect(dataProviderSource).toContain("'leave-calendars': '/api/leave-calendars'");
  });
});
