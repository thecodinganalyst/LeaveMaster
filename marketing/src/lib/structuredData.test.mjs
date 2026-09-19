import assert from 'node:assert/strict';
import test from 'node:test';
import { createSiteStructuredData, createSoftwareStructuredData } from './structuredData.mjs';

test('site structured data uses canonical URLs and supported schema types', () => {
  const data = createSiteStructuredData('https://example.test/');

  assert.deepEqual(data.map((item) => item['@type']), ['Organization', 'WebSite']);
  assert.equal(data[0].url, 'https://example.test');
  assert.equal(data[0]['@id'], 'https://example.test/#organization');
  assert.equal(data[1].publisher['@id'], 'https://example.test/#organization');
});

test('software structured data reflects visible LeaveMaestro claims without ratings or pricing', () => {
  const data = createSoftwareStructuredData('https://example.test/');

  assert.equal(data['@type'], 'SoftwareApplication');
  assert.equal(data.url, 'https://example.test');
  assert.equal(data.applicationCategory, 'BusinessApplication');
  assert.equal(data.operatingSystem, 'Web');
  assert.equal(data.isAccessibleForFree, true);
  assert.equal(data.license, 'https://www.apache.org/licenses/LICENSE-2.0');
  assert.equal(data.codeRepository, 'https://github.com/thecodinganalyst/LeaveMaster');
  assert.equal('aggregateRating' in data, false);
  assert.equal('offers' in data, false);
});

test('structured data is JSON serializable', () => {
  assert.doesNotThrow(() => JSON.stringify(createSiteStructuredData()));
  assert.doesNotThrow(() => JSON.stringify(createSoftwareStructuredData()));
});
