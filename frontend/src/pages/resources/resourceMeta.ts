import { useParams } from 'react-router-dom';

const labels: Record<string, string> = {
  employees: 'Employees',
  'leave-requests': 'Leave Requests',
};

export const useResourceMeta = () => {
  const { resource = '' } = useParams();
  const label = labels[resource] ?? resource.replace(/-/g, ' ');

  return {
    name: resource,
    label: label.charAt(0).toUpperCase() + label.slice(1),
  };
};
