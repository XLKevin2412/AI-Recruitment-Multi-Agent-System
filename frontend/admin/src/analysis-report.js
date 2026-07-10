function uniqueStrings(values) {
  return [...new Set(values.map((value) => String(value).trim()).filter(Boolean))];
}

export function parseListField(value) {
  if (value === null || value === undefined || value === '') return [];
  if (Array.isArray(value)) return uniqueStrings(value);
  if (typeof value !== 'string') return uniqueStrings([value]);

  const trimmed = value.trim();
  if (!trimmed) return [];

  try {
    const parsed = JSON.parse(trimmed);
    if (Array.isArray(parsed)) return uniqueStrings(parsed);
    if (typeof parsed === 'string') return uniqueStrings([parsed]);
  } catch {
    // Older or manually edited records may contain a delimited text value.
  }

  return uniqueStrings(trimmed.split(/[\n,，;；]+/));
}

export function readAnalysisList(report, fieldName) {
  if (!report || typeof report !== 'object') return [];
  return parseListField(report[fieldName] ?? report[`${fieldName}Json`]);
}

export function formatEvidence(report) {
  if (!report || typeof report !== 'object') return [];
  if (!Array.isArray(report.ragEvidence)) {
    return parseListField(report.ragEvidenceIdsJson);
  }

  return uniqueStrings(
    report.ragEvidence.map((evidence) => {
      if (!evidence || typeof evidence !== 'object') return evidence;
      const source = String(evidence.sourceType || 'EVIDENCE').trim();
      const content = String(evidence.content || '').trim();
      const id = String(evidence.evidenceId || '').trim();
      const score = Number.isFinite(evidence.score) ? `，相关度 ${evidence.score}` : '';
      if (content) return `${source}：${content}${score}${id ? `，ID ${id}` : ''}`;
      return id ? `${source}：${id}` : '';
    })
  );
}
