// Supabase Edge Function: age-check (A2). INACTIVE BY DEFAULT: the DB config row ships with
// enforcement_mode = 'off', so this function records nothing and grants nothing until that row is
// changed in a separately approved production step.
//
// Request:  POST, Authorization: Bearer <user access token>, body {"dob":"YYYY-MM-DD"} or
//           {"age_band":"under_13"|"13_15"|"16_17"|"18_plus"}.
// Response: {"status": "...", "social_eligible": boolean}
//
// Uses the auto-provided SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY. No new secrets.
// Acts only on the user id from the verified JWT (never a user id from the body).

import { createClient } from "jsr:@supabase/supabase-js@2";
import { handle, type RecordResult } from "./handler.ts";

const url = Deno.env.get("SUPABASE_URL");
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

Deno.serve((req) => {
  if (!url || !serviceKey) {
    // Fail closed without revealing which variable is missing.
    return new Response(
      JSON.stringify({ status: "error", error: "unavailable", social_eligible: false }),
      { status: 503, headers: { "content-type": "application/json" } },
    );
  }
  const admin = createClient(url, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  return handle(req, {
    async verifyUser(jwt) {
      const { data, error } = await admin.auth.getUser(jwt);
      if (error || !data?.user?.id) return null;
      return data.user.id;
    },
    async getConfig() {
      const { data, error } = await admin.rpc("age_check_get_config");
      if (error) throw new Error("config read failed");
      return data;
    },
    async record(userId, meets, threshold, method) {
      const { data, error } = await admin.rpc("age_check_record", {
        p_user_id: userId,
        p_meets_threshold: meets,
        p_threshold: threshold,
        p_method: method,
      });
      if (error) throw new Error("record failed");
      const row = (Array.isArray(data) ? data[0] : data) as RecordResult | undefined;
      if (!row || typeof row.status !== "string") throw new Error("record failed");
      return { status: row.status, social_eligible: row.social_eligible === true };
    },
    now: () => new Date(),
    // Structured, PII-free log line: no user id, no DOB, no band.
    log: (event) => console.log(JSON.stringify(event)),
  });
});
