import type { Metadata } from 'next';
import { CTAButton } from '@/components/CTAButton';
import { FeatureIcon, LeaveWorkflowVisual } from '@/components/ProductVisuals';
import { EmployeeDashboardSnapshot, JurisdictionSnapshot, PolicyBuilderSnapshot } from '@/components/ProductSnapshots';

const features = [
  ['policy', 'Policy automation', 'Configure leave types, eligibility, entitlement, proration, accrual, and carry-forward rules.', ['Reusable policy rules', 'Employee-level balances']],
  ['selfService', 'Employee self-service', 'Let staff apply for leave, review history, and see their own entitlements and balances.', ['Applications & status', 'Personal leave balances']],
  ['approval', 'Approvals', 'Give configured approvers a clear view of the requests they are responsible for.', ['Approve or reject', 'Permission-controlled access']],
  ['jurisdiction', 'Jurisdiction-aware setup', 'Keep staff connected to the relevant policy configuration, calendars, and public holidays.', ['Locations & jurisdictions', 'Leave calendars']],
] as const;

export const metadata: Metadata = {
  title: 'Leave policy automation and entitlement management features',
  description: 'Explore LeaveMaestro capabilities for leave entitlement management, eligibility, proration, jurisdiction-aware calendars, employee self-service, approvals, and HR administration.',
};

export default function FeaturesPage() {
  return (
    <section className="section">
      <div className="grid gap-10 lg:grid-cols-[0.85fr_1.15fr] lg:items-center">
        <div><p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Features</p><h1 className="mt-3 text-4xl font-bold tracking-tight">Manage the rules behind leave—not just the request form</h1><p className="mt-5 max-w-xl text-lg">A connected leave workflow for HR, employees, and approvers.</p></div>
        <LeaveWorkflowVisual />
      </div>

      <div className="mt-14 grid gap-6 lg:grid-cols-2">
        {features.map(([kind, title, description, bullets]) => (
          <article key={title} className="card"><div className="flex items-start gap-4"><FeatureIcon kind={kind} /><div><h2 className="text-2xl font-semibold">{title}</h2><p className="mt-2">{description}</p></div></div><div className="mt-6 grid gap-3 sm:grid-cols-2">{bullets.map((bullet) => <div key={bullet} className="rounded-xl bg-brand-50 px-4 py-3 text-sm font-medium text-slate-700">✓ {bullet}</div>)}</div></article>
        ))}
      </div>

      <div className="mt-16">
        <div className="max-w-2xl"><p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Product snapshots</p><h2 className="mt-3 text-3xl font-bold tracking-tight">A closer look at the day-to-day experience</h2><p className="mt-3 text-slate-600">Representative UI previews keep the focus on the workflows LeaveMaestro already supports.</p></div>
        <div className="mt-8 grid gap-6 lg:grid-cols-2"><EmployeeDashboardSnapshot /><PolicyBuilderSnapshot /><div className="lg:col-span-2"><JurisdictionSnapshot /></div></div>
      </div>

      <div className="mt-12 grid gap-5 rounded-[2rem] bg-slate-50 p-8 sm:grid-cols-3 sm:p-10">
        <div><FeatureIcon kind="approval" /><h3 className="mt-4 text-lg font-semibold">Controlled access</h3><p className="mt-2 text-sm">Roles and permissions keep actions scoped to the right users.</p></div>
        <div><FeatureIcon kind="selfService" /><h3 className="mt-4 text-lg font-semibold">Flexible sign-in</h3><p className="mt-2 text-sm">Username/password plus supported OAuth/OIDC options for provisioned users.</p></div>
        <div><FeatureIcon kind="assistant" /><h3 className="mt-4 text-lg font-semibold">Ask LeaveMaestro</h3><p className="mt-2 text-sm">Help authorised users understand LeaveMaestro data and policy outcomes.</p></div>
      </div>

      <div className="mt-12 rounded-[2rem] border border-brand-100 bg-brand-50 px-8 py-10"><div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between"><div><h2 className="text-3xl font-bold text-slate-950">Explore the workflow yourself</h2><p className="mt-3 text-slate-600">Use the demo or talk to us about your policies and jurisdictions.</p></div><div className="flex flex-col gap-3 sm:flex-row"><CTAButton href="/demo">Try Demo</CTAButton><CTAButton href="/contact" variant="secondary">Contact Us</CTAButton></div></div></div>
    </section>
  );
}
