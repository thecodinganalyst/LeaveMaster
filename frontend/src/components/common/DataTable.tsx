import type { TableProps } from 'antd';
import { Card, Table } from 'antd';

export const DataTable = <T extends object>({ ...props }: TableProps<T>) => {
  return (
    <Card>
      <Table<T> scroll={{ x: true }} pagination={false} {...props} />
    </Card>
  );
};
