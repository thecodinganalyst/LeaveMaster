import type { Metadata } from 'next';
import { CTAButton } from '@/components/CTAButton';

const featureSections = [
  {
    title: 'Employee leave self-service',
    description: 'Give staff a straightforward place to submit leave applications, review their own requests, and see available leave balances.',
    bullets: ['Submit and track leave applications', 'View personal leave balances and entitlements', 'Access limited to the signed-in employee where required'],
  },
  {
    title: 'Manager approvals',
    description: 'Configured leave approvers can review requests for the employees assigned to them and record approval decisions.',
    bullets: ['Approver-based request visibility', 'Approve or reject eligible requests', 'Backend authorization protects approval actions'],
  },
  {
    title: 'Leave types and entitlement policies',
    description: 'Model leave consistently with reusable leave types and policy-driven entitlement rules rather than maintaining balances manually.',
    bullets: ['Jurisdiction-aware leave types', 'Eligibility and deterministic policy resolution', 'Proration, accrual, carry-forward, and reconciliation'],
  },
  {
    title: 'HR administration',
    description: 'Manage the organizational data that leave processing depends on from a centralized tenant administration experience.',
    bullets: ['Staff, locations, leave types, and entitlements', 'Approvers and leave balances', 'Tenant-scoped administration'],
  },
  {
    title: 'Roles and permissions',
    description: 'Control access with tenant-aware role-based permissions enforced by the backend for protected operations.',
    bullets: ['Tenant-specific Staff, Manager, HR, and Admin roles', 'Permission-driven frontend navigation', 'Server-side authorization remains authoritative'],
  },
  {
    title: 'Calendar and leave visibility',
    description: 'Use leave and calendar views to understand approved or scheduled absences without relying on separate spreadsheets.',
    bullets: ['Leave calendar capability', 'Application status visibility', 'Centralized records for planning and administration'],
  },
  {
    title: 'Flexible sign-in options',
    description: 'Support username/password authentication and optional OAuth/OIDC providers for users that have already been provisioned.',
    bullets: ['Username and password', 'Optional Google and Microsoft sign-in', 'Optional GitHub and Facebook sign-in'],
  },
  {
    title: 'Ask LeaveMaestro assistant',
    description: 'An embedded AI assistant can work with authorized LeaveMaestro capabilities while keeping business authorization in the application.',
    bullets: ['Uses the same authorized backend tool contract', 'Write actions require explicit confirmation', 'Authorization is rechecked before execution'],
  },
];

export const metadata: Metadata = {
  title: 'Features',
  description:
    'Explore LeaveMaestro features for employee leave requests, manager approvals, entitlement policies, leave balances, tenant administration, RBAC, calendars, and the optional AI assistant.',
};

export default function FeaturesPage() {
  return (
    <section className="section">
      <div className="max-w-3xl">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Features</p>
        <h1 className="mt-3 text-4xl font-bold tracking-tight">Leave management from employee request to HR administration</h1>
        <p className="mt-5 text-lg">
          LeaveMaestro connects the everyday workflows employees and managers need with the policy, entitlement, and access controls HR needs to manage leave consistently.
        </p>
      </div>

      <div className="mt-12 grid gap-6 lg:grid-cols-2">
        {featureSections.map((feature, index) => (
          <article key={feature.title} className="card">
            <div className="mb-5 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-50 text-lg font-bold text-brand-700">
              {String(index + 1).padStart(2, '0')}
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

      <div className="mt-12 rounded-[2rem] bg-slate-50 p-8 sm:p-10">
        <h2 className="text-3xl font-bold tracking-tight">Built for different responsibilities</h2>
        <div className="mt-8 grid gap-6 lg:grid-cols-3">
          <div className="card border-0">
            <h3 className="text-xl font-semibold">Employees</h3>
            <p className="mt-3">Request leave, view personal balances, and follow the status of your own applications.</p>
          </div>
          <div className="card border-0">
            <h3 className="text-xl font-semibold">Managers</h3>
            <p className="mt-3">Review requests for assigned staff and make approval decisions within the configured approval model.</p>
          </div>
          <div className="card border-0">
            <h3 className="text-xl font-semibold">HR teams</h3>
            <p className="mt-3">Manage leave configuration, entitlements, staff data, locations, approvers, and tenant-level operations.</p>
          </div>
        </div>
      </div>

      <div className="mt-12 rounded-[2rem] bg-slate-950 px-8 py-10 text-white">
        <h2 className="text-3xl font-bold text-white">See LeaveMaestro in action</h2>
        <p className="mt-4 max-w-2xl text-slate-300">
          Explore the demo experience or contact us to discuss the leave workflows and policies your organization needs.
        </p>
        <div className="mt-8 flex flex-col gap-4 sm:flex-row">
          <CTAButton href="/demo">Try Demo</CTAButton>
          <CTAButton href="/contact" variant="secondary">Contact Us</CTAButton>
        </div>
      </div>
    </section>
  );
}
