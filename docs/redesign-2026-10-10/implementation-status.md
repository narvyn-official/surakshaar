# Spatial Studio release work

Decision: Spatial Studio, light appearance, for Android and web. Confirmed by the user on 11 October 2026. The agreed SurakshaAR problem statement is the product baseline; no external problem statement is required.

This release work preserves the existing uncommitted learning, institutional, account and certificate implementation. It does not treat prior preview tests as evidence for the new release.

## Delivered implementation

- Apply the selected identity and canvas/inspector composition to native and web learning.
- Add browser learning, inspectable models, saved practice, accessible controls and recovery.
- Add disclosed institutional assessment identity evidence and examiner review, with separate personal learning.
- Package standard Next.js hosting, private storage, migrations, configuration checks, backup/restore and signed Android distribution.
- Verify the actual candidate through automated and rendered checks; record remaining external release gates.

## Decisions

- Keep the existing SQLite transaction layer for the initial single-instance deployment, with tenant checks and negative tests. PostgreSQL migration is a separate scale change, not a prerequisite for this bounded release. Do not claim multi-instance database support.
- Reuse current reviewed-status content and preserve its draft designation. Software completion cannot manufacture clinical, language or practical-training approval.
- Manual identity evidence is the first verification path. Optional automated face matching remains disabled until a provider, legal basis and measured thresholds are established.
- Prepare deployment artifacts locally. Public deployment, external messaging and payment/provider activation are not performed by the packaging request.

## Baseline evidence

- Web: 117 tests passed before changes.
- Android: baseline unit tests and debug assembly passed (Gradle reused unchanged outputs).
- Existing prototype: 5 web/mobile concepts rendered; user selected Spatial Studio light.

Candidate evidence, concrete release scope and external gates are now recorded in `docs/surakshaar-0.15.0/VERIFICATION.md` and `DEPLOYMENT.md`.

The initial implementation is a signed Android candidate and a locally verified Next.js standalone/SQLite package. Broader architecture entries remain proposals where explicitly identified in the deployment guide: automated face matching, Google/OTP/enterprise sign-in, payment automation, additional specialist curriculum and multi-node PostgreSQL operation are not silently represented as shipped. The selected release serves all audience contexts through common learning, with separate school-awareness/adult pathways and institution membership.

Identity capture is an online browser assessment path, opened from the native learner portal if needed. Staff MFA is required by default; the isolated general QA environment explicitly opts out and a separate MFA enforcement/TOTP test verifies the production rule. The 20 draft scenario completions remain non-certifiable. Existing independently reviewed simulation credentials keep their limited scope.

No remote hosting, production email configuration, paid provider activation or public release upload was performed.
