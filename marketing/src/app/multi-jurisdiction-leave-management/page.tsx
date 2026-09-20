import type { Metadata } from 'next';
import { SearchLandingPage } from '@/components/SearchLandingPage';

export const metadata: Metadata = {
  alternates: { canonical: '/multi-jurisdiction-leave-management' },
  openGraph: { url: '/multi-jurisdiction-leave-management' },
  title: 'Multi-jurisdiction leave management',
  description: 'LeaveMaestro models jurisdictions as part of leave administration so organisations operating across locations can keep relevant policies, leave types, and calendars distinct while using one tenant workflow.',
};

export default function Page() {
  return <SearchLandingPage eyebrow="Multi-jurisdiction" title="Keep leave configuration aligned to where staff work" intro="LeaveMaestro models jurisdictions as part of leave administration so organisations operating across locations can keep relevant policies, leave types, and calendars distinct while using one tenant workflow." sections={[{"heading":"Avoid one global policy pretending every location is the same","body":"Jurisdiction-aware configuration makes location-specific rules explicit and keeps them connected to the staff they apply to."},{"heading":"Use the relevant calendar context","body":"Public holidays and leave calendars can be associated with jurisdictions so leave calculations do not need to treat every employee as working against one shared calendar.","bullets":["Staff jurisdiction","Jurisdiction leave types","Public holidays","Policy configuration"]},{"heading":"Administer multiple jurisdictions in one product","body":"Tenant administration can manage supported jurisdictions without splitting every location into an unrelated leave system."}]} related={[{"href":"/singapore-leave-management","label":"Singapore leave management","description":"See a jurisdiction-specific use case."},{"href":"/leave-entitlements","label":"Leave entitlements","description":"Connect eligibility and entitlement policies to staff."},{"href":"/employee-leave-calendar","label":"Leave calendar","description":"Explore calendar-aware leave handling."}]} />;
}
