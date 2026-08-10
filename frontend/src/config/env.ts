const apiUrlFromEnv = import.meta.env.VITE_API_URL?.trim();

const stripTrailingSlash = (value: string) => value.replace(/\/+$/, '');

const defaultApiUrl = import.meta.env.PROD ? '' : 'http://localhost:8080';

export const env = {
  apiUrl: stripTrailingSlash(
    apiUrlFromEnv && apiUrlFromEnv.length > 0 ? apiUrlFromEnv : defaultApiUrl,
  ),
};
