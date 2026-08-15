export type ContactPayload = {
  name: string;
  company: string;
  email: string;
  phone?: string;
  companySize?: string;
  country?: string;
  enquiryType: string;
  message: string;
  website?: string;
};

export type SubmissionStatus = 'idle' | 'submitting' | 'success' | 'error';

export function validateContactPayload(payload: ContactPayload): string | null;
export function submissionState(state: SubmissionStatus, event: 'submit' | 'success' | 'error' | string): SubmissionStatus;
export function submitContactEnquiry(
  payload: ContactPayload,
  apiBaseUrl: string,
  fetchImpl?: typeof fetch,
): Promise<{ message?: string }>;
