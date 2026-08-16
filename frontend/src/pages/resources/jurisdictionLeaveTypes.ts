export interface JurisdictionLeaveTypeOptionSource {
  id: string;
  jurisdictionId: string;
  code: string;
  name: string;
  active: boolean;
}

export const getJurisdictionLeaveTypeOptions = (
  leaveTypes: JurisdictionLeaveTypeOptionSource[],
  jurisdictionId?: string,
  selectedId?: string,
) => {
  if (!jurisdictionId) return [];

  return leaveTypes
    .filter((leaveType) => leaveType.jurisdictionId === jurisdictionId && (leaveType.active || leaveType.id === selectedId))
    .sort((left, right) => left.name.localeCompare(right.name) || left.code.localeCompare(right.code))
    .map((leaveType) => ({
      label: `${leaveType.name} (${leaveType.code})`,
      value: leaveType.id,
    }));
};
