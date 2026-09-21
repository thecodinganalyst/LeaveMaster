import type { Metadata } from 'next';
import Link from 'next/link';

const guides = [
  ['annual-leave-entitlement','Annual leave entitlement','Eligibility and statutory minimums by service.'],
  ['annual-leave-proration','Joining mid-year and proration','Completed-month proration and policy configuration.'],
  ['sick-hospitalisation-leave','Sick and hospitalisation leave','Eligibility and service-based entitlements.'],
  ['childcare-leave','Childcare leave','Current eligibility and administration guidance.'],
  ['infant-care-leave','Infant care leave','Unpaid infant care leave and request handling.'],
  ['public-holidays-and-leave','Public holidays and leave','Public-holiday entitlement and calendar context.'],
  ['part-time-employee-leave','Part-time employee leave','Hour-based leave treatment for part-time staff.'],
  ['carry-forward-policies','Carry-forward policies','Statutory protection versus company policy.'],
  ['leave-approval-workflows','Leave approval workflows','Separate entitlement from operational approval.'],
  ['moving-from-spreadsheets','Moving from spreadsheets','Plan and reconcile a controlled system migration.'],
] as const;

export const metadata: Metadata = {
  alternates: { canonical: '/singapore-leave-guides' },
  openGraph: { url: '/singapore-leave-guides' },
  title: 'Singapore leave guides for HR and employees',
  description: 'Practical Singapore leave guides covering annual leave, proration, sick leave, childcare, infant care, public holidays, part-time leave, carry-forward and workflows.',
};

export default function Page() {
  return <section className="section"><div className="mx-auto max-w-4xl"><p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Singapore leave knowledge</p><h1 className="mt-3 text-4xl font-bold tracking-tight sm:text-5xl">Singapore leave guides</h1><p className="mt-5 max-w-3xl text-lg leading-8 text-slate-600">A maintained starting point for understanding Singapore leave topics and translating reviewed policy into a leave-management workflow. Statutory-sensitive articles link to official government guidance and identify their review date.</p><div className="mt-10 rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-950"><strong>Not legal advice.</strong> Confirm current statutory requirements with the linked official sources and your organisation&apos;s advisers where appropriate.</div><div className="mt-8"><Link href="/tools/singapore-annual-leave-calculator" className="inline-flex rounded-full bg-brand-600 px-5 py-3 text-sm font-semibold text-white hover:bg-brand-700">Try the annual leave calculator</Link></div><div className="mt-12 grid gap-5 sm:grid-cols-2">{guides.map(([slug,title,description]) => <Link key={slug} href={`/singapore-leave-guides/${slug}`} className="card hover:border-brand-300"><h2 className="text-xl font-semibold text-slate-950">{title}</h2><p className="mt-2 text-sm leading-6 text-slate-600">{description}</p></Link>)}</div><div className="mt-12 flex flex-wrap gap-3"><Link href="/singapore-leave-management" className="font-semibold text-brand-700 hover:text-brand-800">Singapore LeaveMaestro configuration →</Link><Link href="/leave-entitlements" className="font-semibold text-brand-700 hover:text-brand-800">Leave entitlements →</Link></div></div></section>;
}
