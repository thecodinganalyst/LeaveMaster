const apiUrlFromEnv = import.meta.env.VITE_API_URL?.trim();

export const env = {
  apiUrl: apiUrlFromEnv && apiUrlFromEnv.length > 0 ? apiUrlFromEnv : 'http://localhost:8080/api',
};
