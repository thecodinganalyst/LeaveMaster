import type { Metadata } from 'next';
import './globals.css';
import { Footer } from '@/components/Footer';
import { Navigation } from '@/components/Navigation';

import { siteUrl } from '@/lib/site';

const googleSiteVerification = process.env.NEXT_PUBLIC_GOOGLE_SITE_VERIFICATION?.trim();
const bingSiteVerification = process.env.NEXT_PUBLIC_BING_SITE_VERIFICATION?.trim();

export const metadata: Metadata = {
  verification: {
    ...(googleSiteVerification ? { google: googleSiteVerification } : {}),
    ...(bingSiteVerification ? { other: { 'msvalidate.01': bingSiteVerification } } : {}),
  },
  metadataBase: new URL(siteUrl),
  title: {
    default: 'LeaveMaestro | Smarter employee leave management',
    template: '%s | LeaveMaestro',
  },
  description:
    'LeaveMaestro helps HR teams automate requests, approvals, calendars, and reporting for employee leave management.',
  keywords: ['leave management', 'HR software', 'employee scheduling', 'absence tracking'],
  openGraph: {
    type: 'website',
    url: '/',
    siteName: 'LeaveMaestro',
    title: 'LeaveMaestro | Smarter employee leave management',
    description:
      'LeaveMaestro helps HR teams automate requests, approvals, calendars, and reporting for employee leave management.',
  },
  twitter: {
    card: 'summary',
    title: 'LeaveMaestro | Smarter employee leave management',
    description:
      'LeaveMaestro helps HR teams automate requests, approvals, calendars, and reporting for employee leave management.',
  },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>
        <Navigation />
        <main className="page-shell">{children}</main>
        <Footer />
      </body>
    </html>
  );
}
