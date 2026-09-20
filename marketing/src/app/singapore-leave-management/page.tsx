import type { Metadata } from 'next';
import { SearchLandingPage } from '@/components/SearchLandingPage';

export const metadata: Metadata = {
  alternates: { canonical: '/singapore-leave-management' },
  openGraph: { url: '/singapore-leave-management' },
  title: 'Singapore leave management software',
  description: 'LeaveMaestro supports jurisdiction-aware leave configuration so Singapore staff can be connected to the relevant leave types, policy rules, public-holiday calendars, and entitlement processing. Product configuration should still be reviewed against your organisation\'s policies and current statutory requirements.',
};

export default function Page() {
  return <SearchLandingPage eyebrow="Singapore leave management" title="Manage Singapore leave rules in a configurable workflow" intro="LeaveMaestro supports jurisdiction-aware leave configuration so Singapore staff can be connected to the relevant leave types, policy rules, public-holiday calendars, and entitlement processing. Product configuration should still be reviewed against your organisation's policies and current statutory requirements." sections={[{"heading":"Keep Singapore configuration explicit","body":"Model jurisdiction-specific leave types, eligibility and entitlement rules instead of hiding important assumptions in spreadsheet formulas."},{"heading":"Connect staff to the relevant jurisdiction","body":"A staff member's jurisdiction is part of the leave context, helping keep policies and calendars aligned to where the employee works.","bullets":["Jurisdiction assignment","Public-holiday calendars","Eligibility rules","Entitlement policies"]},{"heading":"Configuration is not legal advice","body":"LeaveMaestro helps implement configured rules; organisations remain responsible for confirming their policies and statutory obligations against authoritative Singapore sources."}]} related={[{"href":"/multi-jurisdiction-leave-management","label":"Multi-jurisdiction leave","description":"See how jurisdiction-specific configuration scales."},{"href":"/leave-entitlements","label":"Leave entitlements","description":"Explore entitlement and eligibility handling."},{"href":"/features","label":"Product features","description":"Review the wider LeaveMaestro workflow."}]} />;
}
