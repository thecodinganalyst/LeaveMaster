const apiUrlFromEnv = import.meta.env.VITE_API_URL?.trim();

const stripTrailingSlash = (value: string) => value.replace(/\/+$/, '');

export const env = {
  apiUrl: stripTrailingSlash(
    apiUrlFromEnv && apiUrlFromEnv.length > 0 ? apiUrlFromEnv : 'http://localhost:8080',
  ),
};
