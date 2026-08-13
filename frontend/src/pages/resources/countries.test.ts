import { describe, expect, it } from 'vitest';

import { countryOptions, getCountryOptions } from './countries.ts';

describe('country options', () => {
  it('contains the ISO country list in canonical display form', () => {
    expect(countryOptions).toHaveLength(249);
    expect(countryOptions).toContainEqual({ label: 'Singapore', value: 'Singapore' });
    expect(countryOptions).toContainEqual({ label: 'United States', value: 'United States' });
  });

  it('moves the preferred country to the first option without duplicating it', () => {
    const options = getCountryOptions('Singapore');

    expect(options[0]).toEqual({ label: 'Singapore', value: 'Singapore' });
    expect(options.filter((option) => option.value === 'Singapore')).toHaveLength(1);
    expect(options).toHaveLength(countryOptions.length);
  });

  it('also recognises an ISO alpha-2 country code as the preferred country', () => {
    expect(getCountryOptions('SG')[0]).toEqual({ label: 'Singapore', value: 'Singapore' });
  });

  it('falls back to the normal country list when the preferred country is unavailable', () => {
    expect(getCountryOptions('Unknown Country')).toEqual(countryOptions);
    expect(getCountryOptions(null)).toEqual(countryOptions);
  });
});
