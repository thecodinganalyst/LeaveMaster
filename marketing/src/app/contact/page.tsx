import type { Metadata } from 'next';
import { ContactForm } from '@/components/ContactForm';

export const metadata: Metadata = {
  title: 'Contact',
  description: 'Contact LeaveMaestro with a general enquiry, partnership enquiry, or request for temporary evaluation access.',
};

export default function ContactPage() {
  return (
    <section className="section">
      <div className="max-w-3xl">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Contact</p>
        <h1 className="mt-3 text-4xl font-bold tracking-tight">Contact the LeaveMaestro project</h1>
        <p className="mt-5 text-lg">
          Use the form for a general enquiry, partnership enquiry, or to request temporary access to the hosted evaluation environment.
        </p>
      </div>

      <div className="mt-8 rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-950" role="note">
        <strong>Evaluation access is temporary and test-only.</strong> If you are requesting access, please say so in your message. Approved users must use fictional or anonymised data only; the environment is not for production use and data may be deleted at any time.
      </div>

      <div className="mt-12 grid gap-8 lg:grid-cols-[1.1fr_0.9fr]">
        <ContactForm />

        <aside className="space-y-6">
          <div className="card">
            <h2 className="text-xl font-semibold">Evaluation access</h2>
            <ul className="mt-5 space-y-4 text-sm text-slate-600">
              <li><strong className="text-slate-900">Request-only:</strong> submitting the form does not automatically create an account or guarantee access.</li>
              <li><strong className="text-slate-900">Limited duration:</strong> approved access may expire or be revoked.</li>
              <li><strong className="text-slate-900">Test data only:</strong> do not enter real employee, dependant, confidential, medical, or other production personal data.</li>
              <li><strong className="text-slate-900">No production commitment:</strong> no SLA, backup, recovery, or data-retention guarantee is provided.</li>
            </ul>
          </div>

          <div className="card border-brand-100 bg-brand-50">
            <h2 className="text-xl font-semibold text-slate-950">Prefer to run it yourself?</h2>
            <p className="mt-3 text-slate-600">
              LeaveMaestro is open-source under Apache License 2.0. You can clone or fork the repository and operate your own instance when you need production control, retention, backups, or availability on your own terms.
            </p>
            <a href="https://github.com/thecodinganalyst/LeaveMaster" className="mt-5 inline-block font-semibold text-brand-700 hover:text-brand-600">
              View the open-source repository →
            </a>
          </div>
        </aside>
      </div>
    </section>
  );
}
