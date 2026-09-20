import Link from 'next/link';

export type KnowledgeSection = { heading: string; paragraphs: string[]; bullets?: string[] };

export function SingaporeKnowledgeArticle({ title, intro, reviewed, sections, sources, related }: {
  title: string;
  intro: string;
  reviewed: string;
  sections: KnowledgeSection[];
  sources: { label: string; href: string }[];
  related: { label: string; href: string }[];
}) {
  return (
    <article className="section">
      <div className="mx-auto max-w-3xl">
        <nav aria-label="Breadcrumb" className="text-sm text-slate-500"><Link href="/singapore-leave-management" className="hover:text-brand-700">Singapore leave management</Link><span aria-hidden="true"> / </span><span>Guide</span></nav>
        <h1 className="mt-5 text-4xl font-bold tracking-tight text-slate-950 sm:text-5xl">{title}</h1>
        <p className="mt-5 text-lg leading-8 text-slate-600">{intro}</p>
        <p className="mt-4 text-sm text-slate-500">Reviewed: {reviewed}</p>
        <div className="mt-6 rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm leading-6 text-amber-950"><strong>Information, not legal advice.</strong> Statutory summaries below link to official Singapore sources. Check the latest government guidance and your employment terms before making policy decisions. LeaveMaestro applies the rules configured by your organisation; product behaviour is not a statement of legal entitlement.</div>

        <div className="mt-12 space-y-10">
          {sections.map((section) => <section key={section.heading}><h2 className="text-2xl font-semibold text-slate-950">{section.heading}</h2>{section.paragraphs.map((p) => <p key={p} className="mt-3 leading-7 text-slate-600">{p}</p>)}{section.bullets && <ul className="mt-4 list-disc space-y-2 pl-6 text-slate-600">{section.bullets.map((b) => <li key={b}>{b}</li>)}</ul>}</section>)}
        </div>

        <section className="mt-12 border-t border-slate-200 pt-8"><h2 className="text-2xl font-semibold">Official sources</h2><ul className="mt-4 space-y-2">{sources.map((source) => <li key={source.href}><a className="font-medium text-brand-700 underline decoration-brand-200 underline-offset-4 hover:text-brand-800" href={source.href} rel="noreferrer">{source.label}</a></li>)}</ul></section>
        <aside className="mt-12 rounded-[2rem] bg-slate-50 p-7" aria-labelledby="related-guides"><h2 id="related-guides" className="text-2xl font-semibold">Related guides and product topics</h2><div className="mt-5 flex flex-wrap gap-3">{related.map((item) => <Link key={item.href} href={item.href} className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:border-brand-300 hover:text-brand-700">{item.label}</Link>)}</div></aside>
      </div>
    </article>
  );
}
