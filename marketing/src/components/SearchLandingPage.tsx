import Link from 'next/link';

export type LandingSection = { heading: string; body: string; bullets?: string[] };

export function SearchLandingPage({ eyebrow, title, intro, sections, related }: {
  eyebrow: string;
  title: string;
  intro: string;
  sections: LandingSection[];
  related: { href: string; label: string; description: string }[];
}) {
  return (
    <section className="section">
      <div className="mx-auto max-w-4xl">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">{eyebrow}</p>
        <h1 className="mt-3 text-4xl font-bold tracking-tight text-slate-950 sm:text-5xl">{title}</h1>
        <p className="mt-5 max-w-3xl text-lg leading-8 text-slate-600">{intro}</p>
        <div className="mt-10 flex flex-wrap gap-3">
          <Link href="/features" className="rounded-full bg-brand-600 px-5 py-3 text-sm font-semibold text-white hover:bg-brand-700">Explore features</Link>
          <Link href="/demo" className="rounded-full border border-brand-200 px-5 py-3 text-sm font-semibold text-brand-700 hover:bg-brand-50">Request evaluation access</Link>
        </div>
      </div>

      <div className="mx-auto mt-16 grid max-w-4xl gap-6">
        {sections.map((section) => (
          <article key={section.heading} className="card">
            <h2 className="text-2xl font-semibold text-slate-950">{section.heading}</h2>
            <p className="mt-3 leading-7 text-slate-600">{section.body}</p>
            {section.bullets && <ul className="mt-5 grid gap-3 sm:grid-cols-2">{section.bullets.map((bullet) => <li key={bullet} className="rounded-xl bg-brand-50 px-4 py-3 text-sm text-slate-700">✓ {bullet}</li>)}</ul>}
          </article>
        ))}
      </div>

      <aside className="mx-auto mt-16 max-w-4xl rounded-[2rem] bg-slate-50 p-8" aria-labelledby="related-heading">
        <h2 id="related-heading" className="text-2xl font-semibold text-slate-950">Explore related LeaveMaestro capabilities</h2>
        <div className="mt-6 grid gap-4 sm:grid-cols-2">
          {related.map((item) => <Link key={item.href} href={item.href} className="rounded-2xl border border-slate-200 bg-white p-5 hover:border-brand-300"><strong className="text-slate-950">{item.label}</strong><span className="mt-2 block text-sm leading-6 text-slate-600">{item.description}</span></Link>)}
        </div>
      </aside>
    </section>
  );
}
