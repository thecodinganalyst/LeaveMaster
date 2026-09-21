import type { Metadata } from 'next';
import { SearchLandingPage } from '@/components/SearchLandingPage';

export const metadata: Metadata = {
  alternates: { canonical: '/ai-leave-assistant' },
  openGraph: { url: '/ai-leave-assistant' },
  title: 'AI leave assistant for policy and balance explanations',
  description: 'Ask LeaveMaestro is the application\'s AI-assisted interface for helping authorised users understand leave data and policy outcomes. It complements the configured leave workflow rather than replacing policy rules or access controls.',
};

export default function Page() {
  return <SearchLandingPage eyebrow="Ask LeaveMaestro" title="Explain authorised leave information with Ask LeaveMaestro" intro="Ask LeaveMaestro is the application's AI-assisted interface for helping authorised users understand leave data and policy outcomes. It complements the configured leave workflow rather than replacing policy rules or access controls." sections={[{"heading":"Explain, rather than invent, leave outcomes","body":"The assistant is intended to work from authorised application information and policy context so users can understand why balances or entitlements look the way they do."},{"heading":"Respect application access","body":"AI assistance sits behind the application's authentication and authorisation model; it is not a public chatbot with unrestricted employee data.","bullets":["Authorised context","Policy explanations","Leave-data questions","Controlled access"]},{"heading":"Keep deterministic rules in the leave engine","body":"Eligibility, entitlement, proration and workflow decisions belong in configured application rules. The assistant helps explain those outcomes instead of becoming the source of truth."}]} related={[{"href":"/leave-entitlements","label":"Leave entitlements","description":"See the policy data the assistant can help explain."},{"href":"/leave-management","label":"Leave management","description":"Understand the underlying workflow."},{"href":"/features","label":"Features","description":"Explore Ask LeaveMaestro alongside other capabilities."}]} resources={[{href:"https://thecodinganalyst.github.io/LeaveMaster/assistant-security/",label:"AI assistant security",external:true},{href:"https://thecodinganalyst.github.io/LeaveMaster/assistant-setup/",label:"Ask LeaveMaestro setup",external:true}]} />;
}
