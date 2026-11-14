// Validates and normalises the questions returned by the AI.
// The app trusts this output (it still goes through the Review screen, but a broken
// question must never even reach it). Pure functions: easy to test without the API.

const TYPES = ["TRUE_FALSE", "TWO_OPTIONS", "MISSING_WORD"];
const TRUE_TEXT = "True";
const FALSE_TEXT = "False";
const GAP = "____";
const MAX_TEXT = 300;       // a question must fit in a notification
const MAX_OPTION = 40;      // an option must fit on a watch button
const MAX_EXPLANATION = 300;

function clean(s) {
  return typeof s === "string" ? s.replace(/\s+/g, " ").trim() : "";
}

/** Returns a valid question {type,text,options,correctIndex,explanation} or null. */
function normalizeQuestion(raw, allowedTypes) {
  if (!raw || typeof raw !== "object") return null;
  const type = clean(raw.type).toUpperCase();
  if (!TYPES.includes(type) || !allowedTypes.includes(type)) return null;

  let text = clean(raw.text);
  if (!text || text.length > MAX_TEXT) return null;

  let options;
  if (type === "TRUE_FALSE") {
    options = [TRUE_TEXT, FALSE_TEXT];            // fixed texts, the app relies on them
  } else {
    if (!Array.isArray(raw.options)) return null;
    options = raw.options.map(clean);
    const wanted = type === "TWO_OPTIONS" ? 2 : 3;
    if (options.length !== wanted) return null;
    if (options.some((o) => !o || o.length > MAX_OPTION)) return null;
    if (new Set(options.map((o) => o.toLowerCase())).size !== options.length) return null;
    if (!text.includes(GAP)) {                    // fill-in questions need the gap
      text = text.replace(/_{2,}|\.{3,}|…/, GAP);
      if (!text.includes(GAP)) return null;
    }
  }

  const idx = Number(raw.correctIndex);
  if (!Number.isInteger(idx) || idx < 0 || idx >= options.length) return null;

  return {
    type,
    text,
    options,
    correctIndex: idx,
    explanation: clean(raw.explanation).slice(0, MAX_EXPLANATION),
  };
}

/** Filters, de-duplicates and trims to `count`. Returns {questions, dropped}. */
function normalizeAll(rawList, allowedTypes, count) {
  const out = [];
  const seen = new Set();
  let dropped = 0;
  for (const raw of Array.isArray(rawList) ? rawList : []) {
    const q = normalizeQuestion(raw, allowedTypes);
    const key = q && q.text.toLowerCase();
    if (!q || seen.has(key)) { dropped++; continue; }
    seen.add(key);
    out.push(q);
    if (out.length >= count) break;
  }
  return { questions: out, dropped };
}

module.exports = { normalizeQuestion, normalizeAll, TYPES };
