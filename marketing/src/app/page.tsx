import type { Metadata } from 'next';
import { CTAButton } from '@/components/CTAButton';
import { ManualCostCalculator } from '@/components/ManualCostCalculator';

const policyWorkflow = [
  ['1', 'Start with the employee context', 'Choose the employee jurisdiction and staff details that drive the applicable leave setup.'],
  ['2', 'Apply the right policies', 'Use configured leave types, eligibility rules, and entitlement policies for that jurisdiction.'],
  ['3', 'Calculate the entitlement', 'Apply the configured entitlement amount, proration, accrual, and carry-forward rules where relevant.'],
  ['4', 'Keep calendars aligned', 'Use the configured leave calendar and public holidays for the employee jurisdiction.'],
  ['5', 'Let HR review instead of rebuild', 'Keep the rules in LeaveMaestro so HR can review and adjust without returning to spreadsheet formulas.'],
];

const comparisonRows = [
  ['Spreadsheet formulas', 'Configured entitlement and eligibility rules'],
  ['Manual eligibility checks', 'Consistent policy application'],
  ['Hand-calculated proration', 'Configured proration rules'],
  ['Separate holiday lists', 'Jurisdiction-aware leave calendars'],
  ['Email or chat approvals', 'Structured approval workflows'],
  ['HR answers routine balance questions', 'Employee leave self-service'],
  ['Policy knowledge lives with individuals', 'Leave rules live in the system'],
];

const valuePillars = [
  {
    title: 'Rules, not spreadsheets',
    description: 'Model how your organisation grants leave with reusable leave types, eligibility, entitlement, proration, accrual, and carry-forward rules.',
  },
  {
    title: 'Built around jurisdictions',
    description: 'Connect staff to the leave policies and calendars configured for where they work instead of maintaining separate offline rulebooks.',
  },
  {
    title: 'Flexible without becoming manual',
    description: 'Start from reusable policy configuration, adapt it to the organisation, and keep necessary adjustments inside the same leave-management workflow.',
  },
];

export const metadata: Metadata = {
  title: 'Policy-aware leave management for growing companies',
  description:
    'LeaveMaestro helps growing companies replace spreadsheet-based leave administration with jurisdiction-aware leave policies, entitlements, eligibility, proration, calendars, self-service, and approvals.',
};

export default function HomePage() {
  return (
    <>
      <section className="bg-slate-950">
        <div className="section pt-20 sm:pt-24">
          <div className="grid gap-12 lg:grid-cols-[1.08fr_0.92fr] lg:items-start">
            <div>
              <p className="mb-4 inline-flex rounded-full border border-brand-100 bg-brand-50 px-4 py-2 text-sm font-medium text-brand-700">
                Leave management without the spreadsheet
              </p>
              <h1 className="max-w-3xl text-4xl font-bold tracking-tight text-white sm:text-5xl lg:text-6xl">
                Turn your leave policies into rules that run themselves.
              </h1>
              <p className="mt-6 max-w-2xl text-lg text-slate-300">
                LeaveMaestro brings employee entitlements, eligibility, proration, public holidays, balances, and approvals into one policy-aware workflow—so HR spends less time calculating leave and correcting records manually.
              </p>
              <p className="mt-4 max-w-2xl text-sm leading-6 text-slate-400">
                Built for growing companies that have outgrown spreadsheet-based leave management and need a clearer way to apply leave rules consistently.
              </p>
              <div className="mt-8 flex flex-col gap-4 sm:flex-row">
                <CTAButton href="/demo">Try Demo</CTAButton>
                <CTAButton href="/features" variant="secondary">See how it works</CTAButton>
              </div>
            </div>

            <div className="card bg-white/95">
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">From policy to entitlement</p>
              <div className="mt-6 space-y-4">
                {policyWorkflow.map(([step, title, description]) => (
                  <div key={step} className="rounded-2xl border border-slate-200 bg-white p-4">
                    <div className="flex gap-4">
                      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-brand-50 font-semibold text-brand-700">{step}</span>
                      <div>
                        <p className="font-semibold text-slate-900">{title}</p>
                        <p className="mt-1 text-sm text-slate-600">{description}</p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="max-w-3xl">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Why change?</p>
          <h2 className="mt-3 text-3xl font-bold tracking-tight">As leave rules grow, the spreadsheet becomes part of the problem</h2>
          <p className="mt-4 text-lg">
            Leave administration is more than collecting a request. HR still has to know which policy applies, whether the employee qualifies, how much leave to grant, whether it should be prorated, and which calendar applies.
          </p>
        </div>

        <div className="mt-10 overflow-hidden rounded-[2rem] border border-slate-200 bg-white">
          <div className="grid grid-cols-2 bg-slate-950 px-5 py-4 text-sm font-semibold text-white sm:px-8">
            <span>Before LeaveMaestro</span>
            <span>With LeaveMaestro</span>
          </div>
          {comparisonRows.map(([before, after]) => (
            <div key={before} className="grid grid-cols-2 gap-4 border-t border-slate-200 px-5 py-4 text-sm sm:px-8 sm:text-base">
              <span className="text-slate-600">{before}</span>
              <span className="font-medium text-slate-900">{after}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="section pt-0">
        <div className="rounded-[2rem] bg-slate-50 p-8 sm:p-10">
          <div className="max-w-3xl">
            <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Why LeaveMaestro?</p>
            <h2 className="mt-3 text-3xl font-bold tracking-tight">Make complicated leave policies manageable</h2>
            <p className="mt-4 text-lg">
              The goal is not just a better leave-request form. It is confidence that the right rules are being applied to the right employee.
            </p>
          </div>
          <div className="mt-8 grid gap-6 lg:grid-cols-3">
            {valuePillars.map((pillar) => (
              <article key={pillar.title} className="card border-0">
                <h3 className="text-xl font-semibold text-slate-900">{pillar.title}</h3>
                <p className="mt-3">{pillar.description}</p>
              </article>
            ))}
          </div>
          <div className="mt-8">
            <CTAButton href="/features" variant="secondary">Explore the capabilities</CTAButton>
          </div>
        </div>
      </section>

      <section className="section pt-0">
        <div className="mb-8 max-w-3xl">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Why now?</p>
          <h2 className="mt-3 text-3xl font-bold tracking-tight">Manual leave administration has a measurable cost</h2>
          <p className="mt-4 text-lg">
            Put a number on the HR time currently spent interpreting policies, checking entitlements, correcting balances, and answering routine leave questions.
          </p>
        </div>
        <ManualCostCalculator />
      </section>

      <section className="section pt-0">
        <div className="rounded-[2rem] bg-slate-950 px-8 py-10 text-white sm:px-10">
          <div className="max-w-3xl">
            <h2 className="text-3xl font-bold text-white">See whether LeaveMaestro fits the way your organisation manages leave</h2>
            <p className="mt-4 text-slate-300">
              Explore the demo or talk to us about your leave policies, jurisdictions, entitlement rules, and rollout needs.
            </p>
            <div className="mt-8 flex flex-col gap-4 sm:flex-row">
              <CTAButton href="/demo">Try Demo</CTAButton>
              <CTAButton href="/contact" variant="secondary">Contact Us</CTAButton>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
