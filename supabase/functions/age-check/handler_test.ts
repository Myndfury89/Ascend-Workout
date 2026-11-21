import { assert, assertEquals } from "jsr:@std/assert@1";
import { type Deps, handle, type RecordResult } from "./handler.ts";
import type { Method } from "./logic.ts";

const NOW = new Date("2026-06-15T18:00:00Z");
const USER = "11111111-1111-1111-1111-111111111111";
const ADULT_DOB = "1990-04-02";
const MINOR_DOB = "2012-09-30";

type Call = { userId: string; meets: boolean; threshold: number; method: Method };

function fakeDeps(opts: {
  config?: unknown;
  configThrows?: boolean;
  recordThrows?: boolean;
  recordResult?: (c: Call, mode: string) => RecordResult;
  user?: string | null;
} = {}) {
  const calls: Call[] = [];
  const logs: Record<string, unknown>[] = [];
  const config = opts.config ?? { enforcement_mode: "off", min_age: 18, allow_reattest: false };
  const deps: Deps = {
    verifyUser: (jwt) => Promise.resolve(jwt === "good" ? (opts.user ?? USER) : null),
    getConfig: () =>
      opts.configThrows ? Promise.reject(new Error("db down")) : Promise.resolve(config),
    record: (userId, meets, threshold, method) => {
      const c = { userId, meets, threshold, method };
      calls.push(c);
      if (opts.recordThrows) return Promise.reject(new Error("db down"));
      const mode = (config as { enforcement_mode: string }).enforcement_mode;
      return Promise.resolve(
        opts.recordResult?.(c, mode) ??
          (mode === "enforce"
            ? { status: "applied", social_eligible: meets }
            : { status: "recorded", social_eligible: false }),
      );
    },
    now: () => NOW,
    log: (e) => logs.push(e),
  };
  return { deps, calls, logs };
}

function req(body: unknown, token: string | null = "good", method = "POST") {
  const headers: Record<string, string> = { "content-type": "application/json" };
  if (token) headers.authorization = `Bearer ${token}`;
  return new Request("http://localhost/age-check", {
    method,
    headers,
    body: method === "GET" ? undefined : typeof body === "string" ? body : JSON.stringify(body),
  });
}

const cfg = (mode: string, minAge = 18) => ({
  enforcement_mode: mode,
  min_age: minAge,
  allow_reattest: false,
});

Deno.test("rejects non-POST and missing/invalid JWT without touching config or DB", async () => {
  const f = fakeDeps({ config: cfg("enforce") });
  assertEquals((await handle(req(null, "good", "GET"), f.deps)).status, 405);
  assertEquals((await handle(req({ dob: ADULT_DOB }, null), f.deps)).status, 401);
  assertEquals((await handle(req({ dob: ADULT_DOB }, "forged"), f.deps)).status, 401);
  assertEquals(f.calls.length, 0);
});

Deno.test("acts only on the JWT user, ignoring any user id in the body", async () => {
  const f = fakeDeps({ config: cfg("enforce") });
  const other = "22222222-2222-2222-2222-222222222222";
  // A user_id in the body is ignored; only the verified JWT's user is ever passed to record().
  const r1 = await handle(req({ dob: ADULT_DOB, user_id: other }), f.deps);
  assertEquals(r1.status, 200);
  assertEquals(f.calls.length, 1);
  assertEquals(f.calls[0].userId, USER);
});

Deno.test("mode off: no DB write, never eligible", async () => {
  const f = fakeDeps({ config: cfg("off") });
  const res = await handle(req({ dob: ADULT_DOB }), f.deps);
  assertEquals(res.status, 200);
  assertEquals(await res.json(), { status: "disabled", social_eligible: false });
  assertEquals(f.calls.length, 0);
});

Deno.test("mode log_only: records the boolean, never reports eligible", async () => {
  const f = fakeDeps({ config: cfg("log_only") });
  const res = await handle(req({ dob: ADULT_DOB }), f.deps);
  assertEquals(await res.json(), { status: "recorded", social_eligible: false });
  assertEquals(f.calls, [{ userId: USER, meets: true, threshold: 18, method: "dob" }]);
});

Deno.test("mode log_only: even a buggy record result cannot report eligible", async () => {
  const f = fakeDeps({
    config: cfg("log_only"),
    recordResult: () => ({ status: "recorded", social_eligible: true }),
  });
  const body = await (await handle(req({ dob: ADULT_DOB }), f.deps)).json();
  assertEquals(body.social_eligible, false);
});

Deno.test("mode enforce: adult eligible, minor not", async () => {
  const f = fakeDeps({ config: cfg("enforce") });
  assertEquals(await (await handle(req({ dob: ADULT_DOB }), f.deps)).json(), {
    status: "applied",
    social_eligible: true,
  });
  assertEquals(await (await handle(req({ dob: MINOR_DOB }), f.deps)).json(), {
    status: "applied",
    social_eligible: false,
  });
  assertEquals(f.calls.map((c) => c.meets), [true, false]);
});

Deno.test("mode enforce: age band, and threshold comes from config", async () => {
  const f = fakeDeps({ config: cfg("enforce", 16) });
  const body = await (await handle(req({ age_band: "16_17" }), f.deps)).json();
  assertEquals(body.social_eligible, true);
  assertEquals(f.calls[0], { userId: USER, meets: true, threshold: 16, method: "age_band" });
});

Deno.test("mode enforce: DB says locked/ineligible -> not eligible even if input meets", async () => {
  const f = fakeDeps({
    config: cfg("enforce"),
    recordResult: () => ({ status: "locked", social_eligible: false }),
  });
  const body = await (await handle(req({ dob: ADULT_DOB }), f.deps)).json();
  assertEquals(body, { status: "locked", social_eligible: false });
});

Deno.test("fail-safe: config read error -> 503, no write, not eligible", async () => {
  const f = fakeDeps({ configThrows: true });
  const res = await handle(req({ dob: ADULT_DOB }), f.deps);
  assertEquals(res.status, 503);
  assertEquals((await res.json()).social_eligible, false);
  assertEquals(f.calls.length, 0);
});

Deno.test("fail-safe: missing or malformed config -> 503, no write", async () => {
  for (const c of [[], null, { enforcement_mode: "yes", min_age: 18, allow_reattest: false }]) {
    const f = fakeDeps({ config: cfg("enforce") });
    f.deps.getConfig = () => Promise.resolve(c);
    const res = await handle(req({ dob: ADULT_DOB }), f.deps);
    assertEquals(res.status, 503);
    assertEquals((await res.json()).social_eligible, false);
    assertEquals(f.calls.length, 0);
  }
});

Deno.test("fail-safe: record error -> 503, not eligible", async () => {
  const f = fakeDeps({ config: cfg("enforce"), recordThrows: true });
  const res = await handle(req({ dob: ADULT_DOB }), f.deps);
  assertEquals(res.status, 503);
  assertEquals((await res.json()).social_eligible, false);
});

Deno.test("fail-safe: verifyUser throwing -> 401", async () => {
  const f = fakeDeps({ config: cfg("enforce") });
  f.deps.verifyUser = () => Promise.reject(new Error("auth down"));
  assertEquals((await handle(req({ dob: ADULT_DOB }), f.deps)).status, 401);
  assertEquals(f.calls.length, 0);
});

Deno.test("invalid input -> 400, nothing recorded", async () => {
  const f = fakeDeps({ config: cfg("enforce") });
  assertEquals((await handle(req("not json"), f.deps)).status, 400);
  assertEquals((await handle(req({ dob: "2000-02-30" }), f.deps)).status, 400);
  assertEquals((await handle(req({ age_band: "adult" }), f.deps)).status, 400);
  assertEquals(f.calls.length, 0);
});

Deno.test("data minimization: DOB/band never reach record(), logs, or the response", async () => {
  for (const mode of ["off", "log_only", "enforce"]) {
    for (const body of [{ dob: ADULT_DOB }, { dob: MINOR_DOB }, { age_band: "16_17" }]) {
      const f = fakeDeps({ config: cfg(mode) });
      const res = await handle(req(body), f.deps);
      const out = await res.text();
      const raw = Object.values(body)[0];
      const everything = JSON.stringify({ calls: f.calls, logs: f.logs, out });
      assert(!everything.includes(raw), `${mode}: leaked ${raw}`);
      assert(!everything.includes("1990") && !everything.includes("2012"), `${mode}: leaked year`);
      for (const c of f.calls) {
        assertEquals(Object.keys(c).sort(), ["meets", "method", "threshold", "userId"]);
      }
      // Logs carry no user id either.
      assert(!JSON.stringify(f.logs).includes(USER));
    }
  }
});
