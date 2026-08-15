import type { Metadata } from 'next';
import { CTAButton } from '@/components/CTAButton';

const demoUrl = process.env.NEXT_PUBLIC_DEMO_URL ?? 'https://demo.leavemaestro.com';

export const metadata: Metadata = {
  title: 'Demo',
  description: 'Preview the LeaveMaestro leave management experience and explore the end-to-end request and approval workflow.',
};

export default function DemoPage() {
  return (
    <section className="section">
      <div className="grid gap-10 lg:grid-cols-[0.95fr_1.05fr] lg:items-start">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Interactive demo</p>
          <h1 className="mt-3 text-4xl font-bold tracking-tight">Preview the LeaveMaestro workflow before your team adopts it</h1>
          <p className="mt-5 text-lg">
            Explore how employees submit requests, how managers review team coverage, and how HR keeps policies aligned across the business.
          </p>
          <div className="mt-8 space-y-4">
            {[
              'Employee request intake with policy-aware forms',
              'Approver review views with conflict visibility',
              'Live calendar and reporting snapshots for HR teams',
            ].map((item) => (
              <div key={item} className="flex gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-4 shadow-soft">
                <span className="text-brand-600">✓</span>
                <p>{item}</p>
              </div>
            ))}
          </div>
          <div className="mt-8 flex flex-col gap-4 sm:flex-row">
            <CTAButton href={demoUrl}>Launch Demo</CTAButton>
            <CTAButton href="/contact" variant="secondary">
              Request Guided Walkthrough
            </CTAButton>
          </div>
        </div>

        <div className="card overflow-hidden p-3">
          <div className="rounded-3xl border border-slate-200 bg-slate-950 p-4">
            <div className="mb-4 flex items-center gap-2">
              <span className="h-3 w-3 rounded-full bg-rose-400" />
              <span className="h-3 w-3 rounded-full bg-amber-400" />
              <span className="h-3 w-3 rounded-full bg-emerald-400" />
            </div>
            <div className="overflow-hidden rounded-2xl border border-white/10 bg-white">
              <iframe
                title="LeaveMaestro demo preview"
                src={demoUrl}
                className="h-[420px] w-full"
                loading="lazy"
                sandbox="allow-scripts allow-same-origin allow-forms"
              />
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
