**SurakshaAR platform architecture**

Architecture baseline for the proposed redesign · 10 October 2026

SurakshaAR will serve personal learners, households, schools, colleges, ITIs, workers and community programmes through a common learning platform. Personal learning will be easy to enter and unproctored. Each onboarded institution will operate its own learner directory, training programmes, examinations, examiner reviews and certificates. The platform owner will provide the service, reviewed shared content and governance controls; routine institutional certification will not require the owner's approval.

This is the proposed implementation specification. Existing preview features are reusable inputs, not evidence that the new architecture has been implemented or validated. Delivery covers every audience from the first release; richer curriculum and automation can expand within that shared structure.

**1. Product decisions**

| Area | Architecture decision |
|---|---|
| Product model | One platform, multiple personal and institutional contexts |
| Learning modes | Structured courses, open exploration, guided practice, independent simulation assessment and supervised practical observation |
| Institutional ownership | Institutions enrol their own people, appoint staff, configure programmes and issue their own certificates |
| Personal access | Guest exploration; optional Google sign-in or email OTP to save and synchronise progress |
| Institutional access | Verified membership, normal authentication and a fresh selfie at the start of each new learner institutional session; another identity check before a protected assessment |
| Assessment verification | Disclosed random snapshots with examiner review; optional one-to-one face matching and liveness checks |
| Certification | Evidence and issuer are explicit; learning completion, simulation assessment and observed practical performance remain distinguishable |
| Implementation | Native Android learning and AR; responsive web learning and institutional administration; shared API and content contracts |
| Backend structure | A modular monolith with a separately runnable background worker |
| Privacy boundary | Personal records, each institution's records and examination images have separate access rules |
| Customisation | Versioned configuration, templates and approved content extensions; a common codebase |

**2. Audiences and learning contexts**

| Context | Relevant users | Complete initial journey |
|---|---|---|
| Personal and household | Individuals, parents, caregivers and older people | Explore a topic, practise a scenario, understand mistakes and revisit progress |
| School and campus | School students, college students, ITI learners and educators | Join a programme, complete assigned work and receive suitable feedback or observation |
| Workplace | Workers, apprentices, contractors, supervisors and trainers | Enrol, complete role-specific training, take an assessment and receive the institution's decision |
| Community | NGOs, facilitators and programme participants | Join a cohort, use shared devices where needed and complete facilitated learning |

Audience selection changes recommendations, vocabulary, age suitability and scenario context. It does not grant staff permissions. A person may belong to multiple institutions while retaining a separate personal space. Each workspace selection is visible throughout the journey. Children use an age-appropriate route and linked adult or institutional arrangements where required; child accounts do not inherit adult workplace surveillance settings.

**3. Platform surfaces and navigation**

| Surface | Main navigation and responsibility |
|---|---|
| Public website | Purpose, interactive demonstration, learning catalogue, institutional offering, pricing explanation, sources, support and account entry |
| Learner app and web | Home, Learn, Explore, My work and My record; Downloads and Settings are secondary destinations |
| Institution workspace | Overview, People, Programmes, Assignments, Examinations, Review, Certificates, Reports and Settings |
| Platform owner workspace | Institutions, Plans, Shared content, Content reviews, Service health, Support access and Audit |
| Certificate verification | A minimal public record reached through an unguessable QR link, showing issuer, qualification scope and current status |

Learner Home prioritises Continue, assigned work and recent exploration. Institution staff receive a stable navigation shell with filtered lists and detail views. On a phone, a detailed examiner review becomes a full page. Private examination evidence is excluded from public search, general analytics and normal learner sharing.

Every primary flow includes entry requirements, saved state, success, cancellation, permission denial, service failure and recovery. Notifications link directly to the relevant assignment, review or certificate. A successful action produces a durable record, not only a temporary toast.

**4. Institutional onboarding and independence**

The platform owner creates or approves an institution, verifies its named administrator and allocates the contracted plan, module entitlements, storage and assessment allowances. That administrator completes a setup wizard: identity and branding, branches, learner groups, staff permissions, assessment rules, privacy notices, retention settings and certificate templates.

The institution then independently imports workers or students, sends invitations, verifies institutional enrolment, assigns learning, appoints examiners, reviews submissions and issues certificates. Invitations can be distributed by authorised staff as links or printed QR codes; an institution code and employee number alone are never sufficient authentication.

Institution states are Draft, Active, Restricted and Closed. Restrictions may stop new assignments or issuance, while preserving existing learner records and certificate verification according to the service agreement. Subscription expiry does not silently invalidate a legitimately issued certificate. Closure includes export, access removal, retention and key-history handling.

**5. Roles and authority**

| Role | Allowed responsibility | Boundary |
|---|---|---|
| Platform owner | Provision institutions, manage plans, publish shared content and administer service operations | Routine access does not expose examination photos or alter institutional grades |
| Institution administrator | Manage members, branches, policies, programmes, templates and delegated roles | Authority is limited to that institution |
| Instructor | Assign learning, give feedback and record permitted practical observations | Cannot automatically issue certificates or browse unrelated cohorts |
| Examiner | Review assigned attempts, authorised identity evidence and practical observations; request clarification or retest | Cannot rewrite submitted answers or approve their own attempt |
| Certificate issuer | Approve and issue qualifying institutional certificates, revoke or replace them with reasons | Cannot bypass missing evidence by editing a score |
| Content author and reviewer | Prepare and approve scoped content revisions | Draft content cannot become approved through an ordinary administrator edit |
| Learner | Access personal and assigned learning, submit work, view decisions and appeal |
| Guardian or facilitator | Assist explicitly linked profiles within the permitted scope | Does not acquire unrestricted access to an adult's records |

An institution may assign examiner and issuer responsibilities to the same qualified staff member for ordinary programmes. A separate reviewer and issuer can be required for higher-assurance programmes. Neither arrangement requires SurakshaAR owner approval for each certificate. Staff cannot assess or certify themselves. Support access requires a recorded reason, time-limited scope and an audit trail.

**6. Personal login and institutional login**

Personal learning starts with language and topic selection. Guest progress stays on that device. Google sign-in or email OTP adds account recovery and synchronisation without examiner approval, selfie collection or face analysis. Guest history is merged only after the user confirms the correct profile. Personal completion records clearly state that identity and practical performance were not independently verified.

Institutional learners authenticate through an invitation-activated account, supported institutional SSO or an issued credential with an appropriate recovery route. Staff accounts require MFA. Before entering a new institutional learner session, the app explains the configured identity policy and captures a current selfie. During assessment preflight it captures a fresh attempt-specific check, preventing an old login session from being treated as current presence evidence.

An institution must first approve the learner's reference identity through a supervised enrolment check or a trusted institutional identity process. A self-uploaded photograph alone is insufficient: a substitute could otherwise enrol themselves and pass subsequent comparisons. Changing a reference photo requires reapproval, records a new version and does not rewrite old attempts.

In manual-review mode, the session selfie is labelled Captured, awaiting review. In face-assisted mode, a passing check is labelled Automated check passed, with its method and time. Neither label implies government identity verification. Failed or unavailable verification offers bounded retries, examiner assistance or an approved supervised route; personal learning remains accessible.

**7. Curriculum and shared learning engine**

The content model connects Domain, Competency, Course, Lesson, Scenario, Asset, Assessment rubric and Revision activity. Topic, setting, age suitability, language, prerequisites and delivery format are metadata, allowing reviewed components to be reused without presenting every learner with identical procedures.

| Domain | Main learning purpose | Spatial learning opportunity |
|---|---|---|
| Fire and burns | Prevention, alarm, evacuation decisions, equipment familiarity and reviewed burn response | Inspect a scene, locate hazards and examine equipment |
| First aid | Recognition, escalation and reviewed response to selected casualty situations | Body relationships and practice around a training manikin |
| Electrical safety | Recognise exposure, energy sources and unsafe approach decisions | Explain hidden paths and equipment boundaries |
| Machinery | Guards, moving parts, pre-use inspection and approved isolation sequences | Part identification, exclusion zones and stored-energy models |
| Falls and surroundings | Home, campus and workplace hazard recognition | Inspect stairs, access routes and working areas |
| Laboratory and chemical safety | Labels, exposure prevention and approved response decisions | Equipment and containment relationships |
| Water and rural safety | Prevention, safe assistance decisions and referral awareness | Selected visual scenarios; AR only where useful |
| Disaster preparedness | Setting-specific preparedness, evacuation and role coordination | Rehearse an approved practice setting |

The first release includes a reviewed common fire and first-aid foundation for all audiences, setting-specific variants and initial electrical and machinery pathways for relevant learners. Other domains enter through the same contracts as content is reviewed. Broad audience access does not imply that every specialist procedure is available or validated at launch.

Anatomy supports emergency decisions through simplified heart, lung, airway, circulation, skin and skeletal models. Equipment exploration supports hotspots, section views, labels, narrated explanations and guided inspection. Medical and engineering reviewers approve the relevant representations. No phone-camera feature diagnoses injuries, certifies electrical isolation or detects hazardous gas.

**8. Courses, exploration and assessment**

Structured courses establish prerequisites and progression. Explore allows users to inspect bodies, equipment and hazards freely. Guided practice permits hints and retries. Independent simulation assessment records choices without directional hints. Practical observation records an authorised instructor's assessment of physical skills that the screen cannot establish.

Use a versioned, declarative scenario specification containing states, allowed actions, consequences, explanations, assessment criteria and renderer-independent object IDs. Android and web interpret the same specification through platform adapters. The server re-evaluates submitted evidence against the pinned rubric. Shared test fixtures verify equivalent scoring across implementations; visual similarity alone does not establish parity.

Unsafe critical actions can require remediation regardless of an overall numeric score. The content reviewer defines those gates. Server-issued assessment variants vary equivalent reviewed situations, not arbitrary difficulty. Learner assistance, presentation mode and assessment version are recorded. Anonymous exploration and self-assessed practice never silently become verified institutional evidence.

An optional learning coach can explain the current lesson using retrieved, approved content and visible source references. It remains available during learning and guided practice, and is disabled during independent examinations. Expert-authored rubrics determine grades. The coach receives no examination photos, biometric templates or another learner's records, and cannot publish new safety procedures, diagnose a casualty or approve a certificate. Authored explanations remain available when the model is unavailable or the answer lacks support.

**9. Assessment identity and snapshot verification**

| Policy | Behaviour | Suitable use |
|---|---|---|
| Self-directed | No proctoring photos or biometric checks | Personal learning |
| Examiner-reviewed snapshots | Approved reference identity, session and preflight selfies, random assessment snapshots, examiner review | Default institutional remote assessment |
| Face-assisted verification | The same evidence plus one-to-one comparison and a validated liveness step | Institutions that enable and validate biometric verification |
| Supervised assessment | In-person identity and practical observation by authorised staff | Physical skills, camera limitations or an approved accommodation |

Automatic photography is disclosed before the learner starts. The assessment displays a persistent Camera verification active indicator. Once permission and the applicable acknowledgement are obtained, each snapshot can be captured and saved without a separate click. Photography stops on submission, cancellation, timeout or leaving the active assessment. Encrypted upload of already captured images can continue later when the operating system permits it. Background saving is not covert camera operation.

During non-AR examination segments, an initial policy can request randomly timed snapshots at intervals between 90 and 180 seconds, bounded by a per-attempt limit. These are proposed defaults to validate, not a research-proven anti-cheating interval. Additional checkpoints occur at the start, after an authorised interruption and at submission. No audio or continuous video is recorded by default. A separate liveness check may require a disclosed short video.

The server creates a short-lived capture challenge bound to the institution, learner, session and attempt. Each evidence record contains the challenge, sequence, image digest, device capture time, elapsed session time, server receipt time, camera result, quality result and optional face-check result. Device time is untrusted; delayed uploads retain that uncertainty. Duplicate challenges, replayed evidence and conflicting event sequences require review. Hashes and device integrity signals help detect some tampering but cannot prove a photograph is authentic.

Images live in private encrypted storage, never in a public gallery. The examiner sees a chronological timeline beside the approved reference photo, answers, interruptions and evidence gaps. Opening or exporting evidence is logged. Ordinary institution administrators do not receive examination-photo access unless assigned the relevant review permission. Downloads are disabled by default and narrowly authorised when necessary.

**10. Face matching and liveness logic**

Face verification compares the current learner only with the institution-approved reference for that learner. It does not search the entire population or create a cross-institution face directory. Face detection first checks whether a usable face is present. It is not identity verification: Google's ML Kit explicitly distinguishes detection from recognising individuals. [ML Kit documentation](https://developers.google.com/ml-kit/vision/face-detection)

A provider adapter separates detection, image quality, liveness and comparison. The initial optional cloud candidate is AWS Rekognition CompareFaces with Face Liveness, subject to supported region, contracts, SDK integration, cost and representative-device evaluation. Snapshots remain usable with manual review when the provider is disabled. A new or unlicensed face model is not assumed to be production-ready. [Face comparison](https://docs.aws.amazon.com/rekognition/latest/dg/faces-comparefaces.html), [Face Liveness](https://docs.aws.amazon.com/rekognition/latest/dg/face-liveness.html)

Automated outcomes are Check passed, Retry needed and Examiner review required. Low quality, multiple faces, liveness uncertainty, a comparison mismatch or provider outage must not produce an automatic accusation of cheating. The examiner considers the context, can request a supervised retest and records a reasoned decision. The learner can appeal. A successful face match supports presence continuity; it cannot establish that no off-camera assistance occurred.

Thresholds are selected using measured false-match and false-rejection rates on the intended population and devices. A similarity score is not a calibrated probability that a person is genuine. NIST documents the effects of image quality and demographic variation, making local evaluation and an alternative verification route material requirements. [NIST demographic evaluation](https://pages.nist.gov/frvt/html/frvt_demographics.html)

**11. Camera ownership and AR examinations**

One app-owned camera coordinator grants exclusive use to AR tracking, selfie capture or another approved camera task. It controls acquisition, pause, release and recovery. Native AR state and attempt state are separate, so switching camera ownership does not lose submitted actions.

Rear-camera AR and front-camera verification are not assumed to operate together. CameraX concurrent-camera support is device-dependent, and support for that API does not establish compatibility with an active ARCore session. ARCore camera sharing also needs its own tested integration. [Android concurrent cameras](https://developer.android.com/reference/androidx/camera/core/ConcurrentCamera), [ARCore camera sharing](https://developers.google.com/ar/develop/java/camera-sharing)

The default AR assessment flow verifies identity before the spatial segment, at safe checkpoints and afterward. At a checkpoint the app saves the scene state, pauses the task and timer as policy permits, releases the rear camera, explains the selfie check, then restores and re-localises the scene. A learner is never prompted to manipulate a phone while operating live hazardous equipment. Uninterrupted front-camera evidence during AR requires a tested compatible device, approved second device or supervised session; the resulting evidence method is recorded honestly.

Leaving the app or hiding a browser tab records an interruption. Capture stops and the pre-agreed examination rule determines whether time continues, the attempt pauses or a retake is required. Camera permission denial offers recovery or a supervised route. Android has while-in-use restrictions on camera permissions; the design does not depend on unrestricted background photography. [Android camera-service restrictions](https://developer.android.com/about/versions/14/changes/fgs-types-required)

**12. Examination lifecycle and decision logic**

The attempt lifecycle is Assigned, Eligible, Preflight, In progress, Paused when permitted, Submitted, Evidence processing, Examiner review, then Accepted, Retest required or Rejected with reasons. A successful acceptance can proceed to Certificate eligible and Issued. An appeal creates a separate review record; it never erases the original decision.

Preflight checks enrolment, prerequisites, reference approval, current policy, required permissions, device capability, connectivity and available storage. The learner sees the time rule, permitted breaks, required evidence, submission behaviour and alternative route before committing.

The server is authoritative for deadlines, attempts allowed, scoring, reviewer authority and certificate eligibility. It atomically accepts submission once. Retried requests return the existing receipt. An outstanding evidence upload produces Submitted, evidence pending rather than an inaccurate success or failure message. The final decision uses separate fields for learning performance, practical observation, identity review and evidence completeness.

| Problem | Required behaviour |
|---|---|
| Camera unavailable or image unusable | Explain, allow bounded retry, preserve work and route to the configured alternative |
| Network loss | Apply the frozen attempt policy; retain permitted encrypted evidence and show its status |
| App crash or restart | Restore allowed state, record the gap and repeat preflight identity checks where required |
| Storage full | Stop new capture safely and display recovery; do not discard answers silently |
| Face provider outage | Mark processing pending or use authorised manual review; never invent a match |
| Examiner access revoked | Block further evidence access, revoke streaming access and reassign the review |
| Policy changes during an attempt | Keep the recorded policy version, except an explicit safety withdrawal that invalidates further use |
| Duplicate submission or issuance | Use idempotency and database constraints to prevent duplicate outcomes |

**13. Institutional certificates**

The institution controls templates, signatories, programme names, required competencies, validity periods and renewal rules. The shared certificate engine checks eligibility, records the authorised decision and signs an issuer-specific credential. A staff member cannot convert missing practical evidence into an observed-practical claim by changing the template.

Three evidence descriptions are supported: personal learning completion, institution-assessed simulation completion and institution-observed practical assessment. Certificates can combine assessed components while identifying each component's method. They do not imply statutory recognition, professional registration or permission to operate equipment unless the issuer has a separately established authority and explicitly valid scheme.

Each credential contains a unique identifier, issuer, learner identity appropriate to the programme, competencies, content/rubric versions, assessment methods, issue date, expiry where applicable and status reference. Sign with an institution-specific key managed through a key-management service; the platform never gives signing secrets to browsers. A replacement preserves the predecessor link. Revocation records a reason and takes effect on online verification.

The QR points to an unguessable verification record with minimal public data. It never exposes selfies, employee numbers, answers or detailed marks. Full name disclosure is controlled and explained. Verification shows Active, Expired, Revoked or Replaced. Offline signature checking can establish signature validity but must label revocation status as last checked or unavailable. Leaving an institution does not automatically erase or revoke a valid credential.

**14. Institutional configuration and limits**

Institutions can configure branding, branches, departments, cohort names, learner fields, course selection, approved local procedures, languages, deadlines, attempt limits, reviewed passing criteria, identity mode, bounded snapshot frequency, review assignments, signatories, certificate templates and permitted retention periods. SSO, custom domains, bulk import, reports and integrations are entitlement-controlled capabilities.

A policy is drafted, validated, previewed and published as a version. Existing attempts retain the version they started with. Platform safeguards prevent an institution from disabling tenant isolation, hiding capture notices, allowing self-certification, suppressing appeals, bypassing required practical evidence or publishing unsafe unreviewed instructions as approved shared content. An administrator's custom text cannot execute code or arbitrary scripts.

Content approval and student examination approval are different authorities. An institution can appoint its own qualified reviewer for local procedures. Public catalogue publication remains a separate platform editorial decision.

**15. Data architecture**

| Entity group | Principal records |
|---|---|
| Identity | User, Login identity, Session, Learner profile, Guardian relationship |
| Institution | Organisation, Branch, Membership, Role grant, Cohort, Invitation |
| Policy | Policy version, Entitlement, Notice version, Acknowledgement, Retention rule |
| Learning | Competency, Course version, Lesson, Scenario version, Asset manifest, Localisation review |
| Delivery | Enrolment, Assignment, Practice attempt, Examination attempt, Action event |
| Verification | Identity reference, Reference approval, Capture challenge, Evidence object, Check result |
| Review | Practical observation, Review case, Decision, Appeal |
| Certification | Issuer, Certificate template, Credential, Signing key reference, Revocation |
| Operations | Audit event, Outbox job, Notification, Subscription, Usage counter, Deletion job |

Institution-owned records carry organisation identifiers with composite foreign keys and scoped uniqueness. Employee numbers are unique within an institution, not globally. Personal learning uses a separate ownership scope rather than a magic institution ID. Evidence objects are bound to a specific attempt and membership. Reference photos and biometric templates are sensitive records with their own retention and access controls.

PostgreSQL row-level policies provide a second enforcement layer alongside application authorisation. Runtime database roles cannot own protected tables or bypass row security. Tenant context is set and cleared within each transaction, including pooled connections and worker jobs. Database owners and superusers can bypass ordinary policies, so merely enabling RLS is insufficient. [PostgreSQL row security](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)

Keep raw images in object storage and metadata in PostgreSQL. Do not place images, face templates, OTPs or tokens in ordinary logs, analytics, crash reports or public URLs. Search, exports, notification links and storage retrieval enforce the same ownership rules as ordinary pages.

**16. System architecture**

```text
Android learner app       Responsive learner and staff web
        |                             |
        +---------- HTTPS API --------+
                         |
                Modular application
  Identity | Institutions | Curriculum | Assignments
  Attempts | Evidence | Reviews | Certificates | Billing
                         |
       +-----------------+------------------+
       |                 |                  |
   PostgreSQL       Private object      Job worker
   and job queue       storage       processing and delivery
       |                 |                  |
       +------ Audit and observability -----+
                         |
       Optional adapters: identity verification, SSO,
       email/SMS, payments and certificate integrations

Reviewed learning assets are distributed separately through a CDN.
Private examination evidence never enters the public asset cache.
```

Keep module boundaries in code and contracts, without operating a service for every feature. The API is stateless apart from durable data; workers process uploads, comparison jobs, reports, certificates, notifications and deletions. Use a transactional outbox so a committed decision cannot silently lose its corresponding job. Retry-safe handlers and unique issuance constraints protect against repeated delivery.

One REST API with OpenAPI contracts serves Android and web. Stream or poll review progress where useful; do not introduce permanent socket connections for every learner by default. Database transactions own final submission, accepted reviews and issuance. Content and evidence uploads use bounded upload sessions and server-side validation before becoming reviewable.

**17. Recommended technology stack**

| Layer | Selected direction | Reason and implementation boundary |
|---|---|---|
| Android | Kotlin, Jetpack Compose for new UI, existing native components behind adapters | Retain working native learning and AR foundations; migrate incrementally |
| Android AR and rendering | ARCore plus Filament, through one app-owned runtime adapter | Separate tracking, rendering, camera ownership and scenario state |
| Android persistence | Room over SQLite, app-private encrypted evidence files, WorkManager upload queue | Durable learning and retryable upload; WorkManager does not take background selfies |
| Web | TypeScript, React, stable Next.js App Router | Public site, learner routes and institutional workspaces in one web codebase |
| Web controls | Existing Base UI-based primitives and semantic CSS/Tailwind tokens | One component system with accessible forms and consistent behaviour |
| Web 3D | Three.js with React Three Fiber and selected helpers | Custom anatomy/equipment interactions; scene rendering loaded only when needed |
| Content assets | glTF/GLB with versioned manifests and validated compression | Share authored assets while using platform-specific renderers |
| API | TypeScript on supported Node.js LTS, Fastify, OpenAPI and schema validation | Shared mobile/web business rules and explicit module boundaries |
| Authentication | Better Auth with reviewed provider and organisation integrations | Reuse current direction; Google/email OTP for personal use, stronger institutional controls |
| Database | Managed PostgreSQL with Drizzle migrations | Relational records, transactions, scoped access and evidence integrity |
| Jobs | PostgreSQL-backed pg-boss plus transactional outbox | Begin with one durable data system; add queue infrastructure when measurements justify it |
| Storage | Private S3-compatible object storage; separate public content storage/CDN | Encrypted evidence, lifecycle deletion and direct bounded uploads |
| Face checks | ML Kit for native face presence/quality cues; optional provider adapter for matching and liveness | Detection is not recognition; manual review works independently |
| Operations | OCI containers, reverse proxy/TLS, managed secrets/KMS and OpenTelemetry | Portable deployment, controlled credentials and observable failures |
| Verification | Unit and contract tests, Playwright, Android device tests, accessibility checks and load tests | Validate workflows and trust boundaries, not just screens |

Primary capability references: [Compose interoperability](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis), [Filament](https://github.com/google/filament), [Room](https://developer.android.com/training/data-storage/room), [WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent), [Next.js self-hosting](https://nextjs.org/docs/app/guides/self-hosting), [Fastify](https://fastify.dev/docs/latest/), [Better Auth](https://better-auth.com/docs/introduction), [email OTP](https://better-auth.com/docs/plugins/email-otp), [Drizzle RLS](https://orm.drizzle.team/docs/rls), [pg-boss](https://github.com/timgit/pg-boss), [glTF](https://www.khronos.org/gltf/).

Exact package versions, licences, provider regions and interoperability are locked during implementation and maintained thereafter. Native Android carries the initial spatial-assessment implementation. iPhone/iPad and other supported devices receive web learning and capability-tested assessments; native iOS AR is an extension of the same contracts, not an assumed first-release equivalent. Device support must be published honestly before enrolment into a required assessment.

**18. Offline and synchronisation rules**

Personal lessons, downloaded exploration and practice work offline. Their records queue for synchronisation with visible Pending, Uploaded or Needs attention states. Each profile has separate local storage access, and switching accounts cannot show a previous learner's cached results.

Remote verified examinations require connectivity at start, submission and required verification checkpoints. A policy may permit a bounded outage with encrypted local evidence, but the gap remains visible and certification waits for required evidence and review. Fully offline institutional examinations use an approved supervised route; uploaded local logs alone do not prove identity continuity.

Content manifests are signed, versioned and expire or refresh according to policy. Active attempts pin their content and rubric versions. Server-side safety withdrawals can block new starts and require intervention for affected attempts. Deletions propagate to originals, thumbnails and derived biometric data; offline copies are removed at next contact or local expiry. Immediate deletion from a disconnected device is not promised.

**19. Privacy and verification governance**

Before enabling assessment capture, the institution records the purpose, applicable processing basis, users affected, evidence recipients, provider, retention period and alternative route. A camera permission prompt is not a substitute for this notice. Where consent is the appropriate basis it must be specific and recorded; workplace or school consent must not be assumed to resolve every legal obligation. Institutions and SurakshaAR allocate responsibilities for their actual data-processing roles in their agreement.

The initial product default is to delete raw assessment snapshots 30 days after the final decision, with a 90-day maximum from capture for unresolved ordinary cases. A justified, access-restricted appeal or required preservation can extend this under a recorded rule. These are proposed product defaults, not statutory retention periods. Approved identity references have a separate lifecycle tied to membership and legitimate verification needs. Certificate metadata and audit events follow a separately justified schedule; photography is not retained simply because a certificate lasts longer.

Support correction, access requests, withdrawal where relevant, deletion and review of identity disputes. Disclose backup expiry and approved preservation exceptions. Biometric verification is disabled by default for minors and requires separate legal and product review before any institution can enable it. A supervised alternative remains available. Exam photos cannot be repurposed for attendance, advertising, disciplinary profiling or model training under the examination notice.

India's final Digital Personal Data Protection Rules 2025 use phased commencement. The release review must consider the provisions actually in force, subsequent corrections and the deployment context; this architecture is not a claim of legal compliance. [MeitY rules notification](https://www.meity.gov.in/static/uploads/2025/11/53450e6e5dc0bfa85ebd78686cadad39.pdf), [MeitY rules and corrigendum listing](https://www.meity.gov.in/documents/act-and-policies/digital-personal-data-protection-rules-2025-gDOxUjMtQWa?pageTitle=Digit)

**20. Visual identity and interaction design**

Use a contemporary technical field-guide direction: warm neutral reading surfaces, deep navy text, teal actions, realistic equipment cutaways, numbered procedures and clear evidence timelines. Preserve the recognisable navy/teal foundation while replacing dense or inconsistent layouts through the redesign. The signature interaction connects a selected object or action to a plain-language explanation of its mechanism and consequence.

| Token | Light appearance | Dark appearance |
|---|---|---|
| Canvas | #F6F4EF | #101C24 |
| Surface | #FFFFFF | #182932 |
| Primary text | #162B3B | #EAF3F2 |
| Secondary text | #4D5F6D | #B8C9D0 |
| Action | #006B63 | #74D9C6 |
| Text on action | #FFFFFF | #101C24 |
| Warning text | #854A00 | #FFD28A |
| Error text | #B42318 | #FFB4A9 |

These tokens require complete state and rendered-contrast checks. System, Light and Dark are independent of the institution's brand accent. Institution branding cannot override accessible status colours or camouflage verification notices. Use system font stacks initially, with reviewed Indic-script fallbacks and tested large-text behaviour.

The learner experience uses one clear next action, a visible learning path and generous touch controls. The exploration workspace places the model beside a labelled inspector, with rotate, zoom, reset and text alternatives. The examination view is quiet: task, remaining time where applicable, verification status and save status. The examiner view places submission details beside an evidence timeline and persistent decision controls. Selection, warnings and scores always have text cues in addition to colour.

Use short functional transitions, optional haptics and reduced-motion support. Avoid decorative glass over instructions, automatic scene movement during reading, flashing effects, four-point sparkle/starburst decorations and northeast arrow icons. Protect keyboard navigation, focus, zoom, screen-reader meaning and useful alternatives to dragging. The design atlas patterns selected are stable workspace navigation, straightforward account entry and responsive list/detail review; no external component design is copied.

**21. Performance and cost controls**

Keep account entry, text lessons and review tools usable without loading a 3D engine. Fetch only the current module and selected language. Reuse geometry and textures, provide quality levels, cap expensive effects and stop rendering inactive scenes. Own camera and GPU resources explicitly so repeated entry and exit does not leak them. Demand rendering is suitable for resting web models. [React Three Fiber performance guidance](https://r3f.docs.pmnd.rs/advanced/scaling-performance)

Initial engineering targets are p95 ordinary API responses below 500 ms at the agreed load, useful non-3D content within 2.5 seconds under the chosen test conditions, and sustained 30 fps for the minimum supported spatial-training device. These are targets to benchmark, not current results or national-scale guarantees. Define representative devices, networks, concurrent attempts and test duration before accepting them.

Use indexed institution/cohort/attempt queries, keyset pagination for evidence, thumbnails for review and background report generation. Public assets may be cached globally; private responses and evidence have separate cache rules. Queue jobs per institution fairly, cap upload sizes and rates, and avoid one tenant exhausting capacity for others.

Estimate evidence cost from actual usage. For illustration, 15 images at 100 KB each are approximately 1.5 MB per attempt; 10,000 attempts produce approximately 15 GB of originals before thumbnails, backups and overhead. Provider checks, email/SMS, rendering assets and examiner effort are additional costs. Short retention and occasional snapshots bound costs; actual image quality requirements determine compression.

**22. Deployment and operational recovery**

Use separate development, staging and production environments. Deploy a web process, an API process and a worker against managed PostgreSQL and private object storage. The processes can begin on a modest container deployment; each can scale independently when load tests identify the need. Choose an Indian hosting region for the initial service where the selected providers support the required functions. No biometric payload is routed to another region merely because an SDK defaults there; provider placement is an explicit configuration and contractual decision.

Store credentials and signing keys in approved secret management. Use scoped workload identities, TLS, expiring sessions, rate limits, upload validation, MFA for privileged staff and audited administrative actions. Rotate credentials and support issuer-key rotation while keeping old public keys available for historical verification. Isolate third-party provider failure from core learning.

Backups include database recovery and protected object metadata. Establish and test recovery targets before launch; initial proposed targets are a 15-minute database recovery-point objective and a four-hour recovery-time objective. Raw-image lifecycle, backup expiry and certificate verification continuity must agree with the retention policy. Monitor queue lag, missing evidence, failed uploads, provider errors, review delays and credential issuance failures without logging sensitive payloads.

CI verifies contract compatibility, security-sensitive invariants, migrations, dependencies and asset licences. Releases use staged rollout, reversible application changes and expand/contract database migrations. Content can be rolled back separately from application code. A deployment is not considered successful until real primary workflows and restoration procedures have been exercised.

**23. Revenue and institutional packaging**

Keep an accessible personal learning tier. Institutional plans can charge for active learners, programme administration, examiner workflows, certificate issuance capacity, reviewed specialist content and support. Face-assisted checks have transparent usage allowances or a separate metered charge. Enterprise options include SSO, custom domains, integrations and negotiated hosting arrangements.

Sponsored school or community cohorts and commissioned equipment-specific modules provide additional commercial paths. Prices remain a validation decision based on actual delivery cost and customer interviews. Personal data is not a revenue product. Essential safety corrections, access to appeals and accurate verification status are not optional paid safeguards.

Entitlements control purchased capabilities while permissions control who may use them. Payment webhooks are authenticated, retry-safe and server-authoritative. Renewal, cancellation, grace periods and export are clear. A billing error must not alter an examination score or erase an issued credential.

**24. Existing project migration**

The workspace already contains native Android learning, AR, local progress, institutional records, React-based staff tools, Better Auth and Drizzle. Preserve those assets and verified behaviours. The current tree contains unrelated ongoing work; this architecture does not authorise resetting it or rewriting current releases.

Introduce the new scenario, attempt and policy contracts first. Add institute provisioning and independent issuance as bounded modules. Move shared persistence to PostgreSQL through versioned migrations and reconciliation checks. Keep compatibility adapters for existing mobile records and distinguish imported legacy evidence from newly verified attempts. Replace web runtime or UI components only through tested routes; a stable Next.js production target is not a claim that the current vinext-based application has already migrated.

Validate the new AR renderer behind the existing app-owned runtime contract. Preserve local records, account ownership, historical certificate signatures and access boundaries. New proctoring policies apply prospectively and cannot manufacture missing evidence for old assessments.

**25. Delivery sequence and release acceptance**

Implementation proceeds through complete journeys while keeping all audiences in scope:

1. Establish shared contracts, account contexts, institute onboarding, role enforcement and configuration.
2. Deliver the common learning foundation, open exploration and audience-specific assignments on app and web.
3. Deliver approved identity enrolment, institutional session selfies, disclosed snapshot capture, examiner review and independent issuance.
4. Add optional face matching and liveness after provider, camera and accuracy validation; retain manual and supervised routes.
5. Complete content review, device testing, accessibility, recovery exercises and a pilot spanning household, campus, workplace and community contexts.

| Acceptance check | Required evidence before release |
|---|---|
| Personal entry | A new learner can learn and save progress without an examiner, selfie or institutional membership |
| Institutional autonomy | An onboarded institute can enrol, assign, review and issue without owner intervention |
| Access boundaries | Attempts to read another learner, cohort or institution through APIs, exports, storage and deep links fail |
| Reference identity | An unapproved or replaced reference cannot silently satisfy the enrolment requirement |
| Capture scope | No proctoring image is captured outside a disclosed active verification or assessment flow |
| Camera recovery | Real supported phones survive front/rear transitions, tracking loss and interruptions without losing work |
| Evidence integrity | Replays, duplicate uploads, missing sequences and timestamp uncertainty are represented correctly |
| Face reliability | False matches, false rejections, poor lighting, accessibility needs and presentation attacks are evaluated |
| Examiner decision | A false flag can be corrected with reasons and appealed; automation never labels a person a cheater |
| Certification | Required practical evidence cannot be bypassed; duplicate issuance is prevented; revocation and key rotation work |
| Offline behaviour | Queued work, stale permissions, expired manifests and reconnect conflicts resolve without false success |
| Data lifecycle | Originals, thumbnails, templates and backups follow declared retention and deletion behaviour |
| Accessibility | Primary journeys work with keyboard, screen readers, large text, reduced motion and accessible alternatives |
| Content quality | Domain reviewers approve instructions; language reviewers approve each released translation |
| Operations | Load, cost, backup restoration and rollback are measured against the agreed targets |

A hackathon demonstration should show one household learner completing practice and one institution independently enrolling a learner, assigning a scenario, reviewing disclosed evidence and issuing a QR-verifiable certificate. A simulated identity flag must be labelled as demo data. It must not be presented as a tested recognition model or a real misconduct incident.

**26. Distinctive value and judge questions**

SurakshaAR combines contextual practice with institutional ownership of assessment and certification. Its distinguishing design is the connection between a hazard, the mechanism behind it, the learner's recorded decision and the evidence supporting the resulting qualification. This is a product position to validate, not a claim that no competing system has similar features.

| Likely question | Defensible answer |
|---|---|
| Why AR instead of video? | Use it for spatial relationships, equipment interaction and contextual decisions; compare practical performance against an equivalent conventional lesson |
| How can this serve everyone? | Reuse competencies and assets, adapt context and language, and provide accessible non-AR routes |
| Does a selfie stop cheating? | It supports identity continuity; it does not prove independent thought or exclude off-camera assistance |
| Who certifies the learner? | The authorised institution, under a disclosed rubric and evidence method |
| Is the certificate government approved? | Only if a separately established authorised scheme applies; the platform itself makes no blanket claim |
| What happens when the model is wrong? | Retry, examiner review, supervised alternatives and appeal; no automatic disciplinary decision |
| What about inexpensive phones? | Downloaded lessons, quality levels and non-AR learning; required assessment capabilities are checked before the attempt |
| Can institutions customise everything? | Programmes and policies are configurable inside enforced safety, privacy and access boundaries |
| How is impact established? | Compare critical mistakes, decision quality, practical observation and delayed retention, with instructor workload and verification errors as guardrails |
| How does it make money? | Institutional administration, reviewed specialist content, support and transparent verification usage; prices follow cost and demand validation |

The remaining implementation gates are qualified content reviewers, representative test devices, acceptable biometric-provider terms and region, local verification benchmarks, institutional agreements, and field validation of the learning benefit. The architecture supports delivery without waiting for automated face verification: examiner-reviewed snapshots and supervised observation are complete operating modes.
