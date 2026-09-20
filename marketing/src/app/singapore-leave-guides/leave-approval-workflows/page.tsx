import type { Metadata } from 'next';
import { SingaporeKnowledgeArticle } from '@/components/SingaporeKnowledgeArticle';

export const metadata: Metadata = {
  alternates: { canonical: '/singapore-leave-guides/leave-approval-workflows' },
  openGraph: { url: '/singapore-leave-guides/leave-approval-workflows' },
  title: 'Leave approval workflows in Singapore: policy versus process',
  description: 'Design a leave approval process that records requests and decisions without confusing company workflow with statutory entitlement.',
};

export default function Page() {
  return <SingaporeKnowledgeArticle title="Leave approval workflows in Singapore: policy versus process" intro="Design a leave approval process that records requests and decisions without confusing company workflow with statutory entitlement." reviewed="21 September 2026" sections={[{"heading":"Entitlement and approval are different questions","paragraphs":["Some statutory leave types have eligibility conditions while employers also need an operational process for notice, scheduling, evidence, and approval. The exact process depends on the leave type and applicable rules.","For example, MOM's childcare guidance discusses mutually agreeing on a suitable time and encourages employers to grant leave for matters that cannot be postponed. That operational guidance should not be reduced to a generic approve/reject rule."]},{"heading":"Use workflow to make responsibility visible","paragraphs":["LeaveMaestro supports configured approvers and request status. Organisations remain responsible for designing workflows that comply with applicable leave requirements and their own employment policies."]}]} sources={[{"label":"MOM: Taking childcare leave","href":"https://www.mom.gov.sg/employment-practices/leave/childcare-leave/taking-childcare-leave"},{"label":"MOM: Annual leave guidance","href":"https://www.mom.gov.sg/employment-practices/leave/annual-leave/eligibility-and-entitlement"}]} related={[{"label":"Approval workflows in LeaveMaestro","href":"/approval-workflows"},{"label":"Leave management","href":"/leave-management"}]} />;
}
