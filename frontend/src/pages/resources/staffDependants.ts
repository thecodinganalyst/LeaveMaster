import { apiFetch } from '../../api/http.ts';

export interface StaffDependantValue {
  id?: string;
  name: string;
  relationshipCode: string;
  dateOfBirth?: string | null;
  citizenshipCode?: string | null;
  residencyCode?: string | null;
  adoptionDate?: string | null;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
  active?: boolean;
}

export const loadStaffDependants = (staffId: string) =>
  apiFetch<StaffDependantValue[]>(`/api/staff/${encodeURIComponent(staffId)}/dependants`);

const payloadFor = (dependant: StaffDependantValue) => ({
  name: dependant.name,
  relationshipCode: dependant.relationshipCode,
  dateOfBirth: dependant.dateOfBirth || null,
  citizenshipCode: dependant.citizenshipCode || null,
  residencyCode: dependant.residencyCode || null,
  adoptionDate: dependant.adoptionDate || null,
  effectiveFrom: dependant.effectiveFrom || null,
  effectiveTo: dependant.effectiveTo || null,
  active: dependant.active ?? true,
});

export const syncStaffDependants = async (staffId: string, dependants?: StaffDependantValue[]) => {
  if (dependants === undefined) return;

  const existing = await loadStaffDependants(staffId);
  const existingById = new Map(existing.map((dependant) => [dependant.id, dependant]));
  const requestedIds = new Set(dependants.map((dependant) => dependant.id).filter(Boolean));

  for (const dependant of dependants) {
    if (dependant.id && existingById.has(dependant.id)) {
      await apiFetch(`/api/staff/${encodeURIComponent(staffId)}/dependants/${encodeURIComponent(dependant.id)}`, {
        method: 'PUT',
        body: JSON.stringify(payloadFor(dependant)),
      });
    } else {
      await apiFetch(`/api/staff/${encodeURIComponent(staffId)}/dependants`, {
        method: 'POST',
        body: JSON.stringify(payloadFor(dependant)),
      });
    }
  }

  for (const dependant of existing) {
    if (!dependant.id || requestedIds.has(dependant.id)) continue;
    await apiFetch(`/api/staff/${encodeURIComponent(staffId)}/dependants/${encodeURIComponent(dependant.id)}`, {
      method: 'DELETE',
    });
  }
};
