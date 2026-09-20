import type { Metadata } from 'next';
import { SingaporeKnowledgeArticle } from '@/components/SingaporeKnowledgeArticle';

export const metadata: Metadata = {
  alternates: { canonical: '/singapore-leave-guides/infant-care-leave' },
  openGraph: { url: '/singapore-leave-guides/infant-care-leave' },
  title: 'Singapore unpaid infant care leave: what employers should track',
  description: 'Review the current unpaid infant care leave period and the employee/employer scheduling considerations published by MOM.',
};

export default function Page() {
  return <SingaporeKnowledgeArticle title="Singapore unpaid infant care leave: what employers should track" intro="Review the current unpaid infant care leave period and the employee/employer scheduling considerations published by MOM." reviewed="21 September 2026" sections={[{"heading":"Current entitlement period","paragraphs":["MOM's current guidance says an eligible employee can take 12 days of unpaid infant care leave within the relevant 12-month period agreed with the employer, before the child turns 2.","Eligibility has additional conditions, including criteria relating to the child and employment. Always confirm those conditions on the official MOM guidance rather than relying on the number of days alone."]},{"heading":"Operational handling","paragraphs":["MOM says employees should notify the employer and obtain approval. A leave system should therefore distinguish entitlement from the workflow used to request and schedule leave."]}]} sources={[{"label":"MOM: Applying for unpaid infant care leave","href":"https://www.mom.gov.sg/faq/unpaid-infant-care-leave/how-do-i-apply-for-unpaid-infant-care-leave"}]} related={[{"label":"Childcare leave","href":"/singapore-leave-guides/childcare-leave"},{"label":"Approval workflows","href":"/approval-workflows"}]} />;
}
