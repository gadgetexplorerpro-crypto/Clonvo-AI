const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("1. Unauthenticated user cannot read voices", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("voices").get());
});

test("2. Authenticated user cannot read another user's voice profile", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("voices").doc("bob_voice_1").set({
      userId: BOB_UID,
      name: "Bob Studio Voice",
      elevenLabsVoiceId: "el_voice_bob123",
      description: "Warm narrator voice",
      consentStatement: "I am Bob and I authorize cloning my voice in Clonvo AI.",
      consentVerified: true,
      sampleDurationSec: 32.5,
      clarityScore: 91.0,
      noiseDb: -48.0,
      fundamentalFreqHz: 125.0,
      status: "ready",
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("voices").doc("bob_voice_1").get());
});

test("3. Authenticated user can create and query their own voice profile with verified consent", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("voices").doc("alice_voice_1").set({
      userId: ALICE_UID,
      name: "Alice Pro Clone",
      elevenLabsVoiceId: "el_voice_alice99",
      description: "Clear broadcast tone",
      consentStatement: "I am Alice and I explicitly consent to cloning my voice.",
      consentVerified: true,
      sampleDurationSec: 28.0,
      clarityScore: 94.5,
      noiseDb: -52.0,
      fundamentalFreqHz: 210.0,
      status: "ready",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("voices").where("userId", "==", ALICE_UID).get()
  );
});

test("4. Creating voice profile without verified consent is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("voices").doc("alice_voice_no_consent").set({
      userId: ALICE_UID,
      name: "Unauthorized Voice",
      elevenLabsVoiceId: "el_voice_unauth",
      description: "Missing consent",
      consentStatement: "I am Alice and I explicitly consent to cloning my voice.",
      consentVerified: false,
      sampleDurationSec: 25.0,
      clarityScore: 88.0,
      noiseDb: -45.0,
      fundamentalFreqHz: 195.0,
      status: "ready",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("5. Unfiltered list query on voices fails (rules are not filters)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("voices").get());
});

test("6. Creating generation without aiDisclosureLabeled == true is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("generations").doc("gen_1").set({
      userId: ALICE_UID,
      voiceId: "alice_voice_1",
      voiceName: "Alice Pro Clone",
      mode: "tts",
      inputText: "Hello from Clonvo AI studio.",
      modelId: "eleven_multilingual_v2",
      languageCode: "en",
      stability: 0.65,
      similarityBoost: 0.85,
      styleExaggeration: 0.2,
      speed: 1.0,
      localAudioPath: "/data/user/0/com.aistudio.clonvoai.vxcqmr/files/gen_1.mp3",
      measuredF0Hz: 208.0,
      measuredSimilarityScore: 93.4,
      creditsCharged: 28,
      isFavorite: false,
      aiDisclosureLabeled: false,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("7. Shadow update with ghost field on user profile is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Producer",
      email: "alice@example.com",
      planTier: "free_trial",
      creditsRemaining: 2500,
      creditsUsed: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      creditsRemaining: 5000,
      isAdmin: true,
      updatedAt: now,
    })
  );
});
