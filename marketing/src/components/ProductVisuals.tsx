const iconPaths = {
  policy: 'M4 5h16M4 10h10M4 15h16M4 20h10',
  selfService: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-7 8a7 7 0 0 1 14 0',
  approval: 'm5 12 4 4L19 6',
  jurisdiction: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18Zm0 0c2.2 2.5 3.3 5.5 3.3 9S14.2 18.5 12 21m0-18C9.8 5.5 8.7 8.5 8.7 12S9.8 18.5 12 21M3.5 9h17M3.5 15h17',
  assistant: 'M5 5h14v10H9l-4 4V5Zm4 4h.01M12 9h.01M15 9h.01',
};

type FeatureIconProps = {
  kind: keyof typeof iconPaths;
  label?: string;
};

export function FeatureIcon({ kind, label }: FeatureIconProps) {
  return (
    <span className="inline-flex h-12 w-12 items-center justify-center rounded-2xl border border-brand-100 bg-brand-50 text-brand-700" aria-hidden={label ? undefined : true} aria-label={label}>
      <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d={iconPaths[kind]} />
      </svg>
    </span>
  );
}

export function HeroProductVisual() {
  return (
    <figure className="relative overflow-hidden rounded-[2rem] border border-brand-100 bg-brand-50/70 p-4 shadow-soft sm:p-6" aria-label="Illustration of LeaveMaestro leave management dashboard">
      <div className="rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
          <div className="flex items-center gap-2">
            <span className="h-2.5 w-2.5 rounded-full bg-brand-600" />
            <span className="text-xs font-semibold text-slate-700">LeaveMaestro</span>
          </div>
          <span className="rounded-full bg-brand-50 px-2.5 py-1 text-[10px] font-semibold text-brand-700">Singapore</span>
        </div>
        <div className="grid gap-3 p-4 sm:grid-cols-[0.8fr_1.2fr]">
          <div className="space-y-3">
            <div className="rounded-xl border border-brand-100 bg-brand-50 p-3">
              <p className="text-[10px] font-semibold uppercase tracking-wider text-brand-700">Annual leave</p>
              <p className="mt-1 text-2xl font-bold text-slate-950">12.5 days</p>
              <p className="mt-1 text-xs text-slate-500">Available balance</p>
            </div>
            <div className="rounded-xl border border-slate-200 p-3">
              <p className="text-xs font-semibold text-slate-800">Policy applied</p>
              <div className="mt-2 space-y-2">
                <span className="block h-2 rounded-full bg-brand-100" />
                <span className="block h-2 w-3/4 rounded-full bg-slate-100" />
              </div>
            </div>
          </div>
          <div className="rounded-xl border border-slate-200 p-3">
            <div className="flex items-center justify-between">
              <p className="text-xs font-semibold text-slate-800">Approval queue</p>
              <span className="text-[10px] font-medium text-brand-700">2 pending</span>
            </div>
            <div className="mt-3 space-y-2">
              {['Annual leave · 2 days', 'Sick leave · 1 day', 'Childcare leave · 1 day'].map((item, index) => (
                <div key={item} className="flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2">
                  <span className="text-[11px] text-slate-600">{item}</span>
                  <span className={`h-2 w-2 rounded-full ${index < 2 ? 'bg-brand-500' : 'bg-slate-300'}`} />
                </div>
              ))}
            </div>
            <div className="mt-3 rounded-lg border border-brand-100 bg-brand-50 px-3 py-2">
              <p className="text-[10px] font-semibold text-brand-700">Ask LeaveMaestro</p>
              <p className="mt-1 text-[11px] text-slate-600">Why is this entitlement 5.5 days?</p>
            </div>
          </div>
        </div>
      </div>
      <figcaption className="sr-only">Representative LeaveMaestro interface showing a leave balance, applied policy, approval queue, and Ask LeaveMaestro.</figcaption>
    </figure>
  );
}

const workflow = ['Policy', 'Eligibility', 'Entitlement', 'Request', 'Approval', 'Balance'];

export function LeaveWorkflowVisual() {
  return (
    <figure className="rounded-[2rem] border border-brand-100 bg-white p-5 shadow-sm sm:p-7" aria-labelledby="leave-workflow-caption">
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        {workflow.map((item, index) => (
          <div key={item} className="relative flex min-h-24 flex-col items-center justify-center rounded-2xl bg-brand-50 px-3 text-center">
            <span className="mb-2 flex h-8 w-8 items-center justify-center rounded-full bg-white text-xs font-bold text-brand-700 shadow-sm">{index + 1}</span>
            <span className="text-xs font-semibold text-slate-800">{item}</span>
            {index < workflow.length - 1 && <span className="absolute -right-2 top-1/2 hidden -translate-y-1/2 text-brand-400 lg:block">→</span>}
          </div>
        ))}
      </div>
      <figcaption id="leave-workflow-caption" className="mt-4 text-center text-sm text-slate-500">One connected path from policy rules to the employee&apos;s remaining balance.</figcaption>
    </figure>
  );
}

type Persona = 'employee' | 'manager' | 'hr';

const personaContent: Record<Persona, { title: string; metric: string; label: string; rows: string[] }> = {
  employee: { title: 'Employee', metric: '12.5', label: 'days available', rows: ['Apply for leave', 'My applications', 'My balances'] },
  manager: { title: 'Manager', metric: '2', label: 'requests pending', rows: ['Review request', 'Approve or reject', 'Team calendar'] },
  hr: { title: 'HR', metric: '4', label: 'policy areas', rows: ['Entitlement rules', 'Staff & locations', 'Calendars & holidays'] },
};

export function PersonaVisual({ persona }: { persona: Persona }) {
  const content = personaContent[persona];
  return (
    <div className="rounded-2xl border border-brand-100 bg-white p-4 shadow-sm" aria-label={`${content.title} demo preview`}>
      <div className="flex items-center justify-between">
        <p className="font-semibold text-slate-900">{content.title}</p>
        <span className="h-2.5 w-2.5 rounded-full bg-brand-500" />
      </div>
      <div className="mt-4 rounded-xl bg-brand-50 p-3">
        <span className="text-2xl font-bold text-brand-700">{content.metric}</span>
        <span className="ml-2 text-xs text-slate-500">{content.label}</span>
      </div>
      <div className="mt-3 space-y-2">
        {content.rows.map((row) => <div key={row} className="rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-600">{row}</div>)}
      </div>
    </div>
  );
}
