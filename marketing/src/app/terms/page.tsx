import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Terms of Service',
  description: 'Review the LeaveMaestro Terms of Service for use of the marketing site, demo environment, and product services.',
};

export default function TermsPage() {
  return (
    <section className="section max-w-4xl">
      <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Terms of Service</p>
      <h1 className="mt-3 text-4xl font-bold tracking-tight">LeaveMaestro Terms of Service</h1>
      <div className="mt-8 space-y-8">
        <section>
          <h2 className="text-2xl font-semibold">Acceptance of terms</h2>
          <p className="mt-3">
            By accessing LeaveMaestro, you agree to these terms and any related policies referenced on this site.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Use of the service</h2>
          <p className="mt-3">
            Customers may use LeaveMaestro only for lawful business purposes and must keep account credentials secure and accurate.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Subscription and availability</h2>
          <p className="mt-3">
            Product access, pricing, and support commitments are governed by the applicable order form or subscription agreement. Service availability may vary during maintenance or updates.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Intellectual property</h2>
          <p className="mt-3">
            All site content, branding, and product materials remain the property of LeaveMaestro or its licensors unless otherwise stated.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Contact</h2>
          <p className="mt-3">
            Questions regarding these terms can be directed to <a className="text-brand-600 hover:text-brand-700" href="mailto:legal@leavemaestro.com">legal@leavemaestro.com</a>.
          </p>
        </section>
      </div>
    </section>
  );
}
