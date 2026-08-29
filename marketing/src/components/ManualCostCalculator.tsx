'use client';

import { useMemo, useState } from 'react';
import {
  calculateManualAdministrationCost,
  formatCurrency,
  supportedCurrencies,
} from './costCalculatorLogic.mjs';

export function ManualCostCalculator() {
  const [employees, setEmployees] = useState('100');
  const [hoursPerMonth, setHoursPerMonth] = useState('12');
  const [hourlyCost, setHourlyCost] = useState('45');
  const [currency, setCurrency] = useState('SGD');

  const result = useMemo(
    () => calculateManualAdministrationCost({ hoursPerMonth, hourlyCost }),
    [hoursPerMonth, hourlyCost],
  );

  const updateNonNegative = (value: string, setter: (value: string) => void) => {
    if (value === '' || Number(value) >= 0) setter(value);
  };

  return (
    <div className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <div className="grid gap-8 lg:grid-cols-[0.9fr_1.1fr] lg:items-start">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-600">Dollarize the status quo</p>
          <h3 className="mt-3 text-2xl font-bold tracking-tight text-slate-950">Estimate the cost of manual leave administration</h3>
          <p className="mt-4 text-slate-600">
            Use your own assumptions to estimate the HR capacity currently spent maintaining leave manually. This is an estimate of your current administration cost, not a guaranteed LeaveMaestro saving.
          </p>

          <div className="mt-6 grid gap-5 sm:grid-cols-2">
            <label className="text-sm font-medium text-slate-800">
              Employees
              <input
                className="mt-2 w-full rounded-xl border border-slate-300 px-3 py-2 text-slate-950"
                type="number"
                min="0"
                inputMode="numeric"
                value={employees}
                onChange={(event) => updateNonNegative(event.target.value, setEmployees)}
              />
              <span className="mt-1 block text-xs font-normal text-slate-500">Context only; it does not change the calculation.</span>
            </label>

            <label className="text-sm font-medium text-slate-800">
              Currency
              <select
                className="mt-2 w-full rounded-xl border border-slate-300 px-3 py-2 text-slate-950"
                value={currency}
                onChange={(event) => setCurrency(event.target.value)}
              >
                {supportedCurrencies.map((option) => (
                  <option key={option} value={option}>{option}</option>
                ))}
              </select>
            </label>

            <label className="text-sm font-medium text-slate-800">
              HR hours per month
              <input
                className="mt-2 w-full rounded-xl border border-slate-300 px-3 py-2 text-slate-950"
                type="number"
                min="0"
                step="0.5"
                inputMode="decimal"
                value={hoursPerMonth}
                onChange={(event) => updateNonNegative(event.target.value, setHoursPerMonth)}
              />
            </label>

            <label className="text-sm font-medium text-slate-800">
              Estimated HR cost per hour
              <input
                className="mt-2 w-full rounded-xl border border-slate-300 px-3 py-2 text-slate-950"
                type="number"
                min="0"
                step="1"
                inputMode="decimal"
                value={hourlyCost}
                onChange={(event) => updateNonNegative(event.target.value, setHourlyCost)}
              />
            </label>
          </div>
        </div>

        <div className="rounded-2xl border border-brand-100 bg-brand-50 p-6" aria-live="polite">
          <p className="text-sm font-medium text-brand-700">Estimated current annual administration cost</p>
          <p className="mt-2 text-4xl font-bold text-slate-950">{formatCurrency(result.annualCost, currency)}</p>

          <div className="mt-6 border-t border-brand-100 pt-6">
            <p className="text-sm font-medium text-brand-700">Estimated HR time each year</p>
            <p className="mt-2 text-3xl font-semibold text-slate-950">{result.annualHours.toLocaleString()} hours</p>
          </div>

          <p className="mt-6 text-sm leading-6 text-slate-600">
            Every month spent interpreting policies, checking eligibility, prorating entitlements, correcting balances, and answering routine leave questions has a measurable cost.
          </p>
          <p className="mt-4 text-xs leading-5 text-slate-500">
            Estimates use the values you enter and are illustrative only. They do not represent guaranteed savings, ROI, or product pricing.
          </p>
        </div>
      </div>
    </div>
  );
}
