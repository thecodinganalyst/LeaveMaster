import type { Metadata } from 'next';
import './globals.css';
import { Footer } from '@/components/Footer';
import { Navigation } from '@/components/Navigation';

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? 'http://localhost:3000';

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: {
    default: 'LeaveMaestro | Smarter employee leave management',
    template: '%s | LeaveMaestro',
  },
  description:
    'LeaveMaestro helps HR teams automate requests, approvals, calendars, and reporting for employee leave management.',
  keywords: ['leave management', 'HR software', 'employee scheduling', 'absence tracking'],
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
