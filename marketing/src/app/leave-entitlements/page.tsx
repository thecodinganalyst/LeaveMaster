import type { Metadata } from 'next';
import { SearchLandingPage } from '@/components/SearchLandingPage';

export const metadata: Metadata = {
  alternates: { canonical: '/leave-entitlements' },
  openGraph: { url: '/leave-entitlements' },
  title: 'Leave entitlement and eligibility management',
  description: 'LeaveMaestro keeps entitlement and eligibility configuration connected to staff leave records, helping HR teams manage how configured policy rules become usable employee balances.',
};

export default function Page() {
  return <SearchLandingPage eyebrow="Leave entitlements" title="Turn entitlement rules into employee leave balances" intro="LeaveMaestro keeps entitlement and eligibility configuration connected to staff leave records, helping HR teams manage how configured policy rules become usable employee balances." sections={[{"heading":"Model entitlement policy explicitly","body":"Configure the rules that determine entitlement rather than relying on manually maintained balance cells."},{"heading":"Account for employee context","body":"Eligibility and entitlement processing can use staff and jurisdiction context so policies are applied to the intended population.","bullets":["Eligibility criteria","Entitlement rules","Join-date context","Employee balances"]},{"heading":"Keep the result visible","body":"Employees can view their own entitlement information while administrators retain the appropriate controls over policy configuration."}]} related={[{"href":"/leave-management","label":"Leave management","description":"See entitlements in the full request workflow."},{"href":"/ai-leave-assistant","label":"Ask LeaveMaestro","description":"See how authorised users can ask about leave outcomes."},{"href":"/singapore-leave-management","label":"Singapore leave","description":"Explore jurisdiction-aware configuration."}]} />;
}
