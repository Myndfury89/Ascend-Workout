import { assert, assertEquals, assertThrows } from "jsr:@std/assert@1";
import {
  ageInYears,
  ConfigError,
  InputError,
  meetsThreshold,
  parseAttestation,
  parseConfig,
  referenceDate,
} from "./logic.ts";

// 2026-06-15 18:00 UTC -> reference date (UTC-12) is 2026-06-15.
const NOW = new Date("2026-06-15T18:00:00Z");

Deno.test("reference date is the UTC-12 calendar date (never early)", () => {
  assertEquals(referenceDate(new Date("2026-06-15T11:59:59Z")), [2026, 6, 14]);
  assertEquals(referenceDate(new Date("2026-06-15T12:00:00Z")), [2026, 6, 15]);
});

Deno.test("threshold boundary: birthday today meets, tomorrow does not", () => {
  const dob = (s: string) => parseAttestation({ dob: s }, NOW);
  assert(meetsThreshold(dob("2008-06-15"), 18, NOW)); // 18 today
  assert(!meetsThreshold(dob("2008-06-16"), 18, NOW)); // 18 tomorrow
  assert(meetsThreshold(dob("2008-06-14"), 18, NOW));
  assert(meetsThreshold(dob("2013-06-15"), 13, NOW));
  assert(!meetsThreshold(dob("2013-06-16"), 13, NOW));
});

Deno.test("birthday is not counted early near the date line", () => {
  // 2026-06-15 05:00 UTC: already the 15th in UTC and UTC+14, still the 14th in UTC-12.
  const early = new Date("2026-06-15T05:00:00Z");
  assert(!meetsThreshold(parseAttestation({ dob: "2008-06-15" }, early), 18, early));
});

Deno.test("Feb 29 birthday is reached on Mar 1 in non-leap years", () => {
  assertEquals(ageInYears(2008, 2, 29, new Date("2026-02-28T18:00:00Z")), 17);
  assertEquals(ageInYears(2008, 2, 29, new Date("2026-03-01T18:00:00Z")), 18);
});

Deno.test("age bands: meets only when the whole band is at/above threshold", () => {
  const band = (b: string) => parseAttestation({ age_band: b }, NOW);
  assert(meetsThreshold(band("18_plus"), 18, NOW));
  assert(!meetsThreshold(band("16_17"), 18, NOW));
  assert(meetsThreshold(band("16_17"), 16, NOW));
  assert(!meetsThreshold(band("13_15"), 14, NOW)); // straddles -> fail closed
  assert(meetsThreshold(band("13_15"), 13, NOW));
  assert(!meetsThreshold(band("under_13"), 13, NOW));
  assert(!meetsThreshold(band("18_plus"), 21, NOW));
});

Deno.test("out-of-range threshold never meets", () => {
  const a = parseAttestation({ age_band: "18_plus" }, NOW);
  assert(!meetsThreshold(a, 12, NOW));
  assert(!meetsThreshold(a, 26, NOW));
  assert(!meetsThreshold(a, 17.5, NOW));
});

Deno.test("invalid attestations are rejected", () => {
  const bad: unknown[] = [
    null,
    [],
    "2000-01-01",
    {},
    { dob: "2000-01-01", age_band: "18_plus" },
    { dob: "2000-02-30" },
    { dob: "2000-13-01" },
    { dob: "01/01/2000" },
    { dob: "2000-1-1" },
    { dob: 20000101 },
    { dob: "2026-06-16" }, // future
    { dob: "1900-01-01" }, // implausible
    { age_band: "adult" },
    { age_band: 18 },
  ];
  for (const b of bad) assertThrows(() => parseAttestation(b, NOW), InputError);
});

Deno.test("config: valid rows parse (object or single-row array)", () => {
  assertEquals(
    parseConfig([{ enforcement_mode: "off", min_age: 18, allow_reattest: false }]),
    { mode: "off", minAge: 18, allowReattest: false },
  );
  assertEquals(
    parseConfig({ enforcement_mode: "enforce", min_age: 13, allow_reattest: true }).mode,
    "enforce",
  );
});

Deno.test("config: missing or malformed config throws (fail closed)", () => {
  const bad: unknown[] = [
    null,
    [],
    undefined,
    { enforcement_mode: "ENFORCE", min_age: 18, allow_reattest: false },
    { enforcement_mode: "on", min_age: 18, allow_reattest: false },
    { enforcement_mode: "enforce", min_age: 12, allow_reattest: false },
    { enforcement_mode: "enforce", min_age: "18", allow_reattest: false },
    { enforcement_mode: "enforce", min_age: 18 },
  ];
  for (const b of bad) assertThrows(() => parseConfig(b), ConfigError);
});
