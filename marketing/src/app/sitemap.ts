import type { MetadataRoute } from 'next';
import { siteUrl } from '@/lib/site';

const publicRoutes = ['/', '/features', '/demo', '/contact', '/privacy', '/terms', '/leave-management', '/singapore-leave-management', '/open-source-leave-management', '/ai-leave-assistant', '/multi-jurisdiction-leave-management', '/leave-entitlements', '/approval-workflows', '/employee-leave-calendar'];

export default function sitemap(): MetadataRoute.Sitemap {
  return publicRoutes.map((route) => ({
    url: `${siteUrl}${route}`,
    changeFrequency: route === '/' ? 'weekly' : 'monthly',
    priority: route === '/' ? 1 : route === '/features' ? 0.9 : 0.6,
  }));
}
