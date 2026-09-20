import type { Metadata } from 'next';
import { SingaporeKnowledgeArticle } from '@/components/SingaporeKnowledgeArticle';

export const metadata: Metadata = {
  alternates: { canonical: '/singapore-leave-guides/childcare-leave' },
  openGraph: { url: '/singapore-leave-guides/childcare-leave' },
  title: 'Singapore childcare leave: eligibility and administration',
  description: 'Understand the current childcare leave framework and why citizenship, child age and service criteria need to be represented carefully.',
};

export default function Page() {
  return <SingaporeKnowledgeArticle title="Singapore childcare leave: eligibility and administration" intro="Understand the current childcare leave framework and why citizenship, child age and service criteria need to be represented carefully." reviewed="21 September 2026" sections={[{"heading":"Current published framework","paragraphs":["MOM currently states that eligible working parents of Singapore citizen children can receive 6 days of paid childcare leave per year, while parents of non-citizen children can receive 2 days under the Employment Act, subject to the relevant eligibility criteria.","MOM's page also flags announced future enhancements to childcare leave. Because implementation details can change, organisations should re-check the official source before changing policy configuration."]},{"heading":"Taking childcare leave","paragraphs":["MOM explains that childcare leave can be used to spend time with a child for any reason and should be arranged with the employer. It also lists restrictions including no encashment of unused childcare leave and no transfer between spouses."]}]} sources={[{"label":"MOM: Childcare leave eligibility and entitlement","href":"https://www.mom.gov.sg/employment-practices/leave/childcare-leave/eligibility-and-entitlement"},{"label":"MOM: Taking childcare leave","href":"https://www.mom.gov.sg/employment-practices/leave/childcare-leave/taking-childcare-leave"}]} related={[{"label":"Infant care leave","href":"/singapore-leave-guides/infant-care-leave"},{"label":"Part-time leave","href":"/singapore-leave-guides/part-time-employee-leave"}]} />;
}
