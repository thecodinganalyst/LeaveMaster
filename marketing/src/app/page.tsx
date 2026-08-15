import type { Metadata } from 'next';
import { CTAButton } from '@/components/CTAButton';

const featureHighlights = [
  {
    title: 'Simple leave applications',
    description: 'Employees can submit leave requests, review their application history, and keep track of available leave balances from one place.',
  },
  {
    title: 'Manager approval workflows',
    description: 'Configured approvers can review requests for the staff they are responsible for and approve or reject them with clear status tracking.',
  },
  {
    title: 'Policy-driven entitlements',
    description: 'HR teams can manage leave types, entitlement policies, eligibility, proration, accrual, carry-forward, and employee-level balances.',
  },
];

const audiences = [
  {
    title: 'For employees',
    description: 'Apply for leave, check balances, and see the status and history of your own requests without chasing HR for updates.',
  },
  {
    title: 'For managers',
    description: 'Review leave requests for your team and make approval decisions through a consistent, permission-controlled workflow.',
  },
  {
    title: 'For HR teams',
    description: 'Configure leave types, policies, locations, staff entitlements, approvers, and tenant-level administration from a centralized system.',
  },
];

export const metadata: Metadata = {
  title: 'Leave management for employees, managers and HR',
  description:
    'LeaveMaestro is a multi-tenant leave management platform for employee requests, manager approvals, policy-driven entitlements, balances, roles, and HR administration.',
};

export default function HomePage() {
  return (
    <>
      <section className="bg-slate-950">
        <div className="section pt-20 sm:pt-24">
          <div className="grid gap-12 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
            <div>
              <p className="mb-4 inline-flex rounded-full border border-brand-100 bg-brand-50 px-4 py-2 text-sm font-medium text-brand-700">
                Leave management built for growing organizations
              </p>
              <h1 className="max-w-3xl text-4xl font-bold tracking-tight text-white sm:text-5xl lg:text-6xl">
                Make employee leave easier to request, approve, and manage.
              </h1>
              <p className="mt-6 max-w-2xl text-lg text-slate-300">
                LeaveMaestro brings leave applications, approvals, balances, entitlement policies, and HR administration into one secure multi-tenant platform.
              </p>
              <div className="mt-8 flex flex-col gap-4 sm:flex-row">
                <CTAButton href="/demo">Try Demo</CTAButton>
                <CTAButton href="/contact" variant="secondary">
                  Contact Us
                </CTAButton>
              </div>
            </div>

            <div className="card bg-white/95">
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">One connected workflow</p>
              <div className="mt-6 space-y-4">
                {[
                  ['1', 'Employee requests leave', 'The employee chooses a leave type, dates, and submits the request.'],
                  ['2', 'Manager reviews the request', 'The configured approver sees the request and records an approval decision.'],
                  ['3', 'HR keeps policies and balances aligned', 'Entitlements, leave types, locations, staff records, and permissions stay centrally managed.'],
                ].map(([step, title, description]) => (
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
        <div className="mb-10 max-w-3xl">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Core capabilities</p>
          <h2 className="mt-3 text-3xl font-bold tracking-tight">A clearer way to run day-to-day leave operations</h2>
          <p className="mt-4 text-lg">
            Replace fragmented leave tracking with workflows that connect employees, approvers, and HR around the same records and rules.
          </p>
        </div>
        <div className="grid gap-6 md:grid-cols-3">
          {featureHighlights.map((feature) => (
            <article key={feature.title} className="card">
              <h3 className="text-xl font-semibold">{feature.title}</h3>
              <p className="mt-3">{feature.description}</p>
            </article>
          ))}
        </div>
        <div className="mt-8">
          <CTAButton href="/features" variant="secondary">Explore all features</CTAButton>
        </div>
      </section>

      <section className="section pt-0">
        <div className="rounded-[2rem] bg-slate-50 p-8 sm:p-10">
          <div className="max-w-3xl">
            <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Designed around each role</p>
            <h2 className="mt-3 text-3xl font-bold tracking-tight">Useful for employees, managers, and HR</h2>
          </div>
          <div className="mt-8 grid gap-6 lg:grid-cols-3">
            {audiences.map((audience) => (
              <article key={audience.title} className="card border-0">
                <h3 className="text-xl font-semibold text-slate-900">{audience.title}</h3>
                <p className="mt-3">{audience.description}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="section pt-0">
        <div className="rounded-[2rem] bg-slate-950 px-8 py-10 text-white sm:px-10">
          <div className="max-w-3xl">
            <h2 className="text-3xl font-bold text-white">See how LeaveMaestro fits your leave process</h2>
            <p className="mt-4 text-slate-300">
              Explore the demo experience or contact us to discuss your organization, leave policies, and rollout needs.
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
