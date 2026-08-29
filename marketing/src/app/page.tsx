import type { Metadata } from 'next';
import { CTAButton } from '@/components/CTAButton';
import { ManualCostCalculator } from '@/components/ManualCostCalculator';
import { FeatureIcon, HeroProductVisual, LeaveWorkflowVisual } from '@/components/ProductVisuals';

const capabilities = [
  ['policy', 'Policy automation', 'Model eligibility, entitlement, proration, accrual, and carry-forward rules.'],
  ['selfService', 'Employee self-service', 'Give staff one place for balances, requests, history, and leave information.'],
  ['approval', 'Predictable approvals', 'Route requests to configured approvers with clear role-based access.'],
  ['jurisdiction', 'Jurisdiction-aware', 'Keep policies, public holidays, and calendars aligned to where staff work.'],
  ['assistant', 'Ask LeaveMaestro', 'Explain leave data and policy outcomes using the information authorised users can access.'],
] as const;

export const metadata: Metadata = {
  title: 'Policy-aware leave management for growing companies',
  description:
    'LeaveMaestro helps growing companies replace spreadsheet-based leave administration with jurisdiction-aware leave policies, entitlements, eligibility, proration, calendars, self-service, and approvals.',
};

export default function HomePage() {
  return (
    <>
      <section className="bg-white">
        <div className="section pt-16 sm:pt-20">
          <div className="grid gap-12 lg:grid-cols-[0.95fr_1.05fr] lg:items-center">
            <div>
              <p className="mb-4 inline-flex rounded-full border border-brand-100 bg-brand-50 px-4 py-2 text-sm font-medium text-brand-700">
                Leave management without the spreadsheet
              </p>
              <h1 className="max-w-3xl text-4xl font-bold tracking-tight text-slate-950 sm:text-5xl lg:text-6xl">
                Turn leave policies into rules that run themselves.
              </h1>
              <p className="mt-6 max-w-xl text-lg text-slate-600">
                Policies, entitlements, requests, approvals, and balances in one jurisdiction-aware workflow.
              </p>
              <div className="mt-8 flex flex-col gap-4 sm:flex-row">
                <CTAButton href="/demo">Try Demo</CTAButton>
                <CTAButton href="/features" variant="secondary">Explore Features</CTAButton>
              </div>
            </div>
            <HeroProductVisual />
          </div>
        </div>
      </section>

      <section className="section pt-4">
        <div className="mb-7 text-center">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">How it works</p>
          <h2 className="mt-3 text-3xl font-bold tracking-tight">From policy to balance</h2>
        </div>
        <LeaveWorkflowVisual />
      </section>

      <section className="section pt-4">
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-5">
          {capabilities.map(([kind, title, description]) => (
            <article key={title} className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
              <FeatureIcon kind={kind} />
              <h3 className="mt-5 text-lg font-semibold text-slate-900">{title}</h3>
              <p className="mt-2 text-sm leading-6 text-slate-600">{description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="section pt-4">
        <div className="grid gap-5 lg:grid-cols-2">
          <div className="rounded-[2rem] border border-slate-200 bg-slate-50 p-7 sm:p-9">
            <p className="text-sm font-semibold uppercase tracking-[0.2em] text-slate-500">Before</p>
            <h2 className="mt-3 text-2xl font-bold">Spreadsheets and manual checks</h2>
            <div className="mt-6 grid grid-cols-2 gap-3 text-sm text-slate-600">
              {['Policy memory', 'Manual proration', 'Separate holiday lists', 'Repeated HR questions'].map((item) => (
                <div key={item} className="rounded-xl border border-slate-200 bg-white px-4 py-3">{item}</div>
              ))}
            </div>
          </div>
          <div className="rounded-[2rem] border border-brand-100 bg-brand-50 p-7 sm:p-9">
            <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-700">With LeaveMaestro</p>
            <h2 className="mt-3 text-2xl font-bold">One policy-aware workflow</h2>
            <div className="mt-6 grid grid-cols-2 gap-3 text-sm text-slate-700">
              {['Configured rules', 'Automatic entitlement logic', 'Jurisdiction calendars', 'Employee self-service'].map((item) => (
                <div key={item} className="rounded-xl border border-brand-100 bg-white px-4 py-3">{item}</div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="section pt-4">
        <div className="mb-8 max-w-2xl">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Why now?</p>
          <h2 className="mt-3 text-3xl font-bold tracking-tight">Put a number on manual leave administration</h2>
        </div>
        <ManualCostCalculator />
      </section>

      <section className="section pt-4">
        <div className="rounded-[2rem] border border-brand-100 bg-brand-50 px-8 py-10 sm:px-10">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <h2 className="text-3xl font-bold text-slate-950">See LeaveMaestro in action</h2>
              <p className="mt-3 max-w-2xl text-slate-600">Explore the demo or discuss your leave policies and rollout needs.</p>
            </div>
            <div className="flex flex-col gap-3 sm:flex-row">
              <CTAButton href="/demo">Try Demo</CTAButton>
              <CTAButton href="/contact" variant="secondary">Contact Us</CTAButton>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
