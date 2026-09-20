import type { Metadata } from 'next';
import { SearchLandingPage } from '@/components/SearchLandingPage';

export const metadata: Metadata = {
  alternates: { canonical: '/open-source-leave-management' },
  openGraph: { url: '/open-source-leave-management' },
  title: 'Open-source leave management software',
  description: 'LeaveMaestro is an open-source leave-management project. Teams can inspect the source, contribute to it, or operate their own instance rather than depending only on a hosted SaaS product.',
};

export default function Page() {
  return <SearchLandingPage eyebrow="Open source" title="Run leave management on code you can inspect" intro="LeaveMaestro is an open-source leave-management project. Teams can inspect the source, contribute to it, or operate their own instance rather than depending only on a hosted SaaS product." sections={[{"heading":"Inspect the implementation","body":"The source repository includes the application, infrastructure definitions, documentation, and automated quality checks used by the project."},{"heading":"Self-host when that fits your needs","body":"The project is designed so technically capable teams can deploy their own environment and retain responsibility for its operation, security, data, and configuration.","bullets":["Apache-2.0 source","Infrastructure as code","Technical documentation","Automated tests"]},{"heading":"Hosted access is evaluation-only","body":"The project-hosted environment is temporary, request-only evaluation access and is not positioned as a production SaaS service."}]} related={[{"href":"/features","label":"Product features","description":"See what the open-source application supports."},{"href":"/leave-management","label":"Leave management","description":"Explore the core leave workflow."},{"href":"/contact","label":"Contact","description":"Ask about the project or evaluation access."}]} resources={[{href:"https://github.com/thecodinganalyst/LeaveMaster",label:"Browse the source on GitHub",external:true},{href:"https://thecodinganalyst.github.io/LeaveMaster/",label:"Technical documentation",external:true}]} />;
}
