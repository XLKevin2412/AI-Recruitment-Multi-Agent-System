import assert from 'node:assert/strict';
import test from 'node:test';

import { formatEvidence, parseListField, readAnalysisList } from './analysis-report.js';

test('reads documented array fields and removes duplicate values', () => {
  const report = { matchedSkills: [' Java ', 'Spring Boot', 'Java'] };
  assert.deepEqual(readAnalysisList(report, 'matchedSkills'), ['Java', 'Spring Boot']);
});

test('falls back to persisted JSON string fields', () => {
  const report = { risksJson: '["Missing Redis", "Manual review"]' };
  assert.deepEqual(readAnalysisList(report, 'risks'), ['Missing Redis', 'Manual review']);
});

test('keeps tolerant support for legacy delimited text', () => {
  assert.deepEqual(parseListField('Java，Spring Boot; Redis\nDocker'), ['Java', 'Spring Boot', 'Redis', 'Docker']);
});

test('formats documented RAG evidence without rendering raw objects', () => {
  const evidence = formatEvidence({
    ragEvidence: [
      { evidenceId: 'ev-1', sourceType: 'RESUME', content: 'Used Spring Boot', score: 0.86 }
    ]
  });
  assert.deepEqual(evidence, ['RESUME：Used Spring Boot，相关度 0.86，ID ev-1']);
});

test('falls back to persisted evidence IDs', () => {
  assert.deepEqual(formatEvidence({ ragEvidenceIdsJson: '["ev-1", "ev-2"]' }), ['ev-1', 'ev-2']);
});
