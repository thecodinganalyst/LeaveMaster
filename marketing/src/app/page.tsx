import type { Metadata } from 'next';
import { CTAButton } from '@/components/CTAButton';
import { ManualCostCalculator } from '@/components/ManualCostCalculator';
import { FeatureIcon, HeroProductVisual, LeaveWorkflowVisual } from '@/components/ProductVisuals';
import { ApprovalSnapshot, AskMaestroSnapshot, LeaveApplicationSnapshot, PolicyBuilderSnapshot } from '@/components/ProductSnapshots';

const capabilities = [
  ['policy', 'Policy automation', 'Model eligibility, entitlement, proration, accrual, and carry-forward rules.'],
  ['selfService', 'Employee self-service', 'Give staff one place for balances, requests, history, and leave information.'],
  ['approval', 'Predictable approvals', 'Route requests to configured approvers with clear role-based access.'],
  ['jurisdiction', 'Jurisdiction-aware', 'Keep policies, public holidays, and calendars aligned to where staff work.'],
  ['assistant', 'Ask LeaveMaestro', 'Explain leave data and policy outcomes using authorised information.'],
] as const;

export const metadata: Metadata = {
  title: 'Policy-aware leave management for growing companies',
  description: 'LeaveMaestro is open-source leave-management software with a request-only hosted evaluation environment for testing using non-production data.',
};

export default function HomePage() {
  return (
    <>
      <section className="bg-white">
        <div className="section pt-16 sm:pt-20">
          <div className="grid gap-12 lg:grid-cols-[0.95fr_1.05fr] lg:items-center">
            <div>
              <p className="mb-4 inline-flex rounded-full border border-brand-100 bg-brand-50 px-4 py-2 text-sm font-medium text-brand-700">Open-source leave management without the spreadsheet</p>
              <h1 className="max-w-3xl text-4xl font-bold tracking-tight text-slate-950 sm:text-5xl lg:text-6xl">Turn leave policies into rules that run themselves.</h1>
              <p className="mt-6 max-w-xl text-lg text-slate-600">Policies, entitlements, requests, approvals, and balances in one jurisdiction-aware workflow. Self-host LeaveMaestro or request temporary access to the project-hosted evaluation environment.</p>
              <div className="mt-8 flex flex-col gap-4 sm:flex-row"><CTAButton href="/demo">Request Evaluation Access</CTAButton><CTAButton href="/features" variant="secondary">Explore Features</CTAButton></div>
            </div>
            <HeroProductVisual />
          </div>
        </div>
      </section>

      <section className="section pt-4">
        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-950" role="note">
          <strong>Hosted access is for evaluation only.</strong> It is request-only, temporary, and not for production use. Use fictional or anonymised data only; hosted data may be deleted at any time.
        </div>
      </section>

      <section className="section pt-4">
        <div className="mb-7 text-center"><p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">How it works</p><h2 className="mt-3 text-3xl font-bold tracking-tight">From policy to balance</h2></div>
        <LeaveWorkflowVisual />
      </section>

      <section className="section pt-4">
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-5">
          {capabilities.map(([kind, title, description]) => <article key={title} className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm"><FeatureIcon kind={kind} /><h3 className="mt-5 text-lg font-semibold text-slate-900">{title}</h3><p className="mt-2 text-sm leading-6 text-slate-600">{description}</p></article>)}
        </div>
      </section>

      <section className="section pt-4">
        <div className="mb-10 max-w-2xl"><p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Inside LeaveMaestro</p><h2 className="mt-3 text-3xl font-bold tracking-tight">See the workflow before you evaluate it</h2><p className="mt-3 text-slate-600">Representative product snapshots show how policy configuration becomes everyday employee and manager actions.</p></div>
        <div className="space-y-16">
          <div className="grid gap-8 lg:grid-cols-[0.8fr_1.2fr] lg:items-center"><div><p className="text-sm font-semibold text-brand-700">01 · Configure the rules</p><h3 className="mt-2 text-2xl font-bold">Model the entitlement, not a spreadsheet formula</h3><p className="mt-3 text-slate-600">Keep jurisdiction, entitlement and proration settings together in the policy.</p></div><PolicyBuilderSnapshot /></div>
          <div className="grid gap-8 lg:grid-cols-[1.2fr_0.8fr] lg:items-center"><LeaveApplicationSnapshot /><div><p className="text-sm font-semibold text-brand-700">02 · Employee self-service</p><h3 className="mt-2 text-2xl font-bold">Show the impact before submitting</h3><p className="mt-3 text-slate-600">Employees can see the request duration and resulting balance in one place.</p></div></div>
          <div className="grid gap-8 lg:grid-cols-[0.8fr_1.2fr] lg:items-center"><div><p className="text-sm font-semibold text-brand-700">03 · Predictable approvals</p><h3 className="mt-2 text-2xl font-bold">Give managers a focused approval inbox</h3><p className="mt-3 text-slate-600">Requests stay tied to the right staff and approver workflow.</p></div><ApprovalSnapshot /></div>
          <div className="grid gap-8 lg:grid-cols-[1.2fr_0.8fr] lg:items-center"><AskMaestroSnapshot /><div><p className="text-sm font-semibold text-brand-700">04 · Explain the outcome</p><h3 className="mt-2 text-2xl font-bold">Ask why an entitlement looks the way it does</h3><p className="mt-3 text-slate-600">Ask LeaveMaestro can explain authorised leave data using the policy context behind it.</p></div></div>
        </div>
      </section>

      <section className="section pt-4">
        <div className="mb-8 max-w-2xl"><p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Why now?</p><h2 className="mt-3 text-3xl font-bold tracking-tight">Put a number on manual leave administration</h2></div>
        <ManualCostCalculator />
      </section>

      <section className="section pt-4">
        <div className="rounded-[2rem] border border-brand-100 bg-brand-50 px-8 py-10 sm:px-10"><div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between"><div><h2 className="text-3xl font-bold text-slate-950">Evaluate LeaveMaestro or run it yourself</h2><p className="mt-3 max-w-2xl text-slate-600">Request temporary test access to the hosted environment, or use the Apache-2.0 source to operate your own instance.</p></div><div className="flex flex-col gap-3 sm:flex-row"><CTAButton href="/demo">Request Evaluation Access</CTAButton><CTAButton href="https://github.com/thecodinganalyst/LeaveMaster" variant="secondary">View Source</CTAButton></div></div></div>
      </section>
    </>
  );
}
