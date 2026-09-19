import type { Metadata } from 'next';
import { CTAButton } from '@/components/CTAButton';
import { ApprovalSnapshot, EmployeeDashboardSnapshot, PolicyBuilderSnapshot } from '@/components/ProductSnapshots';

export const metadata: Metadata = {
  alternates: { canonical: '/demo' },
  openGraph: { url: '/demo' },
  title: 'Public Demo',
  description: 'Explore LeaveMaestro instantly as an Employee, Manager, or HR user using a fictional public demo tenant.',
};

const appUrl = (process.env.NEXT_PUBLIC_APP_URL ?? 'https://app.leavemaestro.com').replace(/\/$/, '');

export default function DemoPage() {
  return (
    <section className="section">
      <div className="max-w-3xl">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Public demo</p>
        <h1 className="mt-3 text-4xl font-bold tracking-tight">Explore LeaveMaestro without signing up</h1>
        <p className="mt-5 text-lg">
          Choose a fictional persona and enter the isolated LeaveMaestro demo tenant immediately. No registration or tenant creation is required.
        </p>
      </div>

      <div className="mt-8 rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-950" role="note">
        <strong>Public demo — test data only.</strong> Do not enter real employee, dependant, confidential, or production personal data. Demo data is reset periodically and may be deleted at any time.
      </div>

      <div className="mt-10 grid gap-8 lg:grid-cols-3">
        <div className="card">
          <EmployeeDashboardSnapshot />
          <div className="mt-4">
            <p className="font-semibold text-slate-900">Employee</p>
            <p className="mt-1 text-sm text-slate-500">Apply for leave and review personal balances and history.</p>
            <div className="mt-5"><CTAButton href={`${appUrl}/demo?persona=employee`}>Try as Employee</CTAButton></div>
          </div>
        </div>
        <div className="card">
          <ApprovalSnapshot />
          <div className="mt-4">
            <p className="font-semibold text-slate-900">Manager</p>
            <p className="mt-1 text-sm text-slate-500">Review team requests and exercise approval/rejection workflows.</p>
            <div className="mt-5"><CTAButton href={`${appUrl}/demo?persona=manager`}>Try as Manager</CTAButton></div>
          </div>
        </div>
        <div className="card">
          <PolicyBuilderSnapshot />
          <div className="mt-4">
            <p className="font-semibold text-slate-900">HR</p>
            <p className="mt-1 text-sm text-slate-500">Explore tenant HR administration, staff, policies, calendars, and approvers.</p>
            <div className="mt-5"><CTAButton href={`${appUrl}/demo?persona=hr`}>Try as HR</CTAButton></div>
          </div>
        </div>
      </div>

      <div className="mt-12 grid gap-6 lg:grid-cols-2">
        <div className="card">
          <h2 className="text-2xl font-bold text-slate-950">How the demo works</h2>
          <ul className="mt-5 space-y-3 text-sm text-slate-600">
            <li>Each persona uses a dedicated identity mapped only to the DEMO tenant.</li>
            <li>Normal LeaveMaestro role permissions remain in force.</li>
            <li>Platform administration is not available through public demo identities.</li>
            <li>Use the in-app <strong>Switch persona</strong> action to return to the persona chooser.</li>
          </ul>
          <div className="mt-6"><CTAButton href={`${appUrl}/demo`}>Open Demo Landing</CTAButton></div>
        </div>

        <div className="card border-brand-100 bg-brand-50">
          <h2 className="text-2xl font-bold text-slate-950">Need production control?</h2>
          <p className="mt-4 text-slate-600">
            LeaveMaestro is open-source software under the Apache License 2.0. Organisations that need production use, their own retention policies, backups, availability controls, or operational ownership should deploy and operate their own instance.
          </p>
          <div className="mt-6"><CTAButton href="https://github.com/thecodinganalyst/LeaveMaster" variant="secondary">View Source</CTAButton></div>
        </div>
      </div>
    </section>
  );
}
