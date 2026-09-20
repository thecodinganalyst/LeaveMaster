import type { Metadata } from 'next';
import { SingaporeKnowledgeArticle } from '@/components/SingaporeKnowledgeArticle';

export const metadata: Metadata = {
  alternates: { canonical: '/singapore-leave-guides/carry-forward-policies' },
  openGraph: { url: '/singapore-leave-guides/carry-forward-policies' },
  title: 'Singapore annual leave carry-forward: statutory rules and company policy',
  description: 'Separate the statutory carry-forward rule for employees covered by Part 4 of the Employment Act from broader company carry-forward policies.',
};

export default function Page() {
  return <SingaporeKnowledgeArticle title="Singapore annual leave carry-forward: statutory rules and company policy" intro="Separate the statutory carry-forward rule for employees covered by Part 4 of the Employment Act from broader company carry-forward policies." reviewed="21 September 2026" sections={[{"heading":"Who has a statutory carry-forward protection","paragraphs":["MOM states that employees covered under Part 4 of the Employment Act must be allowed to carry forward unused statutory annual leave to the next 12 months. MOM's guidance identifies the applicable Part 4 salary and worker categories.","This is not the same as saying every employee must have the same carry-forward configuration. Employers may also have contractual policies that are more generous."]},{"heading":"Configure the reviewed policy","paragraphs":["LeaveMaestro can model carry-forward as a policy setting. HR should configure the organisation's applicable rule after confirming employee coverage and contractual terms, rather than treating a software default as legal advice."]}]} sources={[{"label":"MOM: Annual leave in special situations","href":"https://www.mom.gov.sg/employment-practices/leave/annual-leave/special-situations"}]} related={[{"label":"Annual leave entitlement","href":"/singapore-leave-guides/annual-leave-entitlement"},{"label":"Leave entitlements","href":"/leave-entitlements"}]} />;
}
