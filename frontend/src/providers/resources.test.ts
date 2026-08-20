import { describe, expect, it } from 'vitest';

import { resources } from './resources.ts';

describe('resources', () => {
  it('registers leave calendar show and edit routes', () => {
    const leaveCalendars = resources.find((resource) => resource.name === 'leave-calendars');

    expect(leaveCalendars).toMatchObject({
      list: '/leave-calendars',
      create: '/leave-calendars/create',
      edit: '/leave-calendars/edit/:id',
      show: '/leave-calendars/show/:id',
    });
  });
});
