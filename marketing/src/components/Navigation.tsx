import Link from 'next/link';

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
    <header className="sticky top-0 z-30 border-b border-white/10 bg-slate-950/80 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
        <Link href="/" className="text-lg font-semibold tracking-tight text-white">
          LeaveMaestro
        </Link>

        <nav className="hidden items-center gap-6 md:flex" aria-label="Primary">
          {links.map((link) => (
            <Link key={link.href} href={link.href} className="text-sm font-medium text-slate-200 hover:text-white">
              {link.label}
            </Link>
          ))}
        </nav>

        <details className="md:hidden">
          <summary className="cursor-pointer list-none rounded-full border border-white/10 px-4 py-2 text-sm font-medium text-white marker:hidden">
            Menu
          </summary>
          <nav
            aria-label="Mobile"
            className="absolute right-4 top-16 w-56 rounded-2xl border border-slate-800 bg-slate-950 p-4 shadow-2xl"
          >
            <ul className="space-y-3">
              {links.map((link) => (
                <li key={link.href}>
                  <Link href={link.href} className="block text-sm font-medium text-slate-200 hover:text-white">
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
