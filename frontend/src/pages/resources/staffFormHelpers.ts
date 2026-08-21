export type DaySchedule = 'FULL' | 'AM' | 'PM';
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface WorkScheduleDayValue {
  dayOfWeek: DayOfWeek;
  daySchedule: DaySchedule;
}

export interface LeaveCalendarJurisdictionSource {
  jurisdictionId?: string | null;
}

export const STAFF_SCHEDULE_DAYS: Array<{ value: DayOfWeek; label: string }> = [
  { value: 'MONDAY', label: 'Monday' },
  { value: 'TUESDAY', label: 'Tuesday' },
  { value: 'WEDNESDAY', label: 'Wednesday' },
  { value: 'THURSDAY', label: 'Thursday' },
  { value: 'FRIDAY', label: 'Friday' },
  { value: 'SATURDAY', label: 'Saturday' },
  { value: 'SUNDAY', label: 'Sunday' },
];

export const calendarJurisdictionIds = (calendars: LeaveCalendarJurisdictionSource[]) => new Set(
  calendars.map((calendar) => calendar.jurisdictionId?.trim()).filter((value): value is string => Boolean(value)),
);

export const scheduleForDay = (schedule: WorkScheduleDayValue[] | undefined, day: DayOfWeek) =>
  schedule?.find((entry) => entry.dayOfWeek === day)?.daySchedule ?? 'NONE';

export const updateWorkSchedule = (
  schedule: WorkScheduleDayValue[] | undefined,
  day: DayOfWeek,
  value: DaySchedule | 'NONE',
): WorkScheduleDayValue[] => {
  const withoutDay = (schedule ?? []).filter((entry) => entry.dayOfWeek !== day);
  if (value === 'NONE') return withoutDay;
  const next = [...withoutDay, { dayOfWeek: day, daySchedule: value }];
  return STAFF_SCHEDULE_DAYS.flatMap(({ value: orderedDay }) => next.filter((entry) => entry.dayOfWeek === orderedDay));
};
