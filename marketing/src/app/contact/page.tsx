import type { Metadata } from 'next';
import { ContactForm } from '@/components/ContactForm';

export const metadata: Metadata = {
  title: 'Contact',
  description: 'Contact LeaveMaestro about product demos, leave-management requirements, or implementation questions.',
};

export default function ContactPage() {
  return (
    <section className="section">
      <div className="max-w-3xl">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Contact</p>
        <h1 className="mt-3 text-4xl font-bold tracking-tight">Tell us how your organization manages leave today</h1>
        <p className="mt-5 text-lg">
          Ask about LeaveMaestro, request a product walkthrough, or tell us about the leave policies and approval workflows you need to support.
        </p>
      </div>

      <div className="mt-12 grid gap-8 lg:grid-cols-[1.1fr_0.9fr]">
        <ContactForm />

        <aside className="space-y-6">
          <div className="card">
            <h2 className="text-xl font-semibold">What we can discuss</h2>
            <ul className="mt-5 space-y-4 text-sm text-slate-600">
              <li><strong className="text-slate-900">Product demo:</strong> employee, manager, and HR workflows.</li>
              <li><strong className="text-slate-900">Leave setup:</strong> leave types, entitlement policies, locations, balances, and approvers.</li>
              <li><strong className="text-slate-900">Access model:</strong> tenant-scoped roles and permissions for staff, managers, HR, and administrators.</li>
              <li><strong className="text-slate-900">Rollout questions:</strong> implementation needs, onboarding considerations, and organizational requirements.</li>
            </ul>
          </div>

          <div className="card border-brand-100 bg-brand-50">
            <h2 className="text-xl font-semibold text-slate-950">Still evaluating?</h2>
            <p className="mt-3 text-slate-600">
              You can review the feature overview first, then use this form when you are ready to discuss your requirements.
            </p>
            <a href="/features" className="mt-5 inline-block font-semibold text-brand-700 hover:text-brand-600">
              Explore features →
            </a>
          </div>
        </aside>
      </div>
    </section>
  );
}
