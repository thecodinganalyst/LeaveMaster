import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Privacy Policy',
  description: 'Review the LeaveMaestro Privacy Policy and learn how we collect, use, and protect personal information.',
};

export default function PrivacyPage() {
  return (
    <section className="section max-w-4xl">
      <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Privacy Policy</p>
      <h1 className="mt-3 text-4xl font-bold tracking-tight">Your privacy matters to LeaveMaestro</h1>
      <div className="mt-8 space-y-8">
        <section>
          <h2 className="text-2xl font-semibold">Information we collect</h2>
          <p className="mt-3">
            We collect information you provide directly, including names, email addresses, employer details, and leave-related records needed to operate the service.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">How we use information</h2>
          <p className="mt-3">
            LeaveMaestro uses information to provide the product, improve service quality, respond to support requests, and maintain secure, reliable access to leave data.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Data sharing</h2>
          <p className="mt-3">
            We do not sell personal information. We only share data with service providers and subprocessors that help us host, secure, and support LeaveMaestro.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Security and retention</h2>
          <p className="mt-3">
            We apply administrative, technical, and physical safeguards to protect data. Records are retained only as long as needed for service delivery, legal obligations, and customer agreements.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Contact us</h2>
          <p className="mt-3">
            Questions about this policy can be sent to <a className="text-brand-600 hover:text-brand-700" href="mailto:privacy@leavemaestro.com">privacy@leavemaestro.com</a>.
          </p>
        </section>
      </div>
    </section>
  );
}
