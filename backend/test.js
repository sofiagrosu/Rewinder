const assert = require("assert");
const { normalizeAll } = require("./validate");
const all = ["TRUE_FALSE", "TWO_OPTIONS", "MISSING_WORD"];

const good = [
  { type: "TRUE_FALSE", text: "Nervul median trece prin tunelul carpian.", options: ["x", "y"], correctIndex: 0, explanation: "e" },
  { type: "TWO_OPTIONS", text: "Bicepsul e inervat de nervul ____.", options: ["musculocutanat", "radial"], correctIndex: 0, explanation: "" },
  { type: "MISSING_WORD", text: "Valva ____ separă atriul stâng de ventriculul stâng.", options: ["mitrală", "tricuspidă", "aortică"], correctIndex: 0, explanation: "" },
];
let r = normalizeAll(good, all, 10);
assert.strictEqual(r.questions.length, 3);
assert.deepStrictEqual(r.questions[0].options, ["True", "False"]);   // forced

const bad = [
  { type: "MISSING_WORD", text: "No gap here", options: ["a", "b", "c"], correctIndex: 0 },       // no gap
  { type: "TWO_OPTIONS", text: "Gap ____", options: ["a", "a"], correctIndex: 0 },               // duplicate options
  { type: "TWO_OPTIONS", text: "Gap ____", options: ["a", "b"], correctIndex: 2 },               // index out of range
  { type: "MISSING_WORD", text: "Gap ____", options: ["a", "b"], correctIndex: 0 },              // wrong count
  { type: "ESSAY", text: "x", options: [], correctIndex: 0 },                                    // unknown type
  good[0], good[0],                                                                              // duplicate
];
r = normalizeAll(bad, all, 10);
assert.strictEqual(r.questions.length, 1);
assert.strictEqual(r.dropped, 6);

assert.strictEqual(normalizeAll(good, ["TRUE_FALSE"], 10).questions.length, 1);   // only requested types
assert.strictEqual(normalizeAll(good, all, 2).questions.length, 2);               // count cap
assert.strictEqual(normalizeAll("nonsense", all, 5).questions.length, 0);
console.log("validate.js: all tests passed");
