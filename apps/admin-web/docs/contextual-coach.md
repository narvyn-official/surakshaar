# Contextual learning coach — implementation contract

Implemented on 10 October 2026. The current feature is a bounded selector of authored references, not a medical adviser or an autonomous scene controller. The current Fire and First Aid scenario pack is **draft**. Clinical/fire-instructor approval has not been recorded. A source cross-check, AI review or a published guideline link is not qualified approval.

## API

`POST /api/tutor/context` accepts JSON with the normal Suraksha account session:

```json
{
  "courseId": "first-aid",
  "contentVersion": "learning-2026.10.10-draft1",
  "scenarioId": "adult-collapse",
  "objectiveId": "safe",
  "recentActionIds": [],
  "hintLevel": "controls",
  "language": "en",
  "mode": "learn",
  "allowExternalCoaching": false
}
```

- Optional `workerId` must be a UUID linked to the signed-in account in an approved centre. Omitting it enables signed-in personal learning without inventing a centre membership. Neither form retrieves another learner's history.
- Course: `fire` or `first-aid`. Version must exactly match `content/learning/scenarios.json`. Scenario and objective (`step.id`) must belong to that course. Unknown combinations fail.
- Optional `selectedObjectId` must be in that objective's authored part list. Do not send whichever unrelated structure the learner happens to have inspected earlier. Omit when nothing relevant is selected.
- `recentActionIds`: at most 12 authored choice IDs belonging to the same scenario. They are untrusted context, never assessment evidence. Choice IDs can reveal decisions or permit correctness inference when mapped to published lesson content; no separate correctness flag is sent. No text notes, scores, pupil names, precise locations, ages, camera frames, voice or patient information are accepted.
- `hintLevel`: `controls`, `explain`, `hint`, `reflect`. Assessment mode blocks all coaching. The device must send assessment mode honestly; the API does not claim to prevent someone opening a second learning session while taking an offline assessment.
- `language`: `en`, `hi`, `sat`. Santali returns `language-unavailable` with `responseLanguage: en`, visibly declared rather than falsely labelled a Santali answer. Product-control references are authored in English and Hindi; this does not claim reviewed clinical translation coverage.
- `allowExternalCoaching` defaults false. Set true only after an appropriate, clear disclosure of the configured provider and data handling. For children/institutions, the operator must establish the applicable consent and privacy process before enabling external processing. The boolean is a transport opt-in, not evidence of parental or legal consent.

Replies have `schemaVersion`, `status`, `mode`, `aiStatus`, `text`, `contentVersion`, requested `language`, actual `responseLanguage`, nullable `explanationId`, `actions[{id,label}]` and `sources[]`. A `providerDisclosure` is returned only for a configured service; it includes the operator-supplied provider name, privacy URL and processing/retention notice. Show this before enabling the per-request external-processing opt-in. Every response is `Cache-Control: no-store`.

`mode: authored-reference` must be labelled authored assistance. `mode: provider-selection` means the provider selected an authored card; it did not write the safety answer. Display `aiStatus` honestly: `not-requested`, `disabled`, `missing-config`, `invalid-config`, `review-required`, `assessment-blocked`, `language-unavailable`, `unavailable`, or `selected`.

The only action IDs are `inspect-selection`, `show-step`, `open-sources`, `open-replay`. They are UI suggestions. The client must only expose actions it implements, require a learner tap, validate its current scene/state and ignore stale responses after navigating or switching a profile. No response may auto-advance the lesson, complete a task, move an anchor, alter a score, change access or issue a certificate. No action is executed on the server.

## Approval and model boundary

The registry is built from the shared learning JSON; there is no manually copied course-version table. Current safety intent requests return `review-required` and make **zero provider calls**. Product controls can return a simple authored reference even without an AI service. No draft treatment/fire explanation or correct-choice flag enters the provider request.

Future safety coaching requires both a reviewed scenario (approved status plus a named reviewer) **and** explicitly authored cards with a separately approved source and reviewer. Changing the scenario status alone does not make its content available to AI. Keep approval records tied to exact content versions, scopes and qualified reviewers; do not use model-generated text to create those records.

A configured provider receives only a projection of allowlisted context IDs, language, hint level, and currently eligible cards. It returns an explanation ID plus a subset of that card's allowed UI actions. Extra fields, model prose, invented IDs, foreign actions, duplicate actions and malformed responses are rejected. The app displays the original authored text and original source metadata, never model-supplied text or citations. There are no model tools, database writes beyond rate budgets, retrieval URLs, diagnosis, grading or certification capabilities.

## Operator configuration

No live provider is configured or verified by this change. Existing `TUTOR_PROVIDER_*` configuration belongs to the older product-question classifier and is not silently reused.

The optional provider-independent gateway accepts HTTPS `POST` JSON with protocol `suraksha-context-coach-v1`, task `select-authored-coaching-card`, bounded context/candidates/instructions, and an output contract. It must return `Content-Type: application/json` and only:

```json
{"explanationId":"controls-return-en","actionIds":["show-step"]}
```

Store `COACH_PROVIDER_KEY` only in backend secret storage. Backend configuration also supplies `COACH_PROVIDER_URL`, `COACH_EXTERNAL_ENABLED=true`, `COACH_PROVIDER_NAME`, `COACH_PROVIDER_PRIVACY_URL` and `COACH_PROVIDER_DATA_USE` (a concise, accurate processing/retention notice). Missing disclosure fields leave the provider unavailable. The URL must be an operator-reviewed public HTTPS gateway; redirects, credentials in URLs, query strings, local/IP destinations and nonstandard ports are rejected. Do not configure a model-supplied URL. Gateway/provider DNS and outbound access remain the operator's responsibility; these URL checks are not a DNS-level network firewall.

Each model call has a 3-second deadline, a maximum 8 KiB JSON response and no automatic retry. Failure/cancellation retains the authored reference and reports that AI was unavailable. Both `/api/tutor` and this route use the same durable account budgets: 10/minute and 60/hour. Rate-limit rows retain only account identifier, time bucket and request count and are pruned by subsequent traffic after approximately one day/hour respectively. No prompt, camera, voice, response or action-history telemetry is stored by this endpoint. A chosen provider may have its own processing/retention; document that explicitly before enabling it. No live provider privacy policy is claimed.

## Evidence and limits

Automated tests exercise every current objective's draft gate, stale content/foreign scenarios/foreign objects, unauthorised access ordering, input size/origin checks, budget handling, consent, identifiers omitted from provider input, assessment mode, unavailable language, provider injection/output rejection, cancellation/timeouts and honest fallback. Live provider quality, native UI integration, real user consent flow and production external processing remain separate checks. Existing certification policy, issuer keys, course prerequisites, evidence records and legacy curriculum are unchanged.

Primary implementation references checked 10 October 2026:

- [OWASP GenAI Security Project](https://owasp.org/www-project-top-10-for-large-language-model-applications/): treat model output as untrusted and keep authority outside the model.
- [Cloudflare Workers secrets](https://developers.cloudflare.com/workers/configuration/secrets/): backend secret bindings; never client bundles or committed values.

## Admin diagnostics

`GET /api/tutor/readiness` requires an approved workspace **admin** and reports configuration status, missing field names, public disclosure, content version and review counts. It does not contact a provider, expose credentials/endpoint values, or call a configured connection verified. The native admin **Centre settings → AI service readiness** panel uses this route.

`POST /api/tutor/readiness` accepts only `{ "action": "probe", "confirmExternalRequest": true }`. The explicit test shares a synthetic controls-only context without learner IDs, answers, camera or voice. It uses the same budget, timeout, byte limit and output allowlist as coaching. Passing means only that the gateway selected an allowed authored control card; it is not clinical approval or production readiness. Viewer/trainer/learner access is denied. No live provider was configured or contacted in this work.
