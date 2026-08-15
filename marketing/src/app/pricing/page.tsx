import type { Metadata } from 'next';
import { CTAButton } from '@/components/CTAButton';

export const metadata: Metadata = {
  title: 'Pricing',
  description: 'Learn about LeaveMaestro pricing availability and contact us to discuss your organization and rollout requirements.',
};

export default function PricingPage() {
  return (
    <section className="section">
      <div className="mx-auto max-w-4xl text-center">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Pricing</p>
        <h1 className="mt-3 text-4xl font-bold tracking-tight">Pricing is being prepared for launch</h1>
        <p className="mx-auto mt-5 max-w-2xl text-lg">
          We are still defining the commercial plans for LeaveMaestro. Contact us to discuss your organization size, leave-management requirements, and expected rollout.
        </p>
      </div>

      <div className="mx-auto mt-12 max-w-4xl rounded-[2rem] border border-slate-200 bg-white p-8 shadow-soft sm:p-10">
        <div className="grid gap-8 md:grid-cols-2">
          <div>
            <h2 className="text-2xl font-semibold">What helps us understand your needs</h2>
            <ul className="mt-5 space-y-3 text-slate-600">
              <li>• Approximate number of employees</li>
              <li>• Countries or jurisdictions you operate in</li>
              <li>• Leave types and entitlement policies you need to support</li>
              <li>• Approval and HR administration requirements</li>
            </ul>
          </div>
          <div className="rounded-2xl bg-slate-50 p-6">
            <h2 className="text-xl font-semibold">Want to evaluate the product first?</h2>
            <p className="mt-3 text-slate-600">
              Review the feature overview or explore the public demo experience before discussing commercial options.
            </p>
            <div className="mt-6 flex flex-col gap-3">
              <CTAButton href="/demo">Try Demo</CTAButton>
              <CTAButton href="/contact" variant="secondary">Discuss Pricing</CTAButton>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
