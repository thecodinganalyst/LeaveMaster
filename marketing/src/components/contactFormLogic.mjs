const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateContactPayload(payload) {
  if (!payload.name?.trim()) return 'Name is required.';
  if (!payload.company?.trim()) return 'Company is required.';
  if (!payload.email?.trim() || !emailPattern.test(payload.email.trim())) return 'Enter a valid work email.';
  if (!payload.enquiryType) return 'Choose an enquiry type.';
  if (!payload.message?.trim()) return 'Message is required.';
  return null;
}

export function submissionState(state, event) {
  if (event === 'submit') return 'submitting';
  if (event === 'success') return 'success';
  if (event === 'error') return 'error';
  return state;
}

export function resolveContactApiUrl(apiUrl, appUrl, nodeEnv = 'development') {
  const explicitApiUrl = apiUrl?.trim();
  if (explicitApiUrl) return explicitApiUrl.replace(/\/$/, '');

  const explicitAppUrl = appUrl?.trim();
  if (explicitAppUrl) return explicitAppUrl.replace(/\/$/, '');

  return nodeEnv === 'production' ? 'https://app.leavemaestro.com' : 'http://localhost:8080';
}

export async function submitContactEnquiry(payload, apiBaseUrl, fetchImpl = fetch) {
  const validationError = validateContactPayload(payload);
  if (validationError) throw new Error(validationError);

  let response;
  try {
    response = await fetchImpl(`${apiBaseUrl.replace(/\/$/, '')}/api/public/contact`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
  } catch {
    throw new Error('Unable to reach LeaveMaestro. Please try again in a moment.');
  }

  let body = {};
  try {
    body = await response.json();
  } catch {
    // Use the generic fallback below when the server did not return JSON.
  }

  if (!response.ok) {
    throw new Error(body.message || 'We could not send your enquiry. Please try again.');
  }
  return body;
}
