// Pure age-check logic for the `age-check` Edge Function. No I/O, no Supabase imports, so it can be
// unit-tested with `deno test` and reasoned about in isolation.
//
// Data minimization: the raw DOB / band is parsed and reduced to ONE boolean (meets threshold) here.
// Nothing in this module returns or logs the DOB, an exact age, or a band.

export type EnforcementMode = "off" | "log_only" | "enforce";
export type Method = "dob" | "age_band";

export const AGE_BANDS = ["under_13", "13_15", "16_17", "18_plus"] as const;
export type AgeBand = typeof AGE_BANDS[number];

/** Inclusive lower bound of each band (the youngest age a person in that band can be). */
const BAND_MIN_AGE: Record<AgeBand, number> = {
  under_13: 0,
  "13_15": 13,
  "16_17": 16,
  "18_plus": 18,
};

export const MIN_THRESHOLD = 13;
export const MAX_THRESHOLD = 25;
const MAX_PLAUSIBLE_AGE = 120;

export interface AgeCheckConfig {
  mode: EnforcementMode;
  minAge: number;
  allowReattest: boolean;
}

export type Attestation =
  | { method: "dob"; year: number; month: number; day: number }
  | { method: "age_band"; band: AgeBand };

export class InputError extends Error {}
export class ConfigError extends Error {}

/**
 * Validate the config row. Anything unexpected throws ConfigError so the caller fails closed
 * (no grant). There is intentionally no "default to enforce" path.
 */
export function parseConfig(raw: unknown): AgeCheckConfig {
  const row = Array.isArray(raw) ? raw[0] : raw;
  if (!row || typeof row !== "object") throw new ConfigError("config missing");
  const r = row as Record<string, unknown>;
  const mode = r.enforcement_mode;
  const minAge = r.min_age;
  const allow = r.allow_reattest;
  if (mode !== "off" && mode !== "log_only" && mode !== "enforce") {
    throw new ConfigError("invalid enforcement_mode");
  }
  if (
    typeof minAge !== "number" || !Number.isInteger(minAge) ||
    minAge < MIN_THRESHOLD || minAge > MAX_THRESHOLD
  ) {
    throw new ConfigError("invalid min_age");
  }
  if (typeof allow !== "boolean") throw new ConfigError("invalid allow_reattest");
  return { mode, minAge, allowReattest: allow };
}

/** Parse the request body. Exactly one of `dob` (YYYY-MM-DD) or `age_band` is accepted. */
export function parseAttestation(body: unknown, now: Date): Attestation {
  if (!body || typeof body !== "object" || Array.isArray(body)) {
    throw new InputError("body must be a JSON object");
  }
  const b = body as Record<string, unknown>;
  const hasDob = b.dob !== undefined;
  const hasBand = b.age_band !== undefined;
  if (hasDob === hasBand) throw new InputError("provide exactly one of dob or age_band");

  if (hasBand) {
    if (typeof b.age_band !== "string" || !(AGE_BANDS as readonly string[]).includes(b.age_band)) {
      throw new InputError("invalid age_band");
    }
    return { method: "age_band", band: b.age_band as AgeBand };
  }

  if (typeof b.dob !== "string") throw new InputError("invalid dob");
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(b.dob);
  if (!m) throw new InputError("invalid dob");
  const year = Number(m[1]);
  const month = Number(m[2]);
  const day = Number(m[3]);
  const d = new Date(Date.UTC(year, month - 1, day));
  if (d.getUTCFullYear() !== year || d.getUTCMonth() !== month - 1 || d.getUTCDate() !== day) {
    throw new InputError("invalid dob");
  }
  const ref = referenceDate(now);
  if (compareYmd([year, month, day], ref) > 0) throw new InputError("invalid dob");
  if (ref[0] - year > MAX_PLAUSIBLE_AGE) throw new InputError("invalid dob");
  return { method: "dob", year, month, day };
}

/**
 * The calendar date used as "today". We use the date in UTC-12 (the last time zone on Earth to
 * reach a given date), so a birthday is never counted early for anyone. Conservative by design.
 */
export function referenceDate(now: Date): [number, number, number] {
  const shifted = new Date(now.getTime() - 12 * 60 * 60 * 1000);
  return [shifted.getUTCFullYear(), shifted.getUTCMonth() + 1, shifted.getUTCDate()];
}

function compareYmd(a: [number, number, number], b: [number, number, number]): number {
  for (let i = 0; i < 3; i++) if (a[i] !== b[i]) return a[i] - b[i];
  return 0;
}

/** Completed years. A Feb 29 birthday is reached on Mar 1 in non-leap years (conservative). */
export function ageInYears(year: number, month: number, day: number, now: Date): number {
  const [ry, rm, rd] = referenceDate(now);
  let age = ry - year;
  if (rm < month || (rm === month && rd < day)) age -= 1;
  return age;
}

/**
 * Reduce the attestation to a single boolean. For bands, the person meets the threshold only if
 * EVERY age in the band does (band minimum >= threshold); a band straddling the threshold fails.
 */
export function meetsThreshold(att: Attestation, threshold: number, now: Date): boolean {
  if (!Number.isInteger(threshold) || threshold < MIN_THRESHOLD || threshold > MAX_THRESHOLD) {
    return false;
  }
  if (att.method === "age_band") return BAND_MIN_AGE[att.band] >= threshold;
  return ageInYears(att.year, att.month, att.day, now) >= threshold;
}
