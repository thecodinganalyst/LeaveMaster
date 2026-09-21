import type { MetadataRoute } from 'next';
import { siteUrl } from '@/lib/site';

const publicRoutes = ['/', '/features', '/demo', '/contact', '/privacy', '/terms', '/leave-management', '/singapore-leave-management', '/open-source-leave-management', '/ai-leave-assistant', '/multi-jurisdiction-leave-management', '/leave-entitlements', '/approval-workflows', '/employee-leave-calendar', '/singapore-leave-guides', '/singapore-leave-guides/annual-leave-entitlement', '/singapore-leave-guides/annual-leave-proration', '/singapore-leave-guides/sick-hospitalisation-leave', '/singapore-leave-guides/childcare-leave', '/singapore-leave-guides/infant-care-leave', '/singapore-leave-guides/public-holidays-and-leave', '/singapore-leave-guides/part-time-employee-leave', '/singapore-leave-guides/carry-forward-policies', '/singapore-leave-guides/leave-approval-workflows', '/singapore-leave-guides/moving-from-spreadsheets', '/tools/singapore-annual-leave-calculator'];

export default function sitemap(): MetadataRoute.Sitemap {
  return publicRoutes.map((route) => ({
    url: `${siteUrl}${route}`,
    changeFrequency: route === '/' ? 'weekly' : 'monthly',
    priority: route === '/' ? 1 : route === '/features' ? 0.9 : 0.6,
  }));
}
