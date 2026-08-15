import type { Metadata } from 'next';
import { CTAButton } from '@/components/CTAButton';

const featureSections = [
  {
    title: 'Employee self-service',
    description: 'Let employees request vacation, sick leave, parental leave, and custom policies from a simple guided interface.',
    bullets: ['Policy-aware request forms', 'Request history and balances', 'Automatic status notifications'],
  },
  {
    title: 'Manager visibility',
    description: 'Surface staffing impact before a manager approves time away so schedule conflicts are easier to prevent.',
    bullets: ['Team coverage timelines', 'Overlap and conflict detection', 'Approval routing and delegation'],
  },
  {
    title: 'HR operations',
    description: 'Equip HR with policy controls, audit trails, and clean exports for payroll and compliance workflows.',
    bullets: ['Centralized leave policies', 'Audit logs and approval history', 'Reports for payroll and planning'],
  },
  {
    title: 'Calendar sync',
    description: 'Keep everyone informed with shared calendars that make approved leave visible across teams and departments.',
    bullets: ['Shared time-off calendars', 'Public holiday awareness', 'Department and location filtering'],
  },
  {
    title: 'Reporting insights',
    description: 'See leave trends, absenteeism patterns, and team capacity signals before they become operational issues.',
    bullets: ['Leave trend dashboards', 'Department-level summaries', 'Export-ready reports'],
  },
  {
    title: 'Secure access',
    description: 'Support role-based access and controlled visibility so sensitive HR information stays protected.',
    bullets: ['Role-based permissions', 'Approval audit trails', 'Configurable admin controls'],
  },
];

export const metadata: Metadata = {
  title: 'Features',
  description: 'Explore LeaveMaestro features for employee requests, manager approvals, calendars, policy controls, and reporting.',
};

export default function FeaturesPage() {
  return (
    <section className="section">
      <div className="max-w-3xl">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Features</p>
        <h1 className="mt-3 text-4xl font-bold tracking-tight">Built to simplify every step of leave management</h1>
        <p className="mt-5 text-lg">
          LeaveMaestro gives employees, managers, and HR teams a single system to request, approve, track, and analyze leave.
        </p>
      </div>

      <div className="mt-12 grid gap-6 lg:grid-cols-2">
        {featureSections.map((feature, index) => (
          <article key={feature.title} className="card">
            <div className="mb-5 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-50 text-lg font-bold text-brand-700">
              0{index + 1}
            </div>
            <h2 className="text-2xl font-semibold">{feature.title}</h2>
            <p className="mt-3">{feature.description}</p>
            <ul className="mt-5 space-y-3 text-sm text-slate-600">
              {feature.bullets.map((bullet) => (
                <li key={bullet} className="flex gap-3">
                  <span className="mt-1 text-brand-600">•</span>
                  <span>{bullet}</span>
                </li>
              ))}
            </ul>
          </article>
        ))}
      </div>

      <div className="mt-12 rounded-[2rem] bg-slate-950 px-8 py-10 text-white">
        <h2 className="text-3xl font-bold text-white">See LeaveMaestro in action</h2>
        <p className="mt-4 max-w-2xl text-slate-300">
          Explore the demo experience or speak with our team about the workflows your organization needs most.
        </p>
        <div className="mt-8 flex flex-col gap-4 sm:flex-row">
          <CTAButton href="/demo">Try Demo</CTAButton>
          <CTAButton href="/contact" variant="secondary">
            Talk to Sales
          </CTAButton>
        </div>
      </div>
    </section>
  );
}
