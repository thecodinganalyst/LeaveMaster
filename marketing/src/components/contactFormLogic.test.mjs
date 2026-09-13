import test from 'node:test';
import assert from 'node:assert/strict';
import {
  resolveContactApiUrl,
  submissionState,
  submitContactEnquiry,
  validateContactPayload,
} from './contactFormLogic.mjs';

const validPayload = {
  name: 'Jane Doe',
  company: 'Example Pte Ltd',
  email: 'jane@example.com',
  enquiryType: 'GENERAL_ENQUIRY',
  message: 'I have a general question.',
};

test('validates required contact fields', () => {
  assert.equal(validateContactPayload(validPayload), null);
  assert.equal(validateContactPayload({ ...validPayload, company: '' }), 'Company is required.');
  assert.equal(validateContactPayload({ ...validPayload, email: 'invalid' }), 'Enter a valid work email.');
});

test('accepts partnership enquiries without company size', () => {
  assert.equal(validateContactPayload({ ...validPayload, enquiryType: 'PARTNERSHIP' }), null);
  assert.equal(Object.hasOwn(validPayload, 'companySize'), false);
});

test('tracks submission states', () => {
  assert.equal(submissionState('idle', 'submit'), 'submitting');
  assert.equal(submissionState('submitting', 'success'), 'success');
  assert.equal(submissionState('submitting', 'error'), 'error');
});

test('resolves production contact API without falling back to localhost', () => {
  assert.equal(resolveContactApiUrl('https://api.example.com/', 'https://app.leavemaestro.com', 'production'), 'https://api.example.com');
  assert.equal(resolveContactApiUrl(undefined, 'https://app.leavemaestro.com/', 'production'), 'https://app.leavemaestro.com');
  assert.equal(resolveContactApiUrl(undefined, undefined, 'production'), 'https://app.leavemaestro.com');
  assert.equal(resolveContactApiUrl(undefined, undefined, 'development'), 'http://localhost:8080');
});

test('submits JSON to the public contact endpoint without company size', async () => {
  let request;
  const fakeFetch = async (url, options) => {
    request = { url, options };
    return { ok: true, json: async () => ({ message: 'received' }) };
  };

  const result = await submitContactEnquiry(validPayload, 'https://app.leavemaestro.com/', fakeFetch);
  assert.equal(request.url, 'https://app.leavemaestro.com/api/public/contact');
  assert.equal(request.options.method, 'POST');
  assert.deepEqual(JSON.parse(request.options.body), validPayload);
  assert.equal(Object.hasOwn(JSON.parse(request.options.body), 'companySize'), false);
  assert.equal(result.message, 'received');
});

test('surfaces safe server errors', async () => {
  const fakeFetch = async () => ({
    ok: false,
    json: async () => ({ message: 'Too many submissions. Please try again later.' }),
  });

  await assert.rejects(
    () => submitContactEnquiry(validPayload, 'https://app.leavemaestro.com', fakeFetch),
    /Too many submissions/,
  );
});

test('maps browser network failures to a friendly message', async () => {
  for (const browserMessage of ['Load failed', 'Failed to fetch']) {
    const fakeFetch = async () => {
      throw new TypeError(browserMessage);
    };

    await assert.rejects(
      () => submitContactEnquiry(validPayload, 'https://app.leavemaestro.com', fakeFetch),
      (error) => {
        assert.equal(error.message, 'Unable to reach LeaveMaestro. Please try again in a moment.');
        assert.equal(error.message.includes(browserMessage), false);
        return true;
      },
    );
  }
});
