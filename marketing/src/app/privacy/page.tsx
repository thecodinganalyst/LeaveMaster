import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Privacy Policy',
  description: 'Review the draft LeaveMaestro privacy information for the marketing site and product.',
};

export default function PrivacyPage() {
  return (
    <section className="section max-w-4xl">
      <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Privacy Policy</p>
      <h1 className="mt-3 text-4xl font-bold tracking-tight">Privacy information</h1>
      <div className="mt-6 rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-900">
        <strong>Draft — legal review required.</strong> This page is placeholder product content and must not be treated as the final LeaveMaestro privacy policy until reviewed and approved.
      </div>
      <div className="mt-8 space-y-8">
        <section>
          <h2 className="text-2xl font-semibold">Information the service may process</h2>
          <p className="mt-3">
            LeaveMaestro may process account, organization, staff, location, leave application, entitlement, approval, and related operational data needed to provide leave-management functionality.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Marketing enquiries</h2>
          <p className="mt-3">
            Information submitted through the public contact experience may be used to respond to product, demo, pricing, partnership, or other customer enquiries.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Service providers</h2>
          <p className="mt-3">
            The deployed service may rely on infrastructure, database, authentication, email, analytics, and AI providers according to the environment and features enabled by the operator.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Security and retention</h2>
          <p className="mt-3">
            Final commitments for security controls, retention, deletion, international transfers, subprocessors, data-subject rights, and incident handling must be defined in the approved privacy policy and customer agreements.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Questions</h2>
          <p className="mt-3">
            Until final legal contact details are published, please use the <a className="text-brand-600 hover:text-brand-700" href="/contact">contact form</a> for privacy-related enquiries.
          </p>
        </section>
      </div>
    </section>
  );
}
