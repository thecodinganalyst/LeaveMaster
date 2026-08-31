import { describe, expect, it } from 'vitest';

import { createSubmissionValues } from './ResourceCreatePage.tsx';
import { getAdminResourceConfig, normaliseFormValues } from './resourceConfigResolver.ts';

describe('staff create submission values', () => {
  it('uses preserved step-1 values together with reviewed entitlements', () => {
    const submittedValues = {
      leaveEntitlements: [{
        leaveType: { id: 'ANNUAL_LEAVE', name: 'Annual Leave' },
        policyId: 'SG-ANNUAL',
        from: '2026-01-01',
        to: '2026-12-31',
        entitlement: 14,
      }],
    };
    const allFormValues = {
      id: '001',
      name: 'Test Staff',
      email: 'staff@example.com',
      joinDate: '2026-01-01',
      loginName: '001',
      jurisdictionId: 'SG',
      employmentType: 'FULL_TIME',
      workSchedule: [{ dayOfWeek: 'MONDAY', daySchedule: 'FULL' }],
      roles: ['Bravo_Staff'],
      dependants: [{
        name: 'Child',
        relationshipCode: 'CHILD',
        dateOfBirth: '2024-01-01',
        citizenshipCode: 'SG',
        residencyCode: 'SG',
      }],
      leaveApprovers: [],
      leaveEntitlements: submittedValues.leaveEntitlements,
    };

    const selected = createSubmissionValues(true, submittedValues, allFormValues);
    const config = getAdminResourceConfig('employees');
    if (!config) throw new Error('Employee config missing');
    const payload = normaliseFormValues(config, selected);

    expect(payload).toMatchObject({
      id: '001',
      name: 'Test Staff',
      joinDate: '2026-01-01',
      jurisdictionId: 'SG',
      employmentType: 'FULL_TIME',
      roles: ['Bravo_Staff'],
      leaveApprovers: [],
    });
    expect(payload.dependants).toEqual(allFormValues.dependants);
    expect(payload.workSchedule).toEqual(allFormValues.workSchedule);
    expect(payload.leaveEntitlements).toEqual([{
      policyId: 'SG-ANNUAL',
      from: '2026-01-01',
      to: '2026-12-31',
      entitlement: 14,
      leaveTypeId: 'ANNUAL_LEAVE',
    }]);
  });

  it('keeps the normal submitted values for non-staff resources', () => {
    const submittedValues = { name: 'Submitted' };
    const allFormValues = { name: 'Preserved', hidden: true };

    expect(createSubmissionValues(false, submittedValues, allFormValues)).toBe(submittedValues);
  });
});
