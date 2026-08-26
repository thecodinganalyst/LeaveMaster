import { fireEvent, render, screen } from '@testing-library/react';
import { Form } from 'antd';
import { describe, expect, it } from 'vitest';

import { StaffEmploymentTypeField } from './StaffEmploymentTypeField.tsx';

const renderField = (initialValue?: string | null) => render(
  <Form initialValues={{ employmentType: initialValue }}>
    <StaffEmploymentTypeField />
  </Form>,
);

describe('StaffEmploymentTypeField', () => {
  it('renders all supported employment types with human-readable labels', () => {
    renderField();

    fireEvent.mouseDown(screen.getByRole('combobox'));
    expect(screen.getByText('Full Time')).toBeInTheDocument();
    expect(screen.getByText('Part Time')).toBeInTheDocument();
    expect(screen.getByText('Casual')).toBeInTheDocument();
    expect(screen.getByText('Contract')).toBeInTheDocument();
    expect(screen.getByText('Intern')).toBeInTheDocument();
  });

  it('loads an existing employment type for edit forms', () => {
    renderField('PART_TIME');
    expect(screen.getByText('Part Time')).toBeInTheDocument();
  });

  it('shows the not-specified placeholder for legacy null values', () => {
    renderField(null);
    expect(screen.getByText('Not specified')).toBeInTheDocument();
  });
});
