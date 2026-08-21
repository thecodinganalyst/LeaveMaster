import { describe, expect, it } from 'vitest';

import {
  calendarJurisdictionIds,
  scheduleForDay,
  updateWorkSchedule,
  type WorkScheduleDayValue,
} from './StaffFormFields.tsx';

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
