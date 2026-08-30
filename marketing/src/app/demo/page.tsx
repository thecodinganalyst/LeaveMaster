import type { Metadata } from 'next';
import { CTAButton } from '@/components/CTAButton';
import { ApprovalSnapshot, EmployeeDashboardSnapshot, PolicyBuilderSnapshot } from '@/components/ProductSnapshots';

export const metadata: Metadata = {
  title: 'Evaluation Access',
  description: 'Request temporary access to the LeaveMaestro hosted evaluation environment for testing and demonstration with fictional or anonymised data only.',
};

export default function DemoPage() {
  return (
    <section className="section">
      <div className="max-w-3xl">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Hosted evaluation</p>
        <h1 className="mt-3 text-4xl font-bold tracking-tight">Request temporary evaluation access</h1>
        <p className="mt-5 text-lg">
          The project-hosted LeaveMaestro environment is available only by request for testing, demonstration, and evaluation. It is not a production SaaS service.
        </p>
      </div>

      <div className="mt-8 rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-950" role="note">
        <strong>Evaluation environment — test data only.</strong> Do not enter real employee, dependant, confidential, or other production personal data. Data may be reset or permanently deleted at any time.
      </div>

      <div className="mt-10 grid gap-8 lg:grid-cols-3">
        <div><EmployeeDashboardSnapshot /><div className="mt-4"><p className="font-semibold text-slate-900">Employee</p><p className="mt-1 text-sm text-slate-500">Evaluate leave requests, history, and personal balances.</p></div></div>
        <div><ApprovalSnapshot /><div className="mt-4"><p className="font-semibold text-slate-900">Manager</p><p className="mt-1 text-sm text-slate-500">Evaluate role-aware request review and approvals.</p></div></div>
        <div><PolicyBuilderSnapshot /><div className="mt-4"><p className="font-semibold text-slate-900">HR</p><p className="mt-1 text-sm text-slate-500">Evaluate policy and entitlement configuration.</p></div></div>
      </div>

      <div className="mt-12 grid gap-6 lg:grid-cols-2">
        <div className="card">
          <h2 className="text-2xl font-bold text-slate-950">Hosted evaluation</h2>
          <ul className="mt-5 space-y-3 text-sm text-slate-600">
            <li>Access is granted at the project&apos;s discretion and for a limited period.</li>
            <li>Access may expire, be suspended, or be revoked at any time.</li>
            <li>No SLA, uptime, support, backup, recovery, or data-retention commitment is provided.</li>
            <li>The environment must not be used as an operational HR system or system of record.</li>
          </ul>
          <div className="mt-6"><CTAButton href="/contact">Request Evaluation Access</CTAButton></div>
        </div>

        <div className="card border-brand-100 bg-brand-50">
          <h2 className="text-2xl font-bold text-slate-950">Need production control?</h2>
          <p className="mt-4 text-slate-600">
            LeaveMaestro is open-source software under the Apache License 2.0. Organisations that need production use, their own retention policies, backups, availability controls, or operational ownership should deploy and operate their own instance.
          </p>
          <div className="mt-6"><CTAButton href="https://github.com/thecodinganalyst/LeaveMaster" variant="secondary">View Source</CTAButton></div>
        </div>
      </div>

      <p className="mt-6 text-center text-sm text-slate-500">Submitting a request does not guarantee access. Approved access is temporary and subject to the Terms of Service.</p>
    </section>
  );
}
