import type { Metadata } from 'next';
import { SingaporeKnowledgeArticle } from '@/components/SingaporeKnowledgeArticle';

export const metadata: Metadata = {
  alternates: { canonical: '/singapore-leave-guides/public-holidays-and-leave' },
  openGraph: { url: '/singapore-leave-guides/public-holidays-and-leave' },
  title: 'Singapore public holidays and employee leave calendars',
  description: 'Understand how Singapore public holidays interact with employee leave and why a leave system needs the correct jurisdiction calendar.',
};

export default function Page() {
  return <SingaporeKnowledgeArticle title="Singapore public holidays and employee leave calendars" intro="Understand how Singapore public holidays interact with employee leave and why a leave system needs the correct jurisdiction calendar." reviewed="21 September 2026" sections={[{"heading":"Paid public holidays","paragraphs":["MOM states that employees covered by the Employment Act are entitled to 11 paid public holidays each year. Its guidance also explains treatment when a holiday falls on a rest day and arrangements when an employee is required to work on a public holiday."]},{"heading":"Calendar context matters","paragraphs":["Public holidays and employee working days affect the context around leave requests. LeaveMaestro supports jurisdiction-aware calendars so organisations can configure the calendar used for their employees rather than treating every location as identical."]}]} sources={[{"label":"MOM: Public holidays entitlement and pay","href":"https://www.mom.gov.sg/employment-practices/public-holidays-entitlement-and-pay"}]} related={[{"label":"Employee leave calendar","href":"/employee-leave-calendar"},{"label":"Multi-jurisdiction leave","href":"/multi-jurisdiction-leave-management"}]} />;
}
