import type { Metadata } from 'next';
import { CTAButton } from '@/components/CTAButton';

const featureSections = [
  {
    title: 'Automate leave-policy administration',
    description: 'Keep the logic behind leave in the system instead of relying on spreadsheet formulas and individual memory.',
    bullets: [
      'Configure leave types, entitlement policies, and eligibility rules',
      'Apply proration, accrual, and carry-forward rules where configured',
      'Use jurisdiction-aware leave configuration and leave calendars',
      'Keep employee-level balances aligned with the configured policy model',
    ],
  },
  {
    title: 'Give employees answers without HR',
    description: 'Let staff see the leave information they need without turning every balance or application-status question into an HR task.',
    bullets: [
      'Submit and track leave applications',
      'View personal leave balances and entitlements',
      'Review application history and current status',
      'Use leave-calendar visibility where available',
    ],
  },
  {
    title: 'Make approvals predictable',
    description: 'Give configured approvers a consistent way to review requests while keeping actions limited to the staff and workflows they are responsible for.',
    bullets: [
      'Approver-based request visibility',
      'Approve or reject eligible requests',
      'Clear application-status tracking',
      'Permission-controlled access to leave actions and records',
    ],
  },
  {
    title: 'Adapt as the organisation grows',
    description: 'Manage the organisational context behind leave from one place as staff, locations, jurisdictions, and policies become more complex.',
    bullets: [
      'Manage staff, locations, leave types, and entitlements',
      'Reuse and customise leave-policy configuration',
      'Manage jurisdiction-specific calendars and public holidays',
      'Keep tenant administration centralised',
    ],
  },
];

const supportingCapabilities = [
  {
    title: 'Controlled access',
    description: 'Role and permission rules keep employees, managers, HR teams, and administrators focused on the information and actions relevant to them.',
  },
  {
    title: 'Flexible sign-in',
    description: 'Use username/password authentication with optional supported OAuth/OIDC sign-in providers for provisioned users.',
  },
  {
    title: 'Ask LeaveMaestro',
    description: 'The optional assistant can help authorised users work with LeaveMaestro capabilities while business permissions remain enforced by the application.',
  },
];

export const metadata: Metadata = {
  title: 'Leave policy automation and entitlement management features',
  description:
    'Explore LeaveMaestro capabilities for leave entitlement management, eligibility, proration, jurisdiction-aware calendars, employee self-service, approvals, and HR administration.',
};

export default function FeaturesPage() {
  return (
    <section className="section">
      <div className="max-w-3xl">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Features</p>
        <h1 className="mt-3 text-4xl font-bold tracking-tight">Manage the rules behind leave—not just the request form</h1>
        <p className="mt-5 text-lg">
          LeaveMaestro is designed for growing companies that have outgrown spreadsheet-based leave administration and need a clearer way to manage entitlement rules, employee self-service, approvals, and jurisdiction-specific leave context.
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
        <div className="max-w-3xl">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Supporting capabilities</p>
          <h2 className="mt-3 text-3xl font-bold tracking-tight">The controls and tools around the policy workflow</h2>
          <p className="mt-4 text-lg">
            Security, sign-in, and AI support matter, but they reinforce the core leave-policy experience rather than replacing it.
          </p>
        </div>
        <div className="mt-8 grid gap-6 lg:grid-cols-3">
          {supportingCapabilities.map((capability) => (
            <article key={capability.title} className="card border-0">
              <h3 className="text-xl font-semibold">{capability.title}</h3>
              <p className="mt-3">{capability.description}</p>
            </article>
          ))}
        </div>
      </div>

      <div className="mt-12 rounded-[2rem] border border-brand-100 bg-brand-50 px-8 py-10">
        <h2 className="text-3xl font-bold text-slate-950">See how LeaveMaestro handles your leave-policy workflow</h2>
        <p className="mt-4 max-w-2xl text-slate-600">
          Explore the demo or contact us to discuss the jurisdictions, policies, entitlement rules, and approval workflows your organisation needs.
        </p>
        <div className="mt-8 flex flex-col gap-4 sm:flex-row">
          <CTAButton href="/demo">Try Demo</CTAButton>
          <CTAButton href="/contact" variant="secondary">Contact Us</CTAButton>
        </div>
      </div>
    </section>
  );
}
