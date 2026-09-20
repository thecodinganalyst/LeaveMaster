import type { Metadata } from 'next';
import { SearchLandingPage } from '@/components/SearchLandingPage';

export const metadata: Metadata = {
  alternates: { canonical: '/leave-management' },
  openGraph: { url: '/leave-management' },
  title: 'Leave management software for policy-aware teams',
  description: 'LeaveMaestro connects leave policies, employee entitlements, requests, approvals, balances, and calendars in one workflow. It is designed for teams that need the rules behind leave to remain visible and manageable.',
};

export default function Page() {
  return <SearchLandingPage eyebrow="Leave management" title="Move beyond spreadsheet leave tracking" intro="LeaveMaestro connects leave policies, employee entitlements, requests, approvals, balances, and calendars in one workflow. It is designed for teams that need the rules behind leave to remain visible and manageable." sections={[{"heading":"Keep policy and operations connected","body":"Configure leave types and policy rules, then use those rules in the same system where employees submit requests and approvers make decisions.","bullets":["Policy configuration","Employee balances","Request history","Approval workflow"]},{"heading":"Give employees useful self-service","body":"Staff can review their own leave information and submit requests without relying on HR to maintain a separate spreadsheet or email trail."},{"heading":"Built for controlled administration","body":"Role-based access separates employee, approver, HR, tenant administration, and platform responsibilities so leave operations are not exposed indiscriminately."}]} related={[{"href":"/leave-entitlements","label":"Leave entitlements","description":"See how policy rules connect to employee balances."},{"href":"/approval-workflows","label":"Approval workflows","description":"Understand how requests move to configured approvers."},{"href":"/employee-leave-calendar","label":"Leave calendar","description":"See how calendars and working days fit the workflow."}]} resources={[{href:"https://thecodinganalyst.github.io/LeaveMaster/user-guide/",label:"LeaveMaestro user guide",external:true},{href:"https://thecodinganalyst.github.io/LeaveMaster/technical/",label:"Technical documentation",external:true}]} />;
}
