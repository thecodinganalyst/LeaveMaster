import test from 'node:test';
import assert from 'node:assert/strict';
import { submissionState, submitContactEnquiry, validateContactPayload } from './contactFormLogic.mjs';

const validPayload = {
  name: 'Jane Doe',
  company: 'Example Pte Ltd',
  email: 'jane@example.com',
  enquiryType: 'PRODUCT_DEMO',
  message: 'Please arrange a demo.',
};

test('validates required contact fields', () => {
  assert.equal(validateContactPayload(validPayload), null);
  assert.equal(validateContactPayload({ ...validPayload, company: '' }), 'Company is required.');
  assert.equal(validateContactPayload({ ...validPayload, email: 'invalid' }), 'Enter a valid work email.');
});

test('tracks submission states', () => {
  assert.equal(submissionState('idle', 'submit'), 'submitting');
  assert.equal(submissionState('submitting', 'success'), 'success');
  assert.equal(submissionState('submitting', 'error'), 'error');
});

test('submits JSON to the public contact endpoint', async () => {
  let request;
  const fakeFetch = async (url, options) => {
    request = { url, options };
    return { ok: true, json: async () => ({ message: 'received' }) };
  };

  const result = await submitContactEnquiry(validPayload, 'https://api.leavemaestro.com/', fakeFetch);
  assert.equal(request.url, 'https://api.leavemaestro.com/api/public/contact');
  assert.equal(request.options.method, 'POST');
  assert.deepEqual(JSON.parse(request.options.body), validPayload);
  assert.equal(result.message, 'received');
});

test('surfaces safe server errors', async () => {
  const fakeFetch = async () => ({
    ok: false,
    json: async () => ({ message: 'Too many submissions. Please try again later.' }),
  });

  await assert.rejects(
    () => submitContactEnquiry(validPayload, 'https://api.leavemaestro.com', fakeFetch),
    /Too many submissions/,
  );
});
