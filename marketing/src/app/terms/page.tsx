import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Terms of Service',
  description: 'Review the draft LeaveMaestro terms information for the marketing site, demo environment, and product.',
};

export default function TermsPage() {
  return (
    <section className="section max-w-4xl">
      <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Terms of Service</p>
      <h1 className="mt-3 text-4xl font-bold tracking-tight">Terms information</h1>
      <div className="mt-6 rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-900">
        <strong>Draft — legal review required.</strong> This page is placeholder product content and does not constitute final contractual terms until reviewed and approved.
      </div>
      <div className="mt-8 space-y-8">
        <section>
          <h2 className="text-2xl font-semibold">Website and demo use</h2>
          <p className="mt-3">
            The marketing website and any public demo environment are intended for lawful product evaluation and should not be used to submit real confidential, employee, medical, or production business data.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Product access</h2>
          <p className="mt-3">
            Customer access, service scope, pricing, support, availability targets, and data-processing commitments should be governed by the applicable approved commercial agreement.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Accounts and acceptable use</h2>
          <p className="mt-3">
            Final terms should define account responsibilities, acceptable-use restrictions, suspension and termination rights, and the responsibilities of customer administrators and end users.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Intellectual property and liability</h2>
          <p className="mt-3">
            Ownership, licensing, warranties, disclaimers, liability limits, indemnities, governing law, and dispute-resolution provisions must be established in the final legally reviewed terms.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Questions</h2>
          <p className="mt-3">
            Until final legal contact details are published, please use the <a className="text-brand-600 hover:text-brand-700" href="/contact">contact form</a> for terms-related enquiries.
          </p>
        </section>
      </div>
    </section>
  );
}
