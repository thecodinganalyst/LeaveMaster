const apiUrlFromEnv = import.meta.env.VITE_API_URL?.trim();
const docsUrlFromEnv = import.meta.env.VITE_DOCS_BASE_URL?.trim();

const stripTrailingSlash = (value: string) => value.replace(/\/+$/, '');

const defaultApiUrl = import.meta.env.PROD ? '' : 'http://localhost:8080';
const defaultDocsBaseUrl = 'https://thecodinganalyst.github.io/LeaveMaster';

export const env = {
  apiUrl: stripTrailingSlash(
    apiUrlFromEnv && apiUrlFromEnv.length > 0 ? apiUrlFromEnv : defaultApiUrl,
  ),
  docsBaseUrl: stripTrailingSlash(
    docsUrlFromEnv && docsUrlFromEnv.length > 0 ? docsUrlFromEnv : defaultDocsBaseUrl,
  ),
};
