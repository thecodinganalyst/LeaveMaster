import type { MetadataRoute } from 'next';
import { siteUrl } from './layout';

const publicRoutes = ['/', '/features', '/demo', '/contact', '/privacy', '/terms'];

export default function sitemap(): MetadataRoute.Sitemap {
  return publicRoutes.map((route) => ({
    url: `${siteUrl}${route}`,
    changeFrequency: route === '/' ? 'weekly' : 'monthly',
    priority: route === '/' ? 1 : route === '/features' ? 0.9 : 0.6,
  }));
}
