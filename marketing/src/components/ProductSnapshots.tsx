import type { ReactNode } from 'react';

type SnapshotFrameProps = {
  title: string;
  eyebrow: string;
  children: ReactNode;
};

function SnapshotFrame({ title, eyebrow, children }: SnapshotFrameProps) {
  return (
    <figure className="overflow-hidden rounded-[2rem] border border-brand-100 bg-white shadow-soft">
      <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
        <div>
          <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-brand-600">{eyebrow}</p>
          <p className="mt-1 text-sm font-semibold text-slate-900">{title}</p>
        </div>
        <div className="flex gap-1.5" aria-hidden="true">
          <span className="h-2 w-2 rounded-full bg-brand-100" />
          <span className="h-2 w-2 rounded-full bg-brand-300" />
          <span className="h-2 w-2 rounded-full bg-brand-600" />
        </div>
      </div>
      <div className="bg-brand-50/40 p-5">{children}</div>
    </figure>
  );
}

export function EmployeeDashboardSnapshot() {
  return (
    <SnapshotFrame eyebrow="Employee self-service" title="My leave dashboard">
      <div className="grid gap-3 sm:grid-cols-3">
        {[
          ['Annual leave', '12.5'],
          ['Sick leave', '14'],
          ['Childcare', '4'],
        ].map(([label, value]) => (
          <div key={label} className="rounded-2xl border border-brand-100 bg-white p-4">
            <p className="text-xs font-medium text-slate-500">{label}</p>
            <p className="mt-2 text-2xl font-bold text-slate-950">
              {value} <span className="text-xs font-medium text-slate-400">days</span>
            </p>
          </div>
        ))}
      </div>
      <div className="mt-3 flex items-center justify-between gap-4 rounded-2xl bg-brand-600 px-4 py-3 text-white">
        <div>
          <p className="text-xs font-semibold">Ready to take time off?</p>
          <p className="text-[10px] text-brand-50">Balances update with approved requests.</p>
        </div>
        <span className="shrink-0 rounded-lg bg-white px-3 py-2 text-xs font-bold text-brand-700">Apply leave</span>
      </div>
    </SnapshotFrame>
  );
}

export function LeaveApplicationSnapshot() {
  return (
    <SnapshotFrame eyebrow="Leave request" title="Apply for annual leave">
      <div className="grid gap-3 sm:grid-cols-2">
        {[
          ['Leave type', 'Annual Leave'],
          ['Duration', '2 days'],
          ['From', '14 Sep 2026'],
          ['To', '15 Sep 2026'],
        ].map(([label, value]) => (
          <div key={label} className="rounded-xl border border-slate-200 bg-white p-3">
            <p className="text-[10px] text-slate-400">{label}</p>
            <p className="mt-1 text-xs font-semibold text-slate-800">{value}</p>
          </div>
        ))}
      </div>
      <div className="mt-3 flex items-center justify-between rounded-xl border border-brand-100 bg-brand-50 p-3">
        <span className="text-xs text-slate-600">Balance after request</span>
        <strong className="text-sm text-brand-700">10.5 days</strong>
      </div>
    </SnapshotFrame>
  );
}

export function ApprovalSnapshot() {
  const requests = [
    ['Aisha Tan', 'Annual Leave · 14–15 Sep · 2 days'],
    ['Marcus Lim', 'Childcare Leave · 18 Sep · 1 day'],
    ['Priya Nair', 'Sick Leave · 22 Sep · 1 day'],
  ];

  return (
    <SnapshotFrame eyebrow="Manager workflow" title="Approval inbox">
      <div className="space-y-2">
        {requests.map(([name, detail], index) => (
          <div key={name} className="flex items-center gap-3 rounded-xl border border-slate-100 bg-white p-3">
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-brand-100 text-xs font-bold text-brand-700">
              {name.split(' ').map((part) => part[0]).join('')}
            </span>
            <div className="min-w-0 flex-1">
              <p className="text-xs font-semibold text-slate-800">{name}</p>
              <p className="text-[10px] text-slate-500">{detail}</p>
            </div>
            <span className={`rounded-full px-2 py-1 text-[9px] font-semibold ${index === 0 ? 'bg-brand-600 text-white' : 'bg-brand-50 text-brand-700'}`}>
              {index === 0 ? 'Review' : 'Pending'}
            </span>
          </div>
        ))}
      </div>
    </SnapshotFrame>
  );
}

export function PolicyBuilderSnapshot() {
  return (
    <SnapshotFrame eyebrow="HR configuration" title="Annual Leave policy">
      <div className="grid gap-3 sm:grid-cols-[0.8fr_1.2fr]">
        <div className="rounded-2xl border border-brand-100 bg-white p-4">
          <p className="text-[10px] uppercase tracking-wider text-slate-400">Entitlement</p>
          <p className="mt-2 text-3xl font-bold text-brand-700">14 <span className="text-sm">days</span></p>
          <p className="mt-1 text-[10px] text-slate-500">Singapore · Annual Leave</p>
        </div>
        <div className="space-y-2">
          {[
            ['Service', 'Less than 24 months'],
            ['Proration', 'Half-day denomination'],
            ['Accrual', 'Upfront'],
          ].map(([label, value]) => (
            <div key={label} className="flex items-center justify-between gap-3 rounded-xl border border-slate-100 bg-white px-3 py-2.5">
              <span className="text-[10px] text-slate-500">{label}</span>
              <span className="text-right text-[10px] font-semibold text-slate-800">{value}</span>
            </div>
          ))}
        </div>
      </div>
    </SnapshotFrame>
  );
}

export function AskMaestroSnapshot() {
  return (
    <SnapshotFrame eyebrow="Ask LeaveMaestro" title="Explain an entitlement">
      <div className="ml-auto max-w-[85%] rounded-2xl rounded-br-sm bg-brand-600 px-4 py-3 text-xs text-white">
        Why does staff 001 have 5.5 days of annual leave?
      </div>
      <div className="mt-3 max-w-[92%] rounded-2xl rounded-bl-sm border border-brand-100 bg-white p-4">
        <p className="text-xs font-semibold text-slate-900">Annual Leave was prorated from the employee&apos;s join date.</p>
        <div className="mt-3 rounded-xl bg-brand-50 p-3 text-[11px] text-slate-600">
          <strong className="text-brand-700">14 days</strong> × eligible portion of the year → <strong className="text-brand-700">5.5 days</strong>
        </div>
        <p className="mt-2 text-[10px] text-slate-400">Based on LeaveMaestro entitlement and policy data</p>
      </div>
    </SnapshotFrame>
  );
}

export function JurisdictionSnapshot() {
  return (
    <SnapshotFrame eyebrow="Multi-country" title="Jurisdiction-aware leave setup">
      <div className="flex gap-2 overflow-hidden">
        {['Singapore', 'Malaysia', 'Indonesia'].map((country, index) => (
          <span key={country} className={`whitespace-nowrap rounded-full px-3 py-2 text-[10px] font-semibold ${index === 0 ? 'bg-brand-600 text-white' : 'border border-brand-100 bg-white text-brand-700'}`}>
            {country}
          </span>
        ))}
      </div>
      <div className="mt-4 grid gap-2 sm:grid-cols-2">
        {['Annual Leave', 'Sick Leave', 'Hospitalisation Leave', 'Childcare Leave'].map((leave) => (
          <div key={leave} className="flex items-center gap-2 rounded-xl bg-white px-3 py-2.5 text-[10px] font-medium text-slate-700">
            <span className="h-2 w-2 rounded-full bg-brand-500" />
            {leave}
          </div>
        ))}
      </div>
    </SnapshotFrame>
  );
}
