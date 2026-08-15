import type { Metadata } from 'next';
import { CTAButton } from '@/components/CTAButton';

const demoUrl = process.env.NEXT_PUBLIC_DEMO_URL ?? 'https://demo.leavemaestro.com';

export const metadata: Metadata = {
  title: 'Demo',
  description: 'Learn what the LeaveMaestro demo is designed to show across employee, manager, and HR leave-management workflows.',
};

export default function DemoPage() {
  return (
    <section className="section">
      <div className="grid gap-10 lg:grid-cols-[0.95fr_1.05fr] lg:items-start">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Product demo</p>
          <h1 className="mt-3 text-4xl font-bold tracking-tight">Explore LeaveMaestro from the perspective of each role</h1>
          <p className="mt-5 text-lg">
            The public demo experience is intended to let prospective customers explore representative employee, manager, and HR workflows without creating a tenant.
          </p>
          <div className="mt-8 space-y-4">
            {[
              ['Employee', 'Submit leave requests, review personal applications, and check leave balances.'],
              ['Manager', 'Review requests for assigned staff and make approval decisions.'],
              ['HR', 'Explore leave types, entitlement policies, staff, locations, approvers, and administration.'],
            ].map(([role, description]) => (
              <div key={role} className="rounded-2xl border border-slate-200 bg-white px-5 py-4 shadow-soft">
                <p className="font-semibold text-slate-900">{role}</p>
                <p className="mt-1 text-sm text-slate-600">{description}</p>
              </div>
            ))}
          </div>
          <div className="mt-8 flex flex-col gap-4 sm:flex-row">
            <CTAButton href={demoUrl}>Open Demo</CTAButton>
            <CTAButton href="/contact" variant="secondary">Request a Walkthrough</CTAButton>
          </div>
          <p className="mt-4 text-sm text-slate-500">
            Demo availability and persona-based access depend on the configured public demo environment.
          </p>
        </div>

        <div className="card bg-slate-950 text-white">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-200">What the demo is for</p>
          <h2 className="mt-3 text-3xl font-bold text-white">See the workflow, not just screenshots</h2>
          <p className="mt-4 text-slate-300">
            LeaveMaestro is designed around tenant-aware permissions. A public demo can showcase the same role boundaries so you can understand what employees, approvers, and HR teams each see.
          </p>
          <div className="mt-8 grid gap-4 sm:grid-cols-2">
            <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
              <p className="font-semibold text-white">No tenant setup</p>
              <p className="mt-2 text-sm text-slate-300">The planned public sandbox uses prepared demo data instead of asking visitors to configure a company first.</p>
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
              <p className="font-semibold text-white">Representative data</p>
              <p className="mt-2 text-sm text-slate-300">The demo is intended to use fictional staff, leave balances, applications, and policies suitable for product evaluation.</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
