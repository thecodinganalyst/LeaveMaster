import type { Metadata } from 'next';
import { ContactForm } from '@/components/ContactForm';

export const metadata: Metadata = {
  title: 'Contact',
  description: 'Contact LeaveMaestro to discuss demos, pricing, implementation, or support for your leave management process.',
};

export default function ContactPage() {
  return (
    <section className="section">
      <div className="max-w-3xl">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Contact</p>
        <h1 className="mt-3 text-4xl font-bold tracking-tight">Talk to the LeaveMaestro team</h1>
        <p className="mt-5 text-lg">
          Whether you want a demo, pricing information, or migration help, we&apos;re here to support your rollout.
        </p>
      </div>

      <div className="mt-12 grid gap-8 lg:grid-cols-[1.1fr_0.9fr]">
        <ContactForm />

        <aside className="space-y-6">
          <div className="card">
            <h2 className="text-xl font-semibold">Contact information</h2>
            <dl className="mt-5 space-y-4 text-sm">
              <div>
                <dt className="font-medium text-slate-900">Email</dt>
                <dd>
                  <a href="mailto:hello@leavemaestro.com" className="text-brand-600 hover:text-brand-700">
                    hello@leavemaestro.com
                  </a>
                </dd>
              </div>
              <div>
                <dt className="font-medium text-slate-900">Sales</dt>
                <dd>
                  <a href="tel:+1-800-555-0184" className="text-brand-600 hover:text-brand-700">
                    +1 (800) 555-0184
                  </a>
                </dd>
              </div>
              <div>
                <dt className="font-medium text-slate-900">Office hours</dt>
                <dd>Monday–Friday, 9:00 AM–6:00 PM UTC</dd>
              </div>
            </dl>
          </div>

          <div className="card bg-slate-950 text-white">
            <h2 className="text-xl font-semibold text-white">Implementation support</h2>
            <p className="mt-3 text-slate-300">
              Need help mapping leave policies, approval chains, or rollout plans? Our onboarding team can guide the process.
            </p>
          </div>
        </aside>
      </div>
    </section>
  );
}
