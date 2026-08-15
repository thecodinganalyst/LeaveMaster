export interface JurisdictionOptionSource {
  id?: string;
  code: string;
  name: string;
  parentId?: string | null;
}

export interface JurisdictionOption {
  label: string;
  value: string;
}

export const getJurisdictionOptions = (jurisdictions: JurisdictionOptionSource[]): JurisdictionOption[] => {
  const byId = new Map<string, JurisdictionOptionSource>();

  for (const jurisdiction of jurisdictions) {
    if (jurisdiction.id) byId.set(jurisdiction.id, jurisdiction);
    byId.set(jurisdiction.code, jurisdiction);
  }

  return jurisdictions
    .map((jurisdiction) => {
      const parent = jurisdiction.parentId ? byId.get(jurisdiction.parentId) : undefined;
      return {
        label: parent ? `${parent.name} > ${jurisdiction.name}` : jurisdiction.name,
        value: jurisdiction.code,
      };
    })
    .sort((left, right) => left.label.localeCompare(right.label));
};
