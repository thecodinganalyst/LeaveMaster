import type { Metadata } from 'next';
import { SingaporeKnowledgeArticle } from '@/components/SingaporeKnowledgeArticle';

export const metadata: Metadata = {
  alternates: { canonical: '/singapore-leave-guides/sick-hospitalisation-leave' },
  openGraph: { url: '/singapore-leave-guides/sick-hospitalisation-leave' },
  title: 'Singapore paid sick leave and hospitalisation leave',
  description: 'Review the Singapore statutory eligibility and service-based outpatient and hospitalisation leave framework published by MOM.',
};

export default function Page() {
  return <SingaporeKnowledgeArticle title="Singapore paid sick leave and hospitalisation leave" intro="Review the Singapore statutory eligibility and service-based outpatient and hospitalisation leave framework published by MOM." reviewed="21 September 2026" sections={[{"heading":"Eligibility begins after 3 months","paragraphs":["MOM states that employees covered by the Employment Act can qualify for paid outpatient sick leave and paid hospitalisation leave after serving their employer for at least 3 months, subject to the applicable certification and notification requirements."]},{"heading":"Entitlement scales during months 3 to 6","paragraphs":["MOM publishes pro-rated entitlements of 5/15 days after 3 completed months, 8/30 after 4, 11/45 after 5, and the full 14 days outpatient and 60 days hospitalisation from 6 months onward. The 60-day hospitalisation entitlement includes the outpatient sick-leave entitlement.","Medical certification and the circumstances qualifying as hospitalisation leave matter, so HR processes should retain the distinction rather than treating every medical absence as the same leave type."]}]} sources={[{"label":"MOM: Sick leave eligibility and entitlement","href":"https://www.mom.gov.sg/employment-practices/leave/sick-leave/eligibility-and-entitlement"}]} related={[{"label":"Leave entitlements","href":"/leave-entitlements"},{"label":"Singapore leave management","href":"/singapore-leave-management"}]} />;
}
