import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Privacy Policy',
  description: 'Privacy information for the LeaveMaestro website and project-hosted evaluation environment.',
};

export default function PrivacyPage() {
  return (
    <section className="section max-w-4xl">
      <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Privacy Policy</p>
      <h1 className="mt-3 text-4xl font-bold tracking-tight">Privacy and the hosted evaluation environment</h1>
      <p className="mt-4 text-sm text-slate-500">Last updated: 30 August 2026</p>

      <div className="mt-8 space-y-8">
        <section>
          <h2 className="text-2xl font-semibold">Scope</h2>
          <p className="mt-3">
            This policy applies to the LeaveMaestro marketing website and the project-operated hosted evaluation environment. It does not govern independently hosted, cloned, or forked LeaveMaestro installations; the operator of each independent installation is responsible for its own privacy practices.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Evaluation data must be non-production data</h2>
          <p className="mt-3">
            The hosted environment is provided only for testing, demonstration, and evaluation. Users must use fictional, anonymised, or other non-production data and should not enter real employee, dependant, confidential, medical, or other production personal data.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Information we may process</h2>
          <p className="mt-3">
            We may process limited information needed to operate the website and evaluation environment, including evaluation-access requests, contact enquiries, account and authentication identifiers, security and diagnostic logs, and technical information required to operate, secure, and troubleshoot the service.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">How information is used</h2>
          <p className="mt-3">
            Information may be used to review and administer evaluation access, authenticate users, respond to enquiries, operate and secure the environment, investigate faults or misuse, and improve LeaveMaestro. We do not sell personal information.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Service providers</h2>
          <p className="mt-3">
            The project-operated service may rely on third-party hosting, database, authentication, email, monitoring, and AI providers where those features are enabled. Those providers may process information as necessary to provide their services.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Temporary storage and deletion</h2>
          <p className="mt-3">
            The hosted evaluation environment is not a permanent record-keeping or archival service. Evaluation data may be reset, modified, or permanently deleted at any time, including during maintenance, upgrades, resource management, security work, expiry of evaluation access, or discontinuation of the environment. No backup, recovery, or retention commitment is provided.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Security</h2>
          <p className="mt-3">
            Reasonable measures are used to protect the project-operated environment, but no internet service or storage system can be guaranteed to be completely secure or continuously available. Users should not place information in the evaluation environment that requires production-grade protection or retention.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Questions and requests</h2>
          <p className="mt-3">
            For privacy-related questions or requests concerning the project-operated website or evaluation environment, please use the <a className="text-brand-600 hover:text-brand-700" href="/contact">LeaveMaestro contact form</a>. If applicable law requires a separate publicly available business contact channel, that contact information will be published here.
          </p>
        </section>
        <section>
          <h2 className="text-2xl font-semibold">Changes to this policy</h2>
          <p className="mt-3">This policy may be updated as LeaveMaestro and the evaluation environment evolve. The current version will be published on this page with its effective date.</p>
        </section>
      </div>
    </section>
  );
}
