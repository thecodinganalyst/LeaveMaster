import type { Metadata } from 'next';
import './globals.css';
import { Footer } from '@/components/Footer';
import { Navigation } from '@/components/Navigation';

import { siteUrl } from '@/lib/site';

export const metadata: Metadata = {
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
