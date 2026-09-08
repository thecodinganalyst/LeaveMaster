import { Space, Tag } from 'antd';

export const tenantJurisdictionIds = (record: Record<string, unknown>) => {
  const values = Array.isArray(record.jurisdictionIds)
    ? record.jurisdictionIds
    : [];
  const jurisdictionIds = values
    .map((value) => String(value ?? '').trim())
    .filter(Boolean);

  if (jurisdictionIds.length === 0) {
    const legacyJurisdictionId = String(record.jurisdictionId ?? '').trim();
    if (legacyJurisdictionId) jurisdictionIds.push(legacyJurisdictionId);
  }

  return [...new Set(jurisdictionIds)];
};

export const TenantJurisdictionsDisplay = ({ record }: { record: Record<string, unknown> }) => {
  const jurisdictionIds = tenantJurisdictionIds(record);
  if (jurisdictionIds.length === 0) return <span>—</span>;

  return (
    <Space size={[4, 4]} wrap>
      {jurisdictionIds.map((jurisdictionId) => <Tag key={jurisdictionId}>{jurisdictionId}</Tag>)}
    </Space>
  );
};
