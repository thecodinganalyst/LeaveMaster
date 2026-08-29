import Link from 'next/link';
import { LeaveMaestroLogo } from '@/components/LeaveMaestroLogo';

const links = [
  { href: '/', label: 'Home' },
  { href: '/features', label: 'Features' },
  { href: '/demo', label: 'Demo' },
  { href: '/pricing', label: 'Pricing' },
  { href: '/contact', label: 'Contact' },
  { href: '/privacy', label: 'Privacy' },
  { href: '/terms', label: 'Terms' },
];

export function Navigation() {
  return (
    <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/95 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3 sm:px-6 lg:px-8">
        <Link href="/" aria-label="LeaveMaestro home" className="rounded-lg focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-600">
          <LeaveMaestroLogo />
        </Link>

        <nav className="hidden items-center gap-6 md:flex" aria-label="Primary">
          {links.map((link) => (
            <Link key={link.href} href={link.href} className="text-sm font-medium text-slate-600 hover:text-brand-700">
              {link.label}
            </Link>
          ))}
        </nav>

        <details className="md:hidden">
          <summary className="cursor-pointer list-none rounded-full border border-slate-300 px-4 py-2 text-sm font-medium text-slate-900 marker:hidden hover:border-brand-300 hover:text-brand-700">
            Menu
          </summary>
          <nav
            aria-label="Mobile"
            className="absolute right-4 top-16 w-56 rounded-2xl border border-slate-200 bg-white p-4 shadow-2xl"
          >
            <ul className="space-y-3">
              {links.map((link) => (
                <li key={link.href}>
                  <Link href={link.href} className="block text-sm font-medium text-slate-600 hover:text-brand-700">
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>
        </details>
      </div>
    </header>
  );
}
