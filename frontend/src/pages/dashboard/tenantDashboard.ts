export interface TenantSummary {
  id: string;
  name: string;
  startDate?: string;
  endDate?: string;
  status?: 'ACTIVE' | 'DORMANT' | 'TERMINATED' | string;
}

export const summariseTenants = (tenants: TenantSummary[]) => ({
  total: tenants.length,
  active: tenants.filter((tenant) => tenant.status === 'ACTIVE').length,
  dormant: tenants.filter((tenant) => tenant.status === 'DORMANT').length,
  terminated: tenants.filter((tenant) => tenant.status === 'TERMINATED').length,
});
