import type { Metadata } from 'next';
import { CTAButton } from '@/components/CTAButton';

const demoUrl = process.env.NEXT_PUBLIC_DEMO_URL ?? 'https://demo.leavemaestro.com';
const appUrl = process.env.NEXT_PUBLIC_APP_URL ?? 'https://app.leavemaestro.com';

const featureHighlights = [
  {
    title: 'Guided leave requests',
    description: 'Give employees a clear, self-serve workflow for requesting vacation, sick leave, and custom policies.',
  },
  {
    title: 'Manager approvals',
    description: 'Route requests to the right approvers with visibility into team coverage before decisions are made.',
  },
  {
    title: 'Live team calendars',
    description: 'Keep everyone aligned with calendar views that surface conflicts, public holidays, and capacity gaps.',
  },
];

const testimonials = [
  {
    quote: 'LeaveMaestro replaced our spreadsheet chaos in a week. Managers finally trust the data.',
    author: 'Priya Nair',
    role: 'People Operations Lead, Northstar Studio',
  },
  {
    quote: 'We cut approval turnaround from days to hours and employees know exactly where every request stands.',
    author: 'Daniel Brooks',
    role: 'HR Manager, Harbor Health',
  },
];

export const metadata: Metadata = {
  title: 'Home',
  description: 'Discover LeaveMaestro, the employee leave management platform built to simplify requests, approvals, and reporting.',
};

export default function HomePage() {
  return (
    <>
      <section className="bg-slate-950">
        <div className="section pt-20 sm:pt-24">
          <div className="grid gap-12 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
            <div>
              <p className="mb-4 inline-flex rounded-full border border-brand-100 bg-brand-50 px-4 py-2 text-sm font-medium text-brand-700">
                Built for modern HR, operations, and people teams
              </p>
              <h1 className="max-w-3xl text-4xl font-bold tracking-tight text-white sm:text-5xl lg:text-6xl">
                Take the friction out of employee leave management.
              </h1>
              <p className="mt-6 max-w-2xl text-lg text-slate-300">
                LeaveMaestro centralizes leave requests, approvals, policy tracking, and reporting so every team can plan with confidence.
              </p>
              <div className="mt-8 flex flex-col gap-4 sm:flex-row">
                <CTAButton href={demoUrl}>Try Demo</CTAButton>
                <CTAButton href="/contact" variant="secondary">
                  Contact Us
                </CTAButton>
              </div>
              <dl className="mt-10 grid gap-6 sm:grid-cols-3">
                <div>
                  <dt className="text-sm font-medium text-slate-400">Requests processed</dt>
                  <dd className="mt-2 text-3xl font-semibold text-white">50k+</dd>
                </div>
                <div>
                  <dt className="text-sm font-medium text-slate-400">Approval turnaround</dt>
                  <dd className="mt-2 text-3xl font-semibold text-white">3x faster</dd>
                </div>
                <div>
                  <dt className="text-sm font-medium text-slate-400">Team adoption</dt>
                  <dd className="mt-2 text-3xl font-semibold text-white">94%</dd>
                </div>
              </dl>
            </div>

            <div className="card bg-white/95">
              <div className="rounded-3xl bg-slate-950 p-6 text-white">
                <p className="text-sm font-medium text-brand-200">Coverage snapshot</p>
                <div className="mt-6 space-y-4">
                  {[
                    ['Engineering', '2 planned absences', 'Healthy coverage'],
                    ['Support', '1 pending request', 'Awaiting approval'],
                    ['Finance', '0 conflicts', 'Fully staffed'],
                  ].map(([team, summary, status]) => (
                    <div key={team} className="rounded-2xl border border-white/10 bg-white/5 p-4">
                      <div className="flex items-center justify-between gap-3">
                        <div>
                          <p className="font-semibold text-white">{team}</p>
                          <p className="text-sm text-slate-300">{summary}</p>
                        </div>
                        <span className="rounded-full bg-emerald-500/15 px-3 py-1 text-xs font-semibold text-emerald-300">
                          {status}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
                <div className="mt-6 rounded-2xl bg-brand-600 p-4">
                  <p className="text-sm text-brand-100">Ready to see the full workflow?</p>
                  <a href={appUrl} className="mt-2 inline-block text-base font-semibold text-white hover:text-brand-100">
                    Launch product →
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="mb-10 max-w-3xl">
          <h2 className="text-3xl font-bold tracking-tight">Everything you need to run leave operations smoothly</h2>
          <p className="mt-4 text-lg">
            Replace scattered spreadsheets and manual follow-ups with one clean workflow from request through reporting.
          </p>
        </div>
        <div className="grid gap-6 md:grid-cols-3">
          {featureHighlights.map((feature) => (
            <article key={feature.title} className="card">
              <div className="mb-4 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-50 text-brand-700">
                ✦
              </div>
              <h3 className="text-xl font-semibold">{feature.title}</h3>
              <p className="mt-3">{feature.description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="section">
        <div className="rounded-[2rem] bg-slate-50 p-8 sm:p-10">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
            <div className="max-w-2xl">
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Trusted by growing teams</p>
              <h2 className="mt-3 text-3xl font-bold tracking-tight">Teams choose LeaveMaestro to keep leave fair, visible, and compliant.</h2>
            </div>
            <div className="grid grid-cols-2 gap-4 text-sm font-semibold text-slate-500 sm:grid-cols-4">
              {['Northstar', 'Harbor Health', 'BluePeak', 'Summit Labs'].map((logo) => (
                <div key={logo} className="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-center">
                  {logo}
                </div>
              ))}
            </div>
          </div>
          <div className="mt-8 grid gap-6 lg:grid-cols-2">
            {testimonials.map((testimonial) => (
              <blockquote key={testimonial.author} className="card border-0">
                <p className="text-lg text-slate-700">“{testimonial.quote}”</p>
                <footer className="mt-6">
                  <p className="font-semibold text-slate-900">{testimonial.author}</p>
                  <p className="text-sm">{testimonial.role}</p>
                </footer>
              </blockquote>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
