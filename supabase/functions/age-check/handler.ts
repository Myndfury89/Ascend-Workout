// Request handler for the `age-check` Edge Function, with its I/O injected so it can be tested
// without a Supabase project. index.ts wires the real service-role client.

import {
  ConfigError,
  InputError,
  meetsThreshold,
  type Method,
  parseAttestation,
  parseConfig,
} from "./logic.ts";

export interface RecordResult {
  status: string;
  social_eligible: boolean;
}

export interface Deps {
  /** Validate the caller's JWT and return auth.uid(), or null if invalid. */
  verifyUser(jwt: string): Promise<string | null>;
  /** Read the singleton config row (service role). */
  getConfig(): Promise<unknown>;
  /**
   * Record the minimal outcome (service role). Receives ONLY the user id, the boolean, the
   * threshold used, and the method name. Never the DOB, an age, or a band.
   */
  record(userId: string, meets: boolean, threshold: number, method: Method): Promise<RecordResult>;
  now(): Date;
  log(event: Record<string, unknown>): void;
}

function json(status: number, body: Record<string, unknown>): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json", "cache-control": "no-store" },
  });
}

// Fail-safe response: never claims eligibility.
const failClosed = (status: number, error: string) =>
  json(status, { status: "error", error, social_eligible: false });

export async function handle(req: Request, deps: Deps): Promise<Response> {
  if (req.method !== "POST") return failClosed(405, "method_not_allowed");

  const auth = req.headers.get("authorization") ?? "";
  const m = /^Bearer\s+(\S+)$/i.exec(auth);
  if (!m) return failClosed(401, "unauthorized");

  let userId: string | null;
  try {
    userId = await deps.verifyUser(m[1]);
  } catch {
    userId = null;
  }
  if (!userId) return failClosed(401, "unauthorized");

  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return failClosed(400, "invalid_json");
  }

  try {
    const now = deps.now();
    const attestation = parseAttestation(body, now);
    // Drop the reference to the raw body as early as possible; nothing below may see it.
    body = undefined;

    const config = parseConfig(await deps.getConfig());
    if (config.mode === "off") {
      deps.log({ event: "age_check", outcome: "disabled" });
      return json(200, { status: "disabled", social_eligible: false });
    }

    const meets = meetsThreshold(attestation, config.minAge, now);
    const result = await deps.record(userId, meets, config.minAge, attestation.method);
    const eligible = config.mode === "enforce" && result.social_eligible === true && meets;
    deps.log({ event: "age_check", outcome: result.status, mode: config.mode });
    return json(200, { status: result.status, social_eligible: eligible });
  } catch (e) {
    if (e instanceof InputError) return failClosed(400, "invalid_input");
    if (e instanceof ConfigError) {
      deps.log({ event: "age_check", outcome: "config_error" });
      return failClosed(503, "unavailable");
    }
    deps.log({ event: "age_check", outcome: "error" });
    return failClosed(503, "unavailable");
  }
}
