import type { Metadata } from 'next';
import { SingaporeKnowledgeArticle } from '@/components/SingaporeKnowledgeArticle';

export const metadata: Metadata = {
  alternates: { canonical: '/singapore-leave-guides/annual-leave-entitlement' },
  openGraph: { url: '/singapore-leave-guides/annual-leave-entitlement' },
  title: 'Singapore annual leave entitlement: eligibility and statutory minimums',
  description: 'Understand Singapore statutory annual leave eligibility, service-based minimum entitlements and how to keep company policy separate from the legal floor.',
};

export default function Page() {
  return <SingaporeKnowledgeArticle title="Singapore annual leave entitlement: eligibility and statutory minimums" intro="Understand Singapore statutory annual leave eligibility, service-based minimum entitlements and how to keep company policy separate from the legal floor." reviewed="21 September 2026" sections={[{"heading":"Statutory baseline","paragraphs":["MOM states that employees covered by the Employment Act become entitled to paid annual leave after at least 3 months of service. The statutory minimum starts at 7 days in the first year of service and increases by one day for each additional year, reaching 14 days from the eighth year onward.","An employer may provide a more generous contractual entitlement. When configuring LeaveMaestro, the organisation's actual policy should be modelled rather than assuming the statutory minimum is every employee's company entitlement."]},{"heading":"Why service dates matter","paragraphs":["MOM measures the year of service from the employee's start date. Accurate join dates are therefore important both when reviewing statutory eligibility and when configuring entitlement rules in a leave system."]}]} sources={[{"label":"MOM: Annual leave eligibility and entitlement","href":"https://www.mom.gov.sg/employment-practices/leave/annual-leave/eligibility-and-entitlement"}]} related={[{"label":"Annual leave calculator","href":"/tools/singapore-annual-leave-calculator"},{"label":"Mid-year and proration","href":"/singapore-leave-guides/annual-leave-proration"},{"label":"Leave entitlements in LeaveMaestro","href":"/leave-entitlements"}]} />;
}
