import type { Metadata } from 'next';
import { SearchLandingPage } from '@/components/SearchLandingPage';

export const metadata: Metadata = {
  alternates: { canonical: '/employee-leave-calendar' },
  openGraph: { url: '/employee-leave-calendar' },
  title: 'Employee leave calendar and working-day management',
  description: 'LeaveMaestro brings working schedules, leave calendars, public holidays, and employee leave requests into the same policy-aware system so request duration can reflect the relevant calendar context.',
};

export default function Page() {
  return <SearchLandingPage eyebrow="Leave calendar" title="Use working days and jurisdiction calendars in leave handling" intro="LeaveMaestro brings working schedules, leave calendars, public holidays, and employee leave requests into the same policy-aware system so request duration can reflect the relevant calendar context." sections={[{"heading":"Represent working schedules","body":"Employee schedules distinguish working and non-working days so leave handling does not have to assume every calendar day is chargeable."},{"heading":"Keep public holidays jurisdiction-aware","body":"Jurisdiction calendars provide the context needed to account for location-specific public holidays.","bullets":["Working schedules","Public holidays","Jurisdiction calendars","Request dates"]},{"heading":"Respect employment dates","body":"Leave requests should remain within the employee's applicable employment period, helping prevent requests before joining or after termination."}]} related={[{"href":"/multi-jurisdiction-leave-management","label":"Multi-jurisdiction leave","description":"See why calendar context differs by location."},{"href":"/leave-management","label":"Leave management","description":"Explore the complete request workflow."},{"href":"/approval-workflows","label":"Approval workflows","description":"See what happens after an employee submits leave."}]} />;
}
