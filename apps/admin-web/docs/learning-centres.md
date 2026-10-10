# Fire Safety and First Aid institutional learning

The launch catalogue has a separate formative-learning workflow. Existing industrial assessments, practical review and certificate prerequisites are unchanged. `fire` in the launch catalogue must not be treated as a legacy industrial credential merely because the identifier matches.

## Native workflow

1. An approved centre registers the learner's exact phone-profile UUID. Staff can create a private invitation from the roster. A learner signs in using the invited email and explicitly accepts the invitation. No email or other message is sent automatically.
2. Educators open **Fire Safety & First Aid**, choose learner, course, school-awareness/adult scope, guided/independent mode and due date. The server pins the exact catalogue version, scenario IDs and original wording. School scope does not represent guardian consent.
3. Learners open **My assignments** and start a new practice in the assigned mode. Only the installed current content version can start new practice; archived assignments retain readable evidence and require a new current-version assignment for further learning.
4. **Private practice sync** previews records and requests separate upload consent. Both current draft2 and the exact bundled draft1 archive replay without regrading under different wording. Separate objective-revision records are never uploaded as full practice.
5. An explicitly selected subset of already-uploaded completed practice can be shared with the centre. It must belong to that profile, assigned scenario/version/mode, and have started after assignment creation. Private teach-back text is excluded. Already-shared or withdrawn identical snapshots are excluded from new selections.
6. Educators can read original event history, add feedback, record a separate training observation, or export a report. Learners can append correction requests and withdraw sharing. Neither feedback nor observations rewrite the original decisions, issue a certificate, attest real-camera performance, or establish clinical competence.
7. In-app notifications support read/unread, dismiss/restore and opening the authorised assignment. They do not send push, SMS or email. Routes recheck access and return structured destinations rather than arbitrary URLs.

Native entrypoints: `LearningCentreDialog.showLearner(activity, store)`, `showEducator(activity)`; `LearningInboxDialog.show(activity, store)`, `showEducator(activity)`. `SavedPracticeDialog` provides read-only retrieval and deliberate export of private copies without overwriting local history.

## API contract

All endpoints require the normal independent Suraksha account session and use `Cache-Control: no-store`. Existing centre approval, workspace selection and active staff role checks apply on every request. `admin` and `trainer` can author assignments/reviews/observations; `viewer` and `certifier` can read the workspace but cannot author this formative evidence. A learner cannot review or observe their own work.

| Endpoint | Read | Write |
| --- | --- | --- |
| `/api/learning-centre` | Current catalogue, roster, assigned work; `assignmentId` detail; `format=csv` report | `assign`, `cancel`, `review`, `observe` |
| `/api/learning-enrolment?workerId=…` | Linked learner's assigned work; `assignmentId` detail and available private records | `submit`, `withdraw`, `correction` |
| `/api/notifications?scope=learner&workerId=…` or `scope=educator` | Latest authorised events, loaded unread count; `showDismissed=true` | `read`, `unread`, `dismiss`, `restore`, `open` |
| `/api/learning-practice?workerId=…` | Private account copies, ten per page | Explicit validated append-only upload; see [sync contract](learning-practice-sync.md) |

Assignments use UUID request IDs. Creating the same request twice with the same details is safe; changing the details under an existing identity conflicts. The server accepts at most 20 learner IDs per assignment command; the native UI intentionally prepares one learner at a time. The server allows authored scenario subsets; the native form assigns all scenarios in the selected course/audience scope.

Feedback and correction commands include their own UUID and `previousEventId` (null for the first event). A stale parent yields 409. New event timestamps are monotonic even if requests land within the same millisecond. Observations have UUID identity, an explicit in-person-training/remote-simulation setting, observed time, three criteria and a note. All three criteria must be met for `observed`; the result remains non-certifying.

Assignment lists contain 50 rows with `nextCursor: {beforeAt,beforeId}`. Detail contains ten submissions and `nextSubmissionCursor` in the same shape. Supply both cursor fields along with the assignment ID to continue. Overall completion uses the full eligible shared set, independently of the page shown. Available private choices are capped at the newest 200 eligible candidates. Review history is capped at 300 entries for the loaded submission page; observations at the latest 100. The inbox contains the latest 100 events and explicitly reports truncation; old events remain in assignment history. CSV export covers the loaded assignment page and neutralises spreadsheet formula prefixes.

A learner linked to several institutions now selects one explicitly. `/api/learner` lists `owner` with each approved link. Learner enrolment and notification reads/commands accept `owner`; omitting it stays compatible with a single link but fails closed when ambiguous. Assignment destinations carry the same owner. Read/dismiss state is namespaced by account, institution and event, including intentionally colliding event IDs. Staff still use the authorised workspace cookie; a learner-supplied owner never grants staff access.

The native institution home shows launch-course assignments, notifications and private copies first. Legacy industrial records are behind a separate control. Legacy sync now binds account, server, profile **and institution**. Pending old unscoped transfers or copies for another institution stop before upload. An explicit reset removes only the encrypted pending transfer copy; local learning remains available to rebuild the transfer. Single-centre API callers remain supported. Content-only coaching checks current approved membership without arbitrarily choosing a centre.

## Storage, privacy and deployment

Apply `0014_learning_practice.sql` followed by `0015_learning_centres.sql`, `0016_learning_centre_guards.sql`, `0017_learning_account_cleanup.sql`, and `0018_learning_inbox_scope.sql` after a database backup. They do not migrate credential evidence. Migration 0018 retains old read/dismiss state only where the old event ID resolves to one institution; ambiguous IDs reopen as unread. These migrations were applied **locally only** on 10 October 2026 with SQLite backups. No production schema, deployment or invitation delivery was performed.

Shared copies store decisions, retries, hints and self-reported model exploration. Teach-back free text stays in the private account snapshots. Withdrawal removes future centre access but cannot recall earlier exports. Unlinking a learner or suspending a centre blocks access; observations are pinned to the learner account present when recorded, preventing an account replacement from revealing previous observation notes.

Feedback events and source snapshots are immutable. Transaction guards recheck approved centre, active role, current learner link, distinct educator identity and feedback parent. Account deletion removes shared learner evidence, its feedback and account-pinned observation notes; institutional assignment definitions remain. Existing retention, backups, account deletion access and policy must be disclosed accurately before deployment.

Safety content remains source-checked **draft**, with no qualified clinical or instructor approval recorded. No live AI provider was enabled or tested. Contextual coaching continues to fail closed for unapproved safety instruction. These features do not make the build production approved or provide formal qualifications.

## Verification

- Unit and SQLite regressions cover exact archived replay, school/adult scope, profile/mode/time/version matching, free-text exclusion, feedback immutability, stale-parent rejection, self-review, revoked role/link/centre access, irreversible withdrawal, CSV safety and account deletion with dependent feedback.
- `tests/learning-centre.integration.ts` exercises the real local HTTP service using synthetic accounts: invitation redemption, tenant/role isolation, assignment retries, private sharing, educator feedback/correction, observation retries, all inbox actions, revoked destinations, two-page submission history, total completion and absence of legacy credentials. Test fixtures are removed afterward.
- Android compilation and JVM session-race tests passed. The response guard preserves harmless token refresh while invalidating actual sign-in/sign-out/origin/workspace replacements. A server-origin and cookie snapshot is atomic; stale responses cannot clear or overwrite a replacement account. Account sheets cancel queued callbacks on dismissal/lifecycle exit.
- Real-device delivery and full visual inspection of these newly added educator/account panels remain separate from the API/JVM results. Do not describe an HTTP test as physical-device or production verification.
