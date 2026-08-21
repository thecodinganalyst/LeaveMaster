import { describe, expect, it } from 'vitest';

import { getAdminResourceConfig, getAdminResourceInitialValues } from './resourceConfigResolver.ts';
import {
  calendarJurisdictionIds,
  DEFAULT_STAFF_WORK_SCHEDULE,
  scheduleForDay,
  updateWorkSchedule,
  type WorkScheduleDayValue,
} from './staffFormHelpers.ts';

describe('StaffFormFields helpers', () => {
  it('limits jurisdiction ids to calendars configured for the tenant', () => {
    const result = calendarJurisdictionIds([
      { jurisdictionId: 'SG' },
      { jurisdictionId: 'SG' },
      { jurisdictionId: 'MY' },
      { jurisdictionId: null },
      {},
    ]);

    expect([...result]).toEqual(['SG', 'MY']);
  });

  it('defaults new staff to full weekdays and not-working weekends', () => {
    expect(DEFAULT_STAFF_WORK_SCHEDULE).toEqual([
      { dayOfWeek: 'MONDAY', daySchedule: 'FULL' },
      { dayOfWeek: 'TUESDAY', daySchedule: 'FULL' },
      { dayOfWeek: 'WEDNESDAY', daySchedule: 'FULL' },
      { dayOfWeek: 'THURSDAY', daySchedule: 'FULL' },
      { dayOfWeek: 'FRIDAY', daySchedule: 'FULL' },
    ]);
    expect(scheduleForDay(DEFAULT_STAFF_WORK_SCHEDULE, 'SATURDAY')).toBe('NONE');
    expect(scheduleForDay(DEFAULT_STAFF_WORK_SCHEDULE, 'SUNDAY')).toBe('NONE');

    const config = getAdminResourceConfig('employees');
    expect(config).toBeDefined();
    const initialValues = getAdminResourceInitialValues(config!);
    expect(initialValues.workSchedule).toEqual(DEFAULT_STAFF_WORK_SCHEDULE);
    expect(initialValues.workSchedule).not.toBe(DEFAULT_STAFF_WORK_SCHEDULE);
  });

  it('adds, changes and removes work schedule days while retaining weekday order', () => {
    let schedule: WorkScheduleDayValue[] = [];
    schedule = updateWorkSchedule(schedule, 'FRIDAY', 'FULL');
    schedule = updateWorkSchedule(schedule, 'MONDAY', 'AM');
    schedule = updateWorkSchedule(schedule, 'WEDNESDAY', 'PM');

    expect(schedule).toEqual([
      { dayOfWeek: 'MONDAY', daySchedule: 'AM' },
      { dayOfWeek: 'WEDNESDAY', daySchedule: 'PM' },
      { dayOfWeek: 'FRIDAY', daySchedule: 'FULL' },
    ]);
    expect(scheduleForDay(schedule, 'MONDAY')).toBe('AM');
    expect(scheduleForDay(schedule, 'TUESDAY')).toBe('NONE');

    schedule = updateWorkSchedule(schedule, 'MONDAY', 'FULL');
    expect(scheduleForDay(schedule, 'MONDAY')).toBe('FULL');

    schedule = updateWorkSchedule(schedule, 'WEDNESDAY', 'NONE');
    expect(schedule.some((entry) => entry.dayOfWeek === 'WEDNESDAY')).toBe(false);
  });

  it('treats a missing schedule as not working', () => {
    expect(scheduleForDay(undefined, 'SUNDAY')).toBe('NONE');
    expect(updateWorkSchedule(undefined, 'SUNDAY', 'NONE')).toEqual([]);
  });
});
