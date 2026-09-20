import type { Metadata } from 'next';
import { SearchLandingPage } from '@/components/SearchLandingPage';

export const metadata: Metadata = {
  alternates: { canonical: '/approval-workflows' },
  openGraph: { url: '/approval-workflows' },
  title: 'Employee leave approval workflows',
  description: 'LeaveMaestro connects employee leave applications with configured approver relationships so managers can focus on the requests assigned to them and employees can follow request status.',
};

export default function Page() {
  return <SearchLandingPage eyebrow="Approval workflows" title="Route leave requests to the people responsible for deciding them" intro="LeaveMaestro connects employee leave applications with configured approver relationships so managers can focus on the requests assigned to them and employees can follow request status." sections={[{"heading":"Configure approver relationships","body":"Approver assignments can be associated with staff and effective periods, supporting changes in reporting relationships over time."},{"heading":"Keep approval access scoped","body":"Approvers see the workflow they are responsible for rather than receiving broad HR administration rights.","bullets":["Configured approvers","Effective dates","Approve or reject","Role-based access"]},{"heading":"Keep requests and outcomes together","body":"Application status, approval decisions, and resulting leave information remain part of the same leave-management workflow."}]} related={[{"href":"/leave-management","label":"Leave management","description":"See how approvals fit the end-to-end process."},{"href":"/employee-leave-calendar","label":"Leave calendar","description":"Understand the calendar context around requests."},{"href":"/features","label":"Features","description":"Review employee, manager, and HR capabilities."}]} />;
}
