import type { Metadata } from 'next';
import { SingaporeKnowledgeArticle } from '@/components/SingaporeKnowledgeArticle';

export const metadata: Metadata = {
  alternates: { canonical: '/singapore-leave-guides/part-time-employee-leave' },
  openGraph: { url: '/singapore-leave-guides/part-time-employee-leave' },
  title: 'Singapore leave for part-time employees',
  description: 'Review MOM\'s hour-based approach to annual, sick, hospitalisation and childcare leave for part-time employees.',
};

export default function Page() {
  return <SingaporeKnowledgeArticle title="Singapore leave for part-time employees" intro="Review MOM's hour-based approach to annual, sick, hospitalisation and childcare leave for part-time employees." reviewed="21 September 2026" sections={[{"heading":"Entitlements reflect working hours","paragraphs":["MOM states that part-time employees covered by the Employment Act are entitled to paid annual and sick leave, and eligible parents may qualify for parental and childcare leave. Leave and pay are adjusted with reference to working hours.","For annual leave, MOM describes an hours-based proportional calculation against a similar full-time employee. Sick and hospitalisation entitlements for part-time employees are likewise calculated in hours."]},{"heading":"Do not reuse full-time assumptions","paragraphs":["Working schedules and employment arrangements need to be captured accurately before implementing a part-time leave policy. A leave system should not simply copy a full-time day balance to a part-time employee."]}]} sources={[{"label":"MOM: Leave for part-time employees","href":"https://www.mom.gov.sg/employment-practices/part-time-employment/leave"}]} related={[{"label":"Employee leave calendar","href":"/employee-leave-calendar"},{"label":"Leave entitlements","href":"/leave-entitlements"}]} />;
}
