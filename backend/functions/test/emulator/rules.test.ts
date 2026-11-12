import { afterAll, beforeAll, beforeEach, describe, it } from "vitest";
import { assertFails, assertSucceeds, initializeTestEnvironment } from "@firebase/rules-unit-testing";
import type { RulesTestEnvironment } from "@firebase/rules-unit-testing";
import { doc, getDoc, setDoc, updateDoc } from "firebase/firestore";
import { readFileSync } from "node:fs";

let env: RulesTestEnvironment;

beforeAll(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-pitchside-rules",
    firestore: { rules: readFileSync("../firestore.rules", "utf8") },
  });
});

afterAll(async () => { await env.cleanup(); });

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const fs = ctx.firestore();
    await setDoc(doc(fs, "fixtures/1001"), { id: "1001", status: "live" });
    await setDoc(doc(fs, "fixtures/1001/events/e1"), { kind: "goal" });
    await setDoc(doc(fs, "standings/39_2026"), { rows: [] });
    await setDoc(doc(fs, "apiUsage/20261029"), { used: 3 });
    await setDoc(doc(fs, "sentAlerts/x"), { createdAtMs: 1 });
    await setDoc(doc(fs, "users/bob"), { language: "English" });
  });
});

const free = (uid = "alice") => env.authenticatedContext(uid, { pro: false }).firestore();
const pro = (uid = "carol") => env.authenticatedContext(uid, { pro: true }).firestore();

describe("football data", () => {
  it("is unreadable when signed out", async () => {
    const fs = env.unauthenticatedContext().firestore();
    await assertFails(getDoc(doc(fs, "fixtures/1001")));
    await assertFails(getDoc(doc(fs, "standings/39_2026")));
  });

  it("is readable for any signed-in user, including sub-collections", async () => {
    await assertSucceeds(getDoc(doc(free(), "fixtures/1001")));
    await assertSucceeds(getDoc(doc(free(), "fixtures/1001/events/e1")));
    await assertSucceeds(getDoc(doc(free(), "standings/39_2026")));
  });

  it("can never be written by a client", async () => {
    await assertFails(setDoc(doc(pro(), "fixtures/1001"), { status: "finished" }));
    await assertFails(setDoc(doc(free(), "fixtures/2002"), { id: "2002" }));
    await assertFails(setDoc(doc(free(), "standings/39_2026"), { rows: [1] }));
    await assertFails(setDoc(doc(free(), "fixtures/1001/events/e2"), { kind: "goal" }));
  });

  it("keeps operational collections private", async () => {
    await assertFails(getDoc(doc(free(), "apiUsage/20261029")));
    await assertFails(getDoc(doc(free(), "sentAlerts/x")));
    await assertFails(setDoc(doc(free(), "apiUsage/20261029"), { used: 0 }));
  });
});

describe("user documents", () => {
  it("lets a user create and read their own document", async () => {
    const fs = free("alice");
    await assertSucceeds(setDoc(doc(fs, "users/alice"), { language: "English", follows: { teams: ["10", "20"], leagues: ["39"], players: [] }, alertLevel: "Key" }));
    await assertSucceeds(getDoc(doc(fs, "users/alice")));
  });

  it("blocks reading or writing someone else's document", async () => {
    await assertFails(getDoc(doc(free("alice"), "users/bob")));
    await assertFails(setDoc(doc(free("alice"), "users/bob"), { language: "Urdu" }));
  });

  it("rejects unknown fields, such as trying to grant yourself Pro", async () => {
    await assertFails(setDoc(doc(free("alice"), "users/alice"), { language: "English", pro: true }));
    await assertFails(setDoc(doc(free("alice"), "users/alice"), { isAdmin: true }));
  });

  it("caps the follow list: 5 teams free, more for Pro", async () => {
    const six = ["1", "2", "3", "4", "5", "6"];
    await assertFails(setDoc(doc(free("alice"), "users/alice"), { follows: { teams: six } }));
    await assertSucceeds(setDoc(doc(free("alice"), "users/alice"), { follows: { teams: six.slice(0, 5) } }));
    await assertSucceeds(setDoc(doc(pro("carol"), "users/carol"), { follows: { teams: six } }));
  });

  it("rejects an oversized language value and wrong types", async () => {
    await assertFails(setDoc(doc(free("alice"), "users/alice"), { language: "x".repeat(41) }));
    await assertFails(setDoc(doc(free("alice"), "users/alice"), { follows: { teams: "10" } }));
  });

  it("requires sign in", async () => {
    await assertFails(setDoc(doc(env.unauthenticatedContext().firestore(), "users/alice"), { language: "English" }));
  });
});

describe("AI usage counter", () => {
  it("starts at 1 and may only rise by 1", async () => {
    const fs = free("alice");
    const ref = doc(fs, "users/alice/aiUsage/20261029");
    await assertFails(setDoc(ref, { count: 3 }));
    await assertSucceeds(setDoc(ref, { count: 1 }));
    await assertSucceeds(updateDoc(ref, { count: 2 }));
    await assertFails(updateDoc(ref, { count: 5 }));
    await assertFails(updateDoc(ref, { count: 1 }));
  });

  it("stops at 5 for free users but allows up to 50 for Pro", async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "users/alice/aiUsage/20261029"), { count: 5 });
      await setDoc(doc(ctx.firestore(), "users/carol/aiUsage/20261029"), { count: 5 });
    });
    await assertFails(updateDoc(doc(free("alice"), "users/alice/aiUsage/20261029"), { count: 6 }));
    await assertSucceeds(updateDoc(doc(pro("carol"), "users/carol/aiUsage/20261029"), { count: 6 }));
  });

  it("is private to its owner", async () => {
    await assertFails(getDoc(doc(free("alice"), "users/bob/aiUsage/20261029")));
  });
});
