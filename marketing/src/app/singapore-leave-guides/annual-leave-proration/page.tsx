import type { Metadata } from 'next';
import { SingaporeKnowledgeArticle } from '@/components/SingaporeKnowledgeArticle';

export const metadata: Metadata = {
  alternates: { canonical: '/singapore-leave-guides/annual-leave-proration' },
  openGraph: { url: '/singapore-leave-guides/annual-leave-proration' },
  title: 'Singapore annual leave proration for employees joining or leaving mid-year',
  description: 'See how MOM describes pro-rated annual leave based on completed months of service and what HR teams should capture in leave-system configuration.',
};

export default function Page() {
  return <SingaporeKnowledgeArticle title="Singapore annual leave proration for employees joining or leaving mid-year" intro="See how MOM describes pro-rated annual leave based on completed months of service and what HR teams should capture in leave-system configuration." reviewed="21 September 2026" sections={[{"heading":"MOM's completed-month approach","paragraphs":["For employees covered by the Employment Act, MOM says annual leave is pro-rated when the employee has worked at least 3 months but less than a year, based on completed months of service. MOM also describes proration within the current year of service when an employee has worked more than a year.","MOM's published formula is completed months of service divided by 12, multiplied by the annual leave entitlement. Its statutory example rounds a fraction below half down and half or more up to a full day."]},{"heading":"Company policy and system configuration","paragraphs":["A company can have contractual rules that are more generous than the statutory floor. LeaveMaestro should be configured to the organisation's reviewed policy; do not infer the legal rule solely from a software default."]}]} sources={[{"label":"MOM: Annual leave eligibility, entitlement and proration","href":"https://www.mom.gov.sg/employment-practices/leave/annual-leave/eligibility-and-entitlement"}]} related={[{"label":"Annual leave entitlement","href":"/singapore-leave-guides/annual-leave-entitlement"},{"label":"Leave entitlements","href":"/leave-entitlements"}]} />;
}
