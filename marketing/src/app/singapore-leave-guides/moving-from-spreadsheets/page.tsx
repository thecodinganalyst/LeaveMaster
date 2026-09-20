import type { Metadata } from 'next';
import { SingaporeKnowledgeArticle } from '@/components/SingaporeKnowledgeArticle';

export const metadata: Metadata = {
  alternates: { canonical: '/singapore-leave-guides/moving-from-spreadsheets' },
  openGraph: { url: '/singapore-leave-guides/moving-from-spreadsheets' },
  title: 'Moving Singapore leave management from spreadsheets to a leave system',
  description: 'Plan a controlled migration from spreadsheet balances to policy-aware leave records without treating historical formulas as automatically correct.',
};

export default function Page() {
  return <SingaporeKnowledgeArticle title="Moving Singapore leave management from spreadsheets to a leave system" intro="Plan a controlled migration from spreadsheet balances to policy-aware leave records without treating historical formulas as automatically correct." reviewed="21 September 2026" sections={[{"heading":"Start with policy, not spreadsheet columns","paragraphs":["Before migrating balances, document leave types, eligibility rules, entitlement rules, carry-forward treatment, working schedules, public-holiday calendars, approval relationships, join dates, and termination dates. Compare statutory-sensitive rules against current official guidance."]},{"heading":"Reconcile opening balances","paragraphs":["Treat imported balances as data requiring reconciliation. Keep an audit trail of the source date and adjustments, test representative employees, and confirm edge cases such as mid-year joiners and part-time schedules before relying on the new system."]},{"heading":"Separate configuration from legal interpretation","paragraphs":["LeaveMaestro provides configurable policy and workflow capabilities. The migration exercise should be owned by the organisation's HR/policy stakeholders, with legal or professional advice sought where needed."]}]} sources={[{"label":"MOM: Leave overview","href":"https://www.mom.gov.sg/employment-practices/leave"},{"label":"MOM: Public holidays","href":"https://www.mom.gov.sg/employment-practices/public-holidays-entitlement-and-pay"}]} related={[{"label":"Leave management","href":"/leave-management"},{"label":"Singapore leave management","href":"/singapore-leave-management"},{"label":"Leave entitlements","href":"/leave-entitlements"}]} />;
}
