'use client';

import { useMemo, useState } from 'react';
import { calculateSingaporeAnnualLeave } from './singaporeAnnualLeaveLogic.mjs';

export function SingaporeAnnualLeaveCalculator() {
  const [startDate, setStartDate] = useState('');
  const [calculationDate, setCalculationDate] = useState('');
  const [employmentEnded, setEmploymentEnded] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const result = useMemo(() => submitted ? calculateSingaporeAnnualLeave({ startDate, calculationDate, employmentEnded }) : null, [startDate, calculationDate, employmentEnded, submitted]);

  return <div className="rounded-[2rem] border border-brand-100 bg-white p-6 shadow-sm sm:p-8">
    <form onSubmit={(event) => { event.preventDefault(); setSubmitted(true); }} noValidate>
      <div className="grid gap-5 sm:grid-cols-2">
        <label className="text-sm font-semibold text-slate-800">Employment start date<input aria-describedby="start-help" required type="date" value={startDate} onChange={(e) => { setStartDate(e.target.value); setSubmitted(false); }} className="mt-2 block w-full rounded-xl border border-slate-300 px-4 py-3 font-normal" /><span id="start-help" className="mt-1 block text-xs font-normal text-slate-500">Use the date employment with this employer began.</span></label>
        <label className="text-sm font-semibold text-slate-800">Calculation date<input required type="date" value={calculationDate} onChange={(e) => { setCalculationDate(e.target.value); setSubmitted(false); }} className="mt-2 block w-full rounded-xl border border-slate-300 px-4 py-3 font-normal" /></label>
      </div>
      <label className="mt-5 flex items-start gap-3 text-sm text-slate-700"><input type="checkbox" checked={employmentEnded} onChange={(e) => { setEmploymentEnded(e.target.checked); setSubmitted(false); }} className="mt-1 h-4 w-4" /><span><strong>Employment has ended on the calculation date.</strong><br />Use this to estimate proration in the current service year. Notice periods and approved unpaid leave can affect the statutory calculation; see the MOM source below.</span></label>
      <button type="submit" className="mt-6 rounded-full bg-brand-600 px-6 py-3 font-semibold text-white hover:bg-brand-700">Calculate entitlement</button>
    </form>
    {result && <div aria-live="polite" className="mt-8 border-t border-slate-200 pt-6">
      {!result.valid ? <p role="alert" className="font-semibold text-red-700">{result.error}</p> : <>
        <p className="text-sm font-semibold uppercase tracking-[0.15em] text-brand-700">Estimated statutory minimum</p>
        <p className="mt-2 text-4xl font-bold text-slate-950">{result.entitlementDays} {result.entitlementDays === 1 ? 'day' : 'days'}</p>
        {!result.eligible ? <p className="mt-3 text-slate-600">Fewer than 3 completed months of service: MOM states paid annual leave eligibility begins after at least 3 months for employees covered by the Employment Act.</p> :
        <p className="mt-3 text-slate-600">{result.prorated ? `Using ${result.completedMonths < 12 ? result.completedMonths : result.completedMonths % 12} completed month(s) in the relevant service year and the ${result.annualTierDays}-day year-${result.yearOfService} statutory tier, rounded to the nearest whole day.` : `For ongoing employment, the statutory minimum tier for service year ${result.yearOfService} is ${result.annualTierDays} days.`}</p>}
      </>}
    </div>}
  </div>;
}
