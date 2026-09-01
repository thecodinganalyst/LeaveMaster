export type JurisdictionFilteredResource = 'leave-types' | 'leave-calendars';

export interface JurisdictionLeaveTypeSource {
  id?: unknown;
  jurisdictionId?: unknown;
}

const FILTERED_RESOURCES = new Set<string>(['leave-types', 'leave-calendars']);

export const supportsJurisdictionFilter = (resourceName?: string): resourceName is JurisdictionFilteredResource =>
  Boolean(resourceName && FILTERED_RESOURCES.has(resourceName));

export const canUseJurisdictionFilter = (
  resourceSupportsFilter: boolean,
  platformAdmin: boolean,
  canReadJurisdictions: boolean,
  canReadTenantJurisdictions: boolean,
) => resourceSupportsFilter && canReadJurisdictions && (platformAdmin || canReadTenantJurisdictions);

export const buildLeaveTypeJurisdictionMap = (sources: JurisdictionLeaveTypeSource[]) => {
  const result = new Map<string, string>();
  for (const source of sources) {
    const id = String(source.id ?? '').trim();
    const jurisdictionId = String(source.jurisdictionId ?? '').trim();
    if (id && jurisdictionId) result.set(id, jurisdictionId);
  }
  return result;
};

export const getRecordJurisdictionId = (
  resourceName: JurisdictionFilteredResource,
  record: Record<string, unknown>,
  leaveTypeJurisdictions: ReadonlyMap<string, string> = new Map(),
) => {
  const directJurisdictionId = String(record.jurisdictionId ?? '').trim();
  if (directJurisdictionId) return directJurisdictionId;

  if (resourceName !== 'leave-types') return '';
  const sourceLeaveTypeId = String(record.sourceJurisdictionLeaveTypeId ?? '').trim();
  return sourceLeaveTypeId ? leaveTypeJurisdictions.get(sourceLeaveTypeId) ?? '' : '';
};

export const filterRecordsByJurisdiction = (
  resourceName: JurisdictionFilteredResource,
  records: Record<string, unknown>[],
  selectedJurisdiction: string | undefined,
  leaveTypeJurisdictions: ReadonlyMap<string, string> = new Map(),
) => {
  if (!selectedJurisdiction) return records;
  return records.filter((record) => getRecordJurisdictionId(resourceName, record, leaveTypeJurisdictions) === selectedJurisdiction);
};
