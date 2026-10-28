# Security Specification (`security_spec.md`)

## 1. Data Invariants
1. **Strict Multi-Tenant Ownership**: Every document in `/users/{userId}`, `/voices/{voiceId}`, `/generations/{generationId}`, and `/abuse_reports/{reportId}` MUST belong to the authenticated caller (`request.auth.uid == userId` or `resource.data.userId == request.auth.uid`).
2. **Explicit Voice Consent Enforcement**: No `VoiceProfile` document can be created or updated unless `consentVerified == true` and `consentStatement` is a valid non-empty string (10..500 chars).
3. **AI Disclosure Labeling**: Every `VoiceGeneration` document MUST have `aiDisclosureLabeled == true`.
4. **Immutable Identity & Creation Timestamps**: `userId` and `createdAt` can never be mutated on `update`.
5. **Bounded Strings & Numbers**: Every string field enforces strict `.size()` bounds; numeric metrics (`creditsRemaining`, `sampleDurationSec`, `stability`, `speed`) enforce domain bounds.

## 2. Dirty Dozen Adversarial Payloads
1. Unauthenticated read on `/voices` -> Reject (`PERMISSION_DENIED`)
2. Cross-user read on `/voices/{bobVoiceId}` by Alice -> Reject (`PERMISSION_DENIED`)
3. Unfiltered list query on `/voices` without `.where("userId", "==", uid)` -> Reject (`PERMISSION_DENIED`)
4. Creating a voice clone with `consentVerified: false` -> Reject (`PERMISSION_DENIED`)
5. Creating a voice clone with `sampleDurationSec: 3` (< 15s minimum) -> Reject (`PERMISSION_DENIED`)
6. Shadow update injecting ghost field `isAdmin: true` into `/users/{uid}` -> Reject (`PERMISSION_DENIED`)
7. Mutating `createdAt` or `userId` during `/voices/{voiceId}` update -> Reject (`PERMISSION_DENIED`)
8. Creating `/generations/{id}` with `aiDisclosureLabeled: false` -> Reject (`PERMISSION_DENIED`)
9. Setting negative `creditsRemaining: -50` on `/users/{uid}` -> Reject (`PERMISSION_DENIED`)
10. Setting invalid `planTier: "unlimited_hack"` on `/users/{uid}` -> Reject (`PERMISSION_DENIED`)
11. Deleting another user's `/generations/{id}` -> Reject (`PERMISSION_DENIED`)
12. Mutating an `/abuse_reports/{reportId}` document after submission -> Reject (`PERMISSION_DENIED`)
