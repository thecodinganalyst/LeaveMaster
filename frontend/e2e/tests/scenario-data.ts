export type ScenarioRole = 'staff' | 'manager' | 'hr' | 'admin';

export interface ScenarioPerson {
  alias: string;
  loginName: string;
  staffId: string;
  name: string;
  role: ScenarioRole;
  joinDate: string;
  termDate?: string;
  jurisdictionId: string;
  approverAlias?: string;
}

export interface StandardScenario {
  scenarioId: string;
  tenantId: string;
  jurisdictionId: string;
  annualLeaveTypeId: string;
  people: Record<string, ScenarioPerson>;
}

const iso = (date: Date) => date.toISOString().slice(0, 10);

const atStartOfYear = (referenceDate: Date, yearOffset = 0) =>
  new Date(Date.UTC(referenceDate.getUTCFullYear() + yearOffset, 0, 1));

export const createStandardSingaporeScenario = (
  scenarioId: string,
  referenceDate = new Date('2026-09-12T00:00:00Z'),
): StandardScenario => {
  const normalized = scenarioId.trim().replace(/[^A-Za-z0-9_-]/g, '-');
  if (!normalized) throw new Error('scenarioId must not be blank');

  const tenantId = `E2E-${normalized}`;
  const priorYearStart = atStartOfYear(referenceDate, -1);
  const twoYearsAgo = atStartOfYear(referenceDate, -2);
  const midYear = new Date(Date.UTC(referenceDate.getUTCFullYear(), 6, 1));
  const recentJoiner = new Date(referenceDate);
  recentJoiner.setUTCDate(recentJoiner.getUTCDate() - 14);

  const person = (
    alias: string,
    name: string,
    role: ScenarioRole,
    joinDate: Date,
    approverAlias?: string,
  ): ScenarioPerson => ({
    alias,
    loginName: alias,
    staffId: `${tenantId}-${alias}`,
    name,
    role,
    joinDate: iso(joinDate),
    jurisdictionId: 'SG',
    approverAlias,
  });

  const people: Record<string, ScenarioPerson> = {
    admin: person('admin', 'E2E Admin', 'admin', twoYearsAgo),
    hr: person('hr', 'E2E HR', 'hr', twoYearsAgo),
    manager01: person('manager01', 'E2E Manager 01', 'manager', twoYearsAgo),
    manager02: person('manager02', 'E2E Manager 02', 'manager', priorYearStart),
    staff001: person('staff001', 'E2E Normal Staff', 'staff', twoYearsAgo, 'manager01'),
    staff002: person('staff002', 'E2E Mid-year Joiner', 'staff', midYear, 'manager01'),
    staff003: person('staff003', 'E2E Recent Joiner', 'staff', recentJoiner, 'manager02'),
    staff004: person('staff004', 'E2E Jurisdiction Override', 'staff', priorYearStart, 'manager02'),
    staff005: person('staff005', 'E2E Missing Approver', 'staff', priorYearStart),
  };

  return {
    scenarioId: normalized,
    tenantId,
    jurisdictionId: 'SG',
    annualLeaveTypeId: `${tenantId}:SG:ANNUAL_LEAVE`,
    people,
  };
};

export const personForRole = (scenario: StandardScenario, role: ScenarioRole): ScenarioPerson => {
  if (role === 'admin') return scenario.people.admin;
  if (role === 'hr') return scenario.people.hr;
  if (role === 'manager') return scenario.people.manager01;
  return scenario.people.staff001;
};
