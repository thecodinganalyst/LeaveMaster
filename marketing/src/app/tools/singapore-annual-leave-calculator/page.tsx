import type { Metadata } from 'next';
import Link from 'next/link';
import { SingaporeAnnualLeaveCalculator } from '@/components/SingaporeAnnualLeaveCalculator';
import { siteUrl } from '@/lib/site';

const path = '/tools/singapore-annual-leave-calculator';
const momUrl = 'https://www.mom.gov.sg/employment-practices/leave/annual-leave/eligibility-and-entitlement';

export const metadata: Metadata = {
  alternates: { canonical: path },
  openGraph: { url: path },
  title: 'Singapore annual leave entitlement calculator',
  description: 'Estimate Singapore statutory minimum annual leave from service dates using MOM eligibility, service-year tiers, completed-month proration and whole-day rounding guidance.',
};

export default function Page() {
  const structuredData = {
    '@context': 'https://schema.org',
    '@type': 'WebApplication',
    name: 'Singapore annual leave entitlement calculator',
    url: `${siteUrl}${path}`,
    applicationCategory: 'BusinessApplication',
    operatingSystem: 'Web',
    isAccessibleForFree: true,
    description: 'A public calculator for estimating Singapore statutory minimum annual leave from employment service dates.',
  };

  return <main className="section">
    <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(structuredData) }} />
    <div className="mx-auto max-w-4xl">
      <nav aria-label="Breadcrumb" className="text-sm text-slate-500"><Link href="/singapore-leave-guides" className="hover:text-brand-700">Singapore leave guides</Link><span aria-hidden="true"> / </span><span>Annual leave calculator</span></nav>
      <p className="mt-6 text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Free Singapore leave tool</p>
      <h1 className="mt-3 text-4xl font-bold tracking-tight text-slate-950 sm:text-5xl">Singapore annual leave entitlement calculator</h1>
      <p className="mt-5 max-w-3xl text-lg leading-8 text-slate-600">Estimate the statutory minimum paid annual leave for an employee covered by Singapore&apos;s Employment Act, based on employment start date, completed service and whether employment has ended.</p>
      <div className="mt-8 rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm leading-6 text-amber-950"><strong>Information, not legal advice.</strong> This calculator models the general MOM annual-leave rules described below. Employment terms can be more generous, and approved unpaid leave, notice periods, coverage questions or other circumstances can change the result. Confirm your situation with current official guidance.</div>
      <div className="mt-10"><SingaporeAnnualLeaveCalculator /></div>

      <section className="mt-14"><h2 className="text-2xl font-semibold">What the calculator assumes</h2><ul className="mt-4 list-disc space-y-2 pl-6 leading-7 text-slate-600"><li>The employee is covered by the Employment Act.</li><li>Paid annual leave eligibility begins after at least 3 completed months with the employer.</li><li>The statutory minimum is 7 days in the first service year, increasing by one day per service year to 14 days from the eighth year onward.</li><li>Where proration applies, completed months are divided by 12 and multiplied by the applicable annual tier.</li><li>Fractions below half a day round down; half a day or more rounds up to a whole day.</li></ul></section>
      <section className="mt-12"><h2 className="text-2xl font-semibold">Why this is an estimate</h2><p className="mt-3 leading-7 text-slate-600">MOM separately explains treatment of approved unpaid leave and notice periods. The calculator does not ask for enough information to determine those cases. Employer contracts can also provide more leave than the statutory minimum. LeaveMaestro itself applies the policy configured by an organisation; this public tool does not read or change LeaveMaestro employee data.</p></section>
      <section className="mt-12"><h2 className="text-2xl font-semibold">Official source</h2><p className="mt-3 text-slate-600">Reviewed 21 September 2026 against <a href={momUrl} rel="noreferrer" className="font-semibold text-brand-700 underline decoration-brand-200 underline-offset-4">MOM: Annual leave eligibility and entitlement</a>.</p></section>
      <aside className="mt-12 rounded-[2rem] bg-slate-50 p-7"><h2 className="text-2xl font-semibold">Continue exploring</h2><div className="mt-5 flex flex-wrap gap-3"><Link href="/singapore-leave-guides/annual-leave-entitlement" className="font-semibold text-brand-700">Annual leave guide →</Link><Link href="/singapore-leave-guides/annual-leave-proration" className="font-semibold text-brand-700">Proration guide →</Link><Link href="/leave-entitlements" className="font-semibold text-brand-700">LeaveMaestro entitlements →</Link><a href="https://thecodinganalyst.github.io/LeaveMaster/leave-entitlement-generation/" className="font-semibold text-brand-700">Technical documentation →</a></div></aside>
    </div>
  </main>;
}
