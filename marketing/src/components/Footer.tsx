import Link from 'next/link';
import { LeaveMaestroLogo } from '@/components/LeaveMaestroLogo';

export function Footer() {
  return (
    <footer className="border-t border-slate-200 bg-white">
      <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-10 text-sm text-slate-600 sm:px-6 md:flex-row md:items-center md:justify-between lg:px-8">
        <div>
          <Link href="/" aria-label="LeaveMaestro home" className="inline-flex rounded-lg focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-600">
            <LeaveMaestroLogo />
          </Link>
          <p className="mt-2">Leave management for employees, managers, and HR teams.</p>
        </div>
        <nav className="flex flex-wrap gap-4" aria-label="Footer">
          <Link href="/features" className="hover:text-brand-600">Features</Link>
          <Link href="/demo" className="hover:text-brand-600">Demo</Link>
          <Link href="/contact" className="hover:text-brand-600">Contact</Link>
          <Link href="/privacy" className="hover:text-brand-600">Privacy</Link>
          <Link href="/terms" className="hover:text-brand-600">Terms</Link>
        </nav>
        <p>© {new Date().getFullYear()} LeaveMaestro. All rights reserved.</p>
      </div>
    </footer>
  );
}
