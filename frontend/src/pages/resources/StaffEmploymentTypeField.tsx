import { Form, Select } from 'antd';

import { EMPLOYMENT_TYPE_OPTIONS } from './employmentTypes.ts';

export const StaffEmploymentTypeField = () => (
  <Form.Item name="employmentType" label="Employment Type">
    <Select
      allowClear
      options={EMPLOYMENT_TYPE_OPTIONS.map((option) => ({ ...option }))}
      placeholder="Not specified"
    />
  </Form.Item>
);
