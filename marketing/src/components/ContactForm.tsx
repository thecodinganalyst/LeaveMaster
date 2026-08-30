'use client';

import { FormEvent, useState } from 'react';
import {
  ContactPayload,
  SubmissionStatus,
  submissionState,
  submitContactEnquiry,
  validateContactPayload,
} from './contactFormLogic.mjs';

const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
const fieldClass = 'w-full rounded-2xl border border-slate-300 px-4 py-3 text-slate-900 outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-100';

export function ContactForm() {
  const [status, setStatus] = useState<SubmissionStatus>('idle');
  const [feedback, setFeedback] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const payload: ContactPayload = {
      name: String(data.get('name') ?? ''),
      company: String(data.get('company') ?? ''),
      email: String(data.get('email') ?? ''),
      phone: String(data.get('phone') ?? ''),
      country: String(data.get('country') ?? ''),
      enquiryType: String(data.get('enquiryType') ?? '') as ContactPayload['enquiryType'],
      message: String(data.get('message') ?? ''),
      website: String(data.get('website') ?? ''),
    };

    const validationError = validateContactPayload(payload);
    if (validationError) {
      setStatus('error');
      setFeedback(validationError);
      return;
    }

    setStatus((current) => submissionState(current, 'submit'));
    setFeedback(null);
    try {
      const response = await submitContactEnquiry(payload, apiUrl);
      setStatus((current) => submissionState(current, 'success'));
      setFeedback(response.message ?? 'Thanks. Your enquiry has been received.');
      form.reset();
    } catch (error) {
      setStatus((current) => submissionState(current, 'error'));
      setFeedback(error instanceof Error ? error.message : 'We could not send your enquiry. Please try again.');
    }
  };

  return (
    <div className="card">
      <form className="space-y-5" onSubmit={handleSubmit}>
        <div className="grid gap-5 sm:grid-cols-2">
          <div>
            <label htmlFor="name" className="mb-2 block text-sm font-medium text-slate-900">Name *</label>
            <input id="name" name="name" type="text" required maxLength={120} className={fieldClass} autoComplete="name" />
          </div>
          <div>
            <label htmlFor="company" className="mb-2 block text-sm font-medium text-slate-900">Company *</label>
            <input id="company" name="company" type="text" required maxLength={160} className={fieldClass} autoComplete="organization" />
          </div>
        </div>

        <div className="grid gap-5 sm:grid-cols-2">
          <div>
            <label htmlFor="email" className="mb-2 block text-sm font-medium text-slate-900">Work email *</label>
            <input id="email" name="email" type="email" required maxLength={254} className={fieldClass} autoComplete="email" />
          </div>
          <div>
            <label htmlFor="phone" className="mb-2 block text-sm font-medium text-slate-900">Phone</label>
            <input id="phone" name="phone" type="tel" maxLength={40} className={fieldClass} autoComplete="tel" />
          </div>
        </div>

        <div>
          <label htmlFor="country" className="mb-2 block text-sm font-medium text-slate-900">Country</label>
          <input id="country" name="country" type="text" maxLength={100} className={fieldClass} autoComplete="country-name" />
        </div>

        <div>
          <label htmlFor="enquiryType" className="mb-2 block text-sm font-medium text-slate-900">Enquiry type *</label>
          <select id="enquiryType" name="enquiryType" required className={fieldClass} defaultValue="GENERAL_ENQUIRY">
            <option value="GENERAL_ENQUIRY">General Enquiry</option>
            <option value="PARTNERSHIP">Partnership</option>
          </select>
        </div>

        <div>
          <label htmlFor="message" className="mb-2 block text-sm font-medium text-slate-900">Message *</label>
          <textarea id="message" name="message" required maxLength={4000} rows={6} className={fieldClass} placeholder="How can we help?" />
        </div>

        <div className="absolute -left-[10000px]" aria-hidden="true">
          <label htmlFor="website">Website</label>
          <input id="website" name="website" type="text" tabIndex={-1} autoComplete="off" />
        </div>

        <button
          type="submit"
          disabled={status === 'submitting'}
          className="inline-flex items-center justify-center rounded-full bg-brand-600 px-6 py-3 text-sm font-semibold text-white transition hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {status === 'submitting' ? 'Sending…' : 'Send enquiry'}
        </button>

        {feedback ? (
          <p className={`text-sm ${status === 'success' ? 'text-emerald-600' : 'text-rose-600'}`} role="status">
            {feedback}
          </p>
        ) : null}
      </form>
    </div>
  );
}
