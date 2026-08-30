import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Terms of Service',
  description: 'Terms for the LeaveMaestro website and request-only hosted evaluation environment.',
};

export default function TermsPage() {
  return (
    <section className="section max-w-4xl">
      <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Terms of Service</p>
      <h1 className="mt-3 text-4xl font-bold tracking-tight">Hosted evaluation terms</h1>
      <p className="mt-4 text-sm text-slate-500">Last updated: 30 August 2026</p>

      <div className="mt-8 space-y-8">
        <section>
          <h2 className="text-2xl font-semibold">Evaluation access only</h2>
          <p className="mt-3">
            The project-hosted LeaveMaestro environment is provided solely for testing, demonstration, and evaluation. Access is not generally available to the public, is granted only upon request and at the project&apos;s discretion, is limited in duration, and may expire, be suspended, or be revoked at any time.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">No production use</h2>
          <p className="mt-3">
            The hosted evaluation environment is not a production SaaS service and must not be used as an operational HR system, an organisation&apos;s system of record, or the sole source of any information that must be retained.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Test data only</h2>
          <p className="mt-3">
            Use fictional, anonymised, or other non-production data only. Do not enter real employee, dependant, confidential, medical, or other production personal data into the project-hosted environment.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">No data-retention guarantee</h2>
          <p className="mt-3">
            Information in the evaluation environment is temporary and may be reset, modified, or permanently deleted at any time. No backup, recovery, retention period, or data-preservation service is provided. You are responsible for keeping any information you need outside the hosted evaluation environment.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">No service commitment</h2>
          <p className="mt-3">
            The evaluation environment is provided on an <strong>AS IS</strong> and <strong>AS AVAILABLE</strong> basis to the fullest extent permitted by applicable law. There is no service-level agreement or commitment concerning uptime, availability, support, storage, performance, backup, recovery, or continued operation. The environment may change, reset, suspend, or be discontinued at any time.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">No legal, HR, or compliance advice</h2>
          <p className="mt-3">
            LeaveMaestro may model or explain leave policies and entitlements, but it does not provide legal, employment, HR, tax, or regulatory advice. Laws, policies, and individual circumstances can change. You are responsible for independently verifying any rule, entitlement, calculation, or decision before relying on it.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Acceptable use</h2>
          <p className="mt-3">
            You must not use the hosted environment unlawfully, attempt unauthorised access, interfere with its operation, introduce malicious software, deliberately overload the service, or use it in a way that creates unreasonable cost, security risk, or operational burden for the project.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Open-source software and self-hosting</h2>
          <p className="mt-3">
            LeaveMaestro source code is available under the Apache License, Version 2.0. These hosted-service terms govern only the project-operated evaluation environment and do not replace or restrict rights granted by the open-source license. Operators of self-hosted or independently modified installations are responsible for their own infrastructure, security, backups, availability, data handling, and legal compliance.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Warranty and liability</h2>
          <p className="mt-3">
            To the fullest extent permitted by applicable law, no warranty is given regarding reliability, availability, accuracy, fitness for a particular purpose, data preservation, or regulatory compliance. To the fullest extent permitted by applicable law, the project and its maintainers are not liable for indirect, incidental, special, consequential, or similar loss arising from use of or inability to use the hosted environment or software, including loss of data, business interruption, or decisions made in reliance on LeaveMaestro. Nothing in these terms excludes liability that cannot lawfully be excluded.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Questions</h2>
          <p className="mt-3">
            For questions about these terms or evaluation access, use the <a className="text-brand-600 hover:text-brand-700" href="/contact">LeaveMaestro contact form</a>.
          </p>
        </section>
      </div>
    </section>
  );
}
