const SITE_URL = 'https://leavemaestro.com';
const REPOSITORY_URL = 'https://github.com/thecodinganalyst/LeaveMaster';

export function createSiteStructuredData(siteUrl = SITE_URL) {
  const root = siteUrl.replace(/\/$/, '');

  return [
    {
      '@context': 'https://schema.org',
      '@type': 'Organization',
      '@id': `${root}/#organization`,
      name: 'LeaveMaestro',
      url: root,
      sameAs: [REPOSITORY_URL],
    },
    {
      '@context': 'https://schema.org',
      '@type': 'WebSite',
      '@id': `${root}/#website`,
      name: 'LeaveMaestro',
      url: root,
      publisher: { '@id': `${root}/#organization` },
    },
  ];
}

export function createSoftwareStructuredData(siteUrl = SITE_URL) {
  const root = siteUrl.replace(/\/$/, '');

  return {
    '@context': 'https://schema.org',
    '@type': 'SoftwareApplication',
    '@id': `${root}/#software`,
    name: 'LeaveMaestro',
    url: root,
    applicationCategory: 'BusinessApplication',
    operatingSystem: 'Web',
    description:
      'Open-source leave-management software for policies, entitlements, requests, approvals, balances, and jurisdiction-aware workflows.',
    license: 'https://www.apache.org/licenses/LICENSE-2.0',
    isAccessibleForFree: true,
    codeRepository: REPOSITORY_URL,
    publisher: { '@id': `${root}/#organization` },
  };
}
