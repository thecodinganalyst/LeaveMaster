import type { Metadata } from 'next';
import { CTAButton } from '@/components/CTAButton';
import { ApprovalSnapshot, EmployeeDashboardSnapshot, PolicyBuilderSnapshot } from '@/components/ProductSnapshots';

const demoUrl = process.env.NEXT_PUBLIC_DEMO_URL ?? 'https://demo.leavemaestro.com';

export const metadata: Metadata = {
  title: 'Demo',
  description: 'Learn what the LeaveMaestro demo is designed to show across employee, manager, and HR leave-management workflows.',
};

export default function DemoPage() {
  return (
    <section className="section">
      <div className="max-w-3xl">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Product demo</p>
        <h1 className="mt-3 text-4xl font-bold tracking-tight">See LeaveMaestro from each role</h1>
        <p className="mt-5 text-lg">Explore representative employee, manager, and HR workflows using prepared demo data.</p>
      </div>

      <div className="mt-10 grid gap-8 lg:grid-cols-3">
        <div><EmployeeDashboardSnapshot /><div className="mt-4"><p className="font-semibold text-slate-900">Employee</p><p className="mt-1 text-sm text-slate-500">Apply, track, and check personal balances.</p></div></div>
        <div><ApprovalSnapshot /><div className="mt-4"><p className="font-semibold text-slate-900">Manager</p><p className="mt-1 text-sm text-slate-500">Review requests for assigned staff.</p></div></div>
        <div><PolicyBuilderSnapshot /><div className="mt-4"><p className="font-semibold text-slate-900">HR</p><p className="mt-1 text-sm text-slate-500">Manage policies and entitlement rules.</p></div></div>
      </div>

      <div className="mt-12 rounded-[2rem] border border-brand-100 bg-brand-50 p-6 sm:p-8">
        <div className="grid gap-6 lg:grid-cols-[1fr_auto] lg:items-center">
          <div><h2 className="text-2xl font-bold text-slate-950">Then explore the real workflow</h2><p className="mt-3 max-w-2xl text-slate-600">The public sandbox is intended to use fictional data while preserving LeaveMaestro role boundaries.</p><div className="mt-5 flex flex-wrap gap-2 text-xs font-medium text-brand-700">{['Prepared demo tenant', 'Representative data', 'Role-aware access'].map((item) => <span key={item} className="rounded-full border border-brand-100 bg-white px-3 py-2">{item}</span>)}</div></div>
          <div className="flex flex-col gap-3 sm:flex-row lg:flex-col"><CTAButton href={demoUrl}>Open Demo</CTAButton><CTAButton href="/contact" variant="secondary">Request Walkthrough</CTAButton></div>
        </div>
      </div>

      <p className="mt-6 text-center text-sm text-slate-500">Demo availability and persona-based access depend on the configured public demo environment.</p>
    </section>
  );
}
