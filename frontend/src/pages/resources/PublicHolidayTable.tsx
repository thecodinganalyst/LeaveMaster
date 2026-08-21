import { Table } from 'antd';

interface PublicHoliday {
  holidayDate?: string;
  holidayName?: string;
}

interface Props {
  value: unknown;
}

const toPublicHolidays = (value: unknown): PublicHoliday[] => {
  if (!Array.isArray(value)) return [];
  return value.filter((holiday): holiday is PublicHoliday => Boolean(holiday) && typeof holiday === 'object');
};

export const PublicHolidayTable = ({ value }: Props) => {
  const holidays = toPublicHolidays(value);

  return (
    <Table<PublicHoliday>
      rowKey={(holiday, index) => `${holiday.holidayDate ?? 'holiday'}-${holiday.holidayName ?? index}`}
      dataSource={holidays}
      pagination={false}
      size="small"
      scroll={{ x: true }}
      locale={{ emptyText: 'No public holidays' }}
      columns={[
        {
          title: 'Date',
          dataIndex: 'holidayDate',
          key: 'holidayDate',
          render: (date: string | undefined) => date || '—',
        },
        {
          title: 'Holiday name',
          dataIndex: 'holidayName',
          key: 'holidayName',
          render: (name: string | undefined) => name || '—',
        },
      ]}
    />
  );
};
