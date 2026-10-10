# The Development Rulebook

**Practical standards for useful, accessible, trustworthy apps and websites**  
Version 2.0 · Expanded design research 18 September 2026 · Owner: Akash Yadav · Review before major releases

This is the default handbook for our development work: discovery, design, implementation, verification, release, and maintenance. Apply the chapters relevant to the actual task. A small content edit does not require an authentication redesign; a prototype must identify its production gaps.

There is no finite collection of “all rules” that guarantees a successful product. This book provides a broad, actionable baseline, links to deeper standards, and a process for making exceptions. Sector-specific products, children’s services, medical or financial features, and regulated markets need additional requirements.

## 01 · How to apply the rules

| Label | Meaning | How to use it |
|---|---|---|
| **MUST** | Our baseline requirement when the feature is in scope | Verify before calling the feature production-ready. Explain unmet requirements. |
| **SHOULD** | Recommended default | Depart when user needs, evidence, or project constraints justify it; record significant tradeoffs. |
| **MAY** | Optional technique | Adopt only when it solves an actual problem. |
| **Standard** | A requirement from a named standard | Preserve its scope, level, and exceptions. |
| **Practice** | Practitioner or official implementation guidance | Adapt to the stack and threat model. |
| **Heuristic** | A useful design hypothesis | Test with the intended audience; do not promise conversion gains. |
| **House default** | Our chosen starting point | A preference, not a universal scientific or legal rule. |

**GOV-01 — MUST:** Establish the product’s audience, primary task, platforms, data sensitivity, markets, and operational owner. For existing projects, inspect instructions, architecture, and the current design system first.

**GOV-02 — MUST:** Apply rules proportionately to changed behavior. Respect explicit user requirements, applicable project instructions, and authorization boundaries. Identify conflicts instead of silently weakening protections or expanding scope.

**GOV-03 — SHOULD:** Prefer these baselines: WCAG 2.2 AA for web accessibility; OWASP ASVS 5.0.0 as a web security verification catalogue; MASVS for native mobile security; NIST SP 800-63B-4 for password and authenticator guidance. These references have different purposes and do not automatically establish legal compliance. [S01], [S16], [S17], [S18]

**GOV-04 — MUST:** Distinguish tested, partially tested, not tested, and not applicable. A polished screen, passing scanner, or successful build alone does not prove product readiness.

**GOV-05 — SHOULD:** Record material exceptions with the rule ID, scope, reason, compensating protection, owner, review date, and evidence. Never turn a documented exception into a claim of standards compliance.

## 02 · Product discovery and the attraction principle

Attraction means helping the right person recognize useful value and trust the next step. There is no single established “attraction rule” that dictates a layout or color.

**PRO-01 — MUST:** Express the primary user need as a concrete outcome: “A returning customer can reorder a previous purchase,” rather than “Build an engaging dashboard.” Define success before choosing components.

**PRO-02 — SHOULD:** Research representative users, including people using assistive technology, lower-end devices, slow connections, and the languages we plan to support. Validate assumptions with observed tasks and interviews.

**PRO-03 — SHOULD:** Make the first screen explain who the product helps, what it does, why it is credible, and the next useful action. Use a specific headline and genuine product evidence.

**PRO-04 — SHOULD:** Give each task area a clear primary action. Secondary actions can coexist; avoid equal visual emphasis for competing actions. Button text should describe the result: “Create invoice,” “Save address,” or “Start free trial.”

**PRO-05 — MUST:** Make prices, recurring charges, eligibility, limitations, and consequential commitments understandable before commitment. Testimonials, ratings, customer counts, security badges, and scarcity claims must be authentic. [S12]

**PRO-06 — SHOULD:** Prefer relevant screenshots, examples, and useful content over decorative motion. Offer a demo or sample when it helps explain an unfamiliar product.

**PRO-07 — SHOULD:** Evaluate attraction using qualified task starts and downstream success, alongside comprehension and trust. Raw clicks are insufficient; misleading promises can increase clicks while damaging retention.

**PRO-08 — MAY:** Use a short comprehension test on the opening screen. Treat the chosen time limit as a research method, not evidence of a universal human attention span.

## 03 · Interaction principles and useful UX heuristics

These principles guide decisions; their simplified names are not automatic design prescriptions.

| Principle | Development rule | Common misuse to avoid |
|---|---|---|
| Visibility of status | Show saving, success, failure, and current location. | Endless unexplained spinners. |
| Familiar language | Name actions and objects as users understand them. | Database or internal team terminology in product copy. |
| User control | Support cancel, back, and undo where feasible. | Trapping users in flows. |
| Consistency | Similar appearance should mean similar behavior. | Identical buttons with surprising outcomes. |
| Error prevention | Constrain invalid actions and clarify consequences. | Asking for confirmation on every harmless click. |
| Recognition over recall | Keep relevant choices and context available. | Making users memorize IDs between screens. |
| Efficient expert use | Offer shortcuts and bulk actions when useful. | Requiring every user to learn shortcuts. |
| Minimalism | Prioritize information necessary for the task. | Hiding essential information to make a sparse screen. |
| Recovery | Explain the problem and a workable next step. | Blame, opaque codes, or a dead end. |
| Contextual help | Place concise help near the difficult decision. | Treating a long tutorial as a substitute for usability. |

The ten rows above adapt Nielsen’s usability heuristics. [S02]

| Additional principle | Practical interpretation | Evidence boundary |
|---|---|---|
| Aesthetic–usability effect | Use coherent typography, alignment, and spacing; test task performance separately. | Attractive interfaces may be perceived as easier; appearance does not fix failure. [S03] |
| Fitts’s law | Make frequent targets easy to reach and select; separate destructive controls. | There is no single ideal button size for all devices. [S04] |
| Hick–Hyman law | Organize complex choices with categories, search, or sensible defaults. | Do not arbitrarily delete useful options or prescribe a universal menu count. [S05] |
| Gestalt proximity | Keep labels, inputs, errors, and related information together. | Responsive wrapping can destroy an apparent relationship. [S06] |
| Gestalt similarity | Reuse styles to express a shared role. | Similar decorative and interactive objects cause ambiguity. [S07] |
| Common region | Use boundaries or backgrounds to group related content. | Putting everything in nested cards adds clutter. [S08] |
| Progressive disclosure | Show frequent actions first; reveal advanced options when needed. | Never hide price, consent, or critical warnings. [S09] |
| Response-time guidance | Acknowledge actions promptly and explain longer waits. | The familiar 0.1/1/10-second guidance is approximate, not a service-level guarantee. [S10] |

**UX-01 — SHOULD:** Prefer established platform conventions for navigation, selection, and editing. Innovate where the product benefits justify relearning.

**UX-02 — SHOULD:** Chunk related information and preserve context. Neither “seven menu items,” “three clicks,” nor “one screen only” is our universal rule.

**UX-03 — SHOULD:** Make interactive elements recognizable without hover. Use real links for navigation and buttons for actions; avoid clickable generic containers when a semantic element fits.

**UX-04 — MUST:** Ensure high-impact actions have clear consequences and an appropriate opportunity to review, confirm, or reverse. Scale friction to impact.

## 04 · Information architecture, navigation, and search

**NAV-01 — SHOULD:** Organize navigation around user tasks and recognizable categories. Validate labels and grouping with representative users.

**NAV-02 — MUST:** Show the current location and selection. Keep navigation, help placement, and action names consistent across the experience.

**NAV-03 — SHOULD:** Preserve browser back/forward behavior, meaningful URLs, and deep links. Restore filters, scroll position, and draft context when users return.

**NAV-04 — MUST:** Route invalid or unavailable destinations to a useful explanation with recovery options. Distinguish no results, no permission, removed content, and server failure where disclosure is safe.

**NAV-05 — SHOULD:** Provide search when content volume or task needs justify it. Keep the query visible; support clearing, useful filters, understandable sorting, result counts, and a recoverable no-results state.

**NAV-06 — MUST:** Enforce permissions on search results, snippets, suggestions, counts, exports, and direct URLs. Hiding a navigation item does not restrict access. [S22]

**NAV-07 — SHOULD:** Prefer pagination or an explicit “Load more” where people need position, comparison, a reachable footer, or shareable results. Infinite scroll is a contextual choice.

## 05 · Visual hierarchy, typography, and spacing

**VIS-01 — SHOULD:** Use a documented scale for text, spacing, corners, elevation, and icons. Extend the existing design system before introducing competing conventions.

**VIS-02 — SHOULD — House defaults:** Begin with readable body text around 16–18 CSS px, line height around 1.5, and prose widths around 45–75 characters. Choose a small family of type sizes and a 4/8-unit spacing scale. Adjust for script, font, density, audience, and platform; these are not WCAG minimums.

**VIS-03 — MUST:** Keep text selectable and adaptable. Avoid rendering ordinary interface text as images. Let headings, labels, and buttons wrap where necessary.

**VIS-04 — SHOULD:** Use size, weight, placement, and spacing to establish hierarchy before adding more colors. Keep secondary copy readable rather than merely pale.

**VIS-05 — MUST:** Test truncation against real content: long names, large values, errors, localization, and empty content. Make essential truncated content accessible without a mouse.

**VIS-06 — SHOULD:** Pair unfamiliar icons with labels. Use a consistent icon family and meaningful alternative text for informative images; decorative images should not add screen-reader noise.

**VIS-07 — MUST:** Support user text-spacing overrides without lost content or functionality: line height 1.5 times font size, paragraph spacing 2 times, letter spacing 0.12 times, and word spacing 0.16 times, with the criterion’s language/script qualifications. These are override test values, not compulsory visual defaults. [S35]

## 06 · Color theory and semantic color

**COL-01 — SHOULD:** Choose a palette for the brand, audience, content, and environment. Hue identifies a color family; saturation expresses intensity; lightness affects appearance. None alone guarantees readable contrast.

**COL-02 — MAY:** Use monochromatic colors for cohesion, analogous hues for closely related visual groups, complementary hues for distinction, or a restrained triadic palette for variety. Harmony is a starting tool, not an accessibility test. [S13]

**COL-03 — SHOULD:** Use mostly neutral surfaces, a purposeful accent, and distinct status colors. The “60–30–10 rule” is an optional composition heuristic, not a literal pixel quota or scientific conversion formula.

**COL-04 — MUST:** Name colors by role: `surface`, `text`, `text-muted`, `border-control`, `action`, `on-action`, `focus`, `danger`, `warning`, `success`, and `info`. Components consume roles rather than scattered hard-coded hex values. Define paired foreground/background roles. [S14]

**COL-05 — MUST:** Meet WCAG AA text contrast: at least **4.5:1** for ordinary text and **3:1** for large text. Large means at least 18 pt (24 CSS px), or 14 pt (about 18.67 CSS px) bold. Preserve the standard’s exceptions for logotypes, incidental/decorative content, and inactive controls. Do not round a failing ratio upward. [S31]

**COL-06 — MUST:** Essential component boundaries, state indicators, and meaningful graphics must meet applicable **3:1** non-text contrast against adjacent colors. Decorative dividers do not automatically require this ratio. [S34]

**COL-07 — MUST:** Never communicate an error, selection, chart series, or availability through color alone. Add text, icons, shapes, patterns, or other cues. Links within prose should normally be underlined. [S01]

**COL-08 — SHOULD:** Test color-vision differences and forced-colors/high-contrast modes. Do not remove system focus treatment without an effective replacement. Check gradients, overlays, transparency, and text over photography using the actual rendered background.

**COL-09 — SHOULD:** Validate cultural and domain meanings. “Blue creates trust” and “red always increases conversion” are hypotheses, not rules we can guarantee.

### Starter palette: a house example, not a mandatory brand

These opaque sRGB values are a reusable starting point. Test every actual pairing and state; mixing arbitrary rows is not guaranteed to pass.

| Semantic role | Light | Dark |
|---|---|---|
| Canvas | `#FFFFFF` | `#0F172A` |
| Raised surface | `#F8FAFC` | `#1E293B` |
| Primary text | `#0F172A` | `#F8FAFC` |
| Secondary text | `#475569` | `#CBD5E1` |
| Link / action | `#1D4ED8` | `#93C5FD` |
| Text on solid action | `#FFFFFF` | `#0F172A` |
| Essential control border | `#64748B` | `#94A3B8` |
| Focus indicator on neutral surfaces | `#1D4ED8` | `#93C5FD` |
| Danger text | `#B91C1C` | `#FCA5A5` |
| Success text | `#166534` | `#86EFAC` |
| Warning text | `#92400E` | `#FDE68A` |

The accompanying `contrast-report.json` records computed ratios for text/status/link colors on both neutral surfaces, button label pairs, and control borders/focus colors against those surfaces. It does not validate hover states, disabled controls, transparency, or focus rings against a same-colored button. Use a contrasting offset or dual ring when necessary.

## 07 · Light and dark themes

**THM-01 — SHOULD:** When both themes are in scope, offer **System / Light / Dark**. Default to system preference and persist explicit user choice. A fixed-theme brand site may justify one theme; do not silently add unnecessary scope.

**THM-02 — MUST:** Implement each theme with semantic tokens and explicit foreground/background pairs. Do not invert the entire page or assume one palette passes in both modes. [S14]

**THM-03 — MUST:** On the web, align `color-scheme` with supported/effective themes and use `prefers-color-scheme` for system mode. Update system-mode rendering when the OS preference changes. Native apps should use equivalent platform theme APIs. [S36], [S37]

**THM-04 — SHOULD:** Resolve the effective theme before first paint when practical; handle server rendering, stored-preference failures, and hydration consistently. A saved preference should not cause a bright flash on every navigation.

**THM-05 — MUST:** Check all component states in both themes: focus, hover, selection, disabled, validation, loading, charts, code blocks, menus, dialogs, tooltips, and third-party widgets.

**THM-06 — SHOULD:** Give dark surfaces distinguishable layers using tone and borders. Shadows alone may not separate them. Pure black and pure white are permissible when appropriate; avoid rigid bans and test comfort and contrast.

**THM-07 — MUST:** Keep image, logo, illustration, and chart content legible. Supply alternate assets where needed; do not recolor informative images indiscriminately.

**THM-08 — SHOULD:** Respect forced colors and reduced motion independently of theme. Dark mode is a preference, not proof of accessibility or lower power use on every display.

## 08 · Responsive web, mobile, tablet, and desktop

**RSP-01 — MUST:** Support the intended screen sizes, orientation, input methods, and browser/platform versions. Use a viewport configuration that preserves user zoom.

**RSP-02 — SHOULD:** Build flexible layouts using intrinsic sizing, grids, wrapping, and content-led breakpoints. Component/container queries may be appropriate. Do not identify layout solely from device names or user-agent strings. [S38]

**RSP-03 — MUST:** For ordinary vertically scrolling web content, verify reflow at **320 CSS px** without loss of content/function or two-dimensional scrolling. The standard allows exceptions for content genuinely requiring a two-dimensional layout, such as some tables and maps. A 1280-wide viewport at 400% zoom is one relevant test. [S32]

**RSP-04 — MUST:** Keep focused fields, validation, and primary actions reachable with the software keyboard open. Check safe areas, browser bars, sticky elements, landscape, and short windows.

**RSP-05 — MUST:** Follow applicable target-size requirements. WCAG 2.2 AA specifies 24×24 CSS px with defined exceptions, including spacing. Our touch-friendly web default is 44×44 CSS px or larger where feasible. Apple’s guidance uses 44×44 points; Android recommends 48×48 dp. These units are not interchangeable. [S33], [S39], [S40]

**RSP-06 — MUST:** Provide alternatives for essential hover-only, drag-only, swipe-only, or multi-touch actions. Support keyboard operation and meaningful pointer/touch behavior.

**RSP-07 — SHOULD:** Adapt information priority: a desktop sidebar might become a drawer; a dense comparison table may need labeled horizontal scrolling or a task-specific mobile presentation. Do not simply shrink the desktop layout.

**RSP-08 — SHOULD:** For desktop apps, support window resizing, keyboard shortcuts, focus order, standard menus, and pointer interaction. For mobile apps, support platform back behavior, text scaling, permission states, and interruptions.

**RSP-09 — MUST:** Test widths between breakpoints and real devices when available. A useful house sample is 320, 360/390, 768, 1024, 1440, and 1920 CSS px; select actual coverage from the audience, not this list alone.

## 09 · Accessibility as a release requirement

**A11Y-01 — MUST:** For web work, evaluate the applicable WCAG 2.2 A and AA criteria across complete user processes, not only selected screenshots. Native apps also need platform-specific assistive-technology checks. The checklist here is not the full conformance standard. [S01]

**A11Y-02 — MUST:** Prefer native semantic controls, logical headings, landmarks, meaningful page titles, an appropriate document language, and a way to bypass repeated navigation.

**A11Y-03 — MUST:** Test every primary task using only a keyboard. Keep focus visible and not entirely hidden by authored sticky content; avoid traps and unexpected focus movement. Our stronger default is to keep the whole focused control visible.

**A11Y-04 — MUST:** Associate labels, descriptions, errors, and state with controls programmatically. Announce important asynchronous status without unnecessarily interrupting the screen reader. [S41]

**A11Y-05 — MUST:** Accessible dialogs need an accessible name, appropriate initial focus, contained focus while modal, a clear close path, and sensible focus restoration. Follow the platform/APG pattern, including Escape behavior where appropriate. [S42]

**A11Y-06 — MUST:** Supply appropriate text alternatives, captions, transcripts, and audio description for media according to the applicable criteria. Provide nonvisual access to meaningful chart data.

**A11Y-07 — MUST:** Support 200% text resizing, reflow, text-spacing overrides, and essential tasks without sensory-only instructions. Do not assume “click the green circle” identifies an action for everyone. [S01], [S32], [S35]

**A11Y-08 — SHOULD:** Respect reduced motion, avoid unnecessary auto-play, and offer pause/stop controls where required. Avoid content that violates flash thresholds. Animation should explain change rather than obstruct reading.

**A11Y-09 — MUST:** Combine automated checks with manual keyboard and screen-reader testing. Test errors, authentication, dialogs, navigation, and dynamic updates; include disabled users in research where feasible.

## 10 · Forms and data entry

**FRM-01 — SHOULD:** Ask only for data necessary at this point. Use progressive profiling; make optional fields visibly optional and explain unusual requests.

**FRM-02 — MUST:** Use persistent labels. Placeholders may illustrate an example but cannot replace the label. Group related choices with appropriate semantics. [S41]

**FRM-03 — SHOULD:** Use sensible input types, input modes, autofill tokens, defaults, and formatting. Accept realistic names, international numbers, pasted content, and local address variations.

**FRM-04 — MUST:** Validate on the server. Client validation improves feedback but is not a trust boundary. Explain expected formats before submission when they are genuinely constrained.

**FRM-05 — SHOULD:** Validate at a useful time, such as blur or submit, rather than scolding users during incomplete entry. Show errors next to fields and provide an error summary for complex forms.

**FRM-06 — MUST:** Preserve valid entries after errors. Identify what failed and how to fix it. Do not expose internal exceptions or sensitive details in public error messages.

**FRM-07 — SHOULD:** Use a single column for ordinary forms, clear steps for long tasks, optional save/resume when needed, and review before consequential submission.

**FRM-08 — MUST:** Prevent accidental duplicate submissions and make pending/success states explicit. Use server-side idempotency where repeating a request could charge, duplicate, or corrupt data.

**FRM-09 — SHOULD:** Explain why an action is unavailable. Avoid silent disabled submission buttons that give no discoverable way to correct the form.

**FRM-10 — MUST:** Reuse or let users select previously entered information in the same process where WCAG requires it, accounting for its security, essential-use, and invalid-information exceptions. [S01]

## 11 · Login, signup, and password UX

These rules apply when accounts are needed. A public brochure site does not need signup. A passkey-only product needs an appropriate recovery method rather than a nonfunctional password-reset link.

**AUTH-01 — SHOULD:** Allow browsing or guest use where an account is unnecessary. Ask users to register when saving, purchasing, collaborating, or another actual requirement makes the account useful.

**AUTH-02 — MUST:** For password login, provide visibly labeled identifier and password fields, a working **Show / Hide password** button, a clear submit action, and a discoverable **Forgot password?** route.

**AUTH-03 — MUST:** The visibility toggle must be keyboard accessible, have a meaningful accessible label/state, avoid submitting the form (`type="button"` on web), and preserve the value and focus. Start masked. Disable spellcheck and autocapitalization on password input. [S15]

**AUTH-04 — MUST:** Support password managers, paste, and autofill. Use `autocomplete="username"`, `current-password`, or `new-password` appropriately. Keep input identifiers stable. Do not require manual memorization/transcription where an accessible mechanism or alternative is needed. [S15], [S30]

**AUTH-05 — SHOULD:** Avoid redundant password-confirmation fields when a reveal control and appropriate recovery address the need. If confirmation is retained for a justified flow, support paste and autofill there too.

**AUTH-06 — MUST:** Show login pending, failure, offline, rate-limited, and successful states. Public failures must avoid disclosing whether an account exists, is disabled, or has a wrong password; consider timing as well as wording. [S19]

**AUTH-07 — SHOULD:** Offer appropriate passkey, enterprise SSO, or established identity-provider support when it helps the audience. Use maintained identity libraries/services; do not invent an authentication protocol.

**AUTH-08 — MUST:** Keep signup, login, recovery, verification, MFA, cancellation, and logout routes connected. Social/provider login must handle denial, provider failure, and account-linking conflicts safely.

**AUTH-09 — MUST:** Restore the intended destination after authentication using a validated internal return path. Never accept an arbitrary untrusted redirect URL.

**AUTH-10 — SHOULD:** Explain “Remember me” or persistent login clearly, especially on shared devices. Keep marketing consent separate from account creation.

**AUTH-11 — MUST:** Test password-manager behavior, Enter-to-submit, reveal toggling, autofill, keyboard focus, small screens, long credentials, and failure recovery.

## 12 · Password policy, MFA, and recovery

### Password policy

**PWD-01 — SHOULD — NIST baseline:** Require at least **15 characters** for a password used as the sole factor. NIST permits **8 or more** only when it is always part of MFA; merely offering optional MFA does not qualify. Accept a maximum of at least 64 characters. [S18]

**PWD-02 — MUST:** Permit spaces and password-manager-generated values; verify the whole password without silent truncation. Define Unicode handling consistently. Reject commonly used, expected, or compromised values. Avoid composition puzzles and routine forced expiry; require a change when compromise is indicated. Do not use security questions. [S18]

**PWD-03 — MUST:** Store passwords with a maintained, salted password-hashing implementation. Prefer Argon2id where supported; OWASP’s cited baseline includes 19 MiB, two iterations, parallelism one. Benchmark and recheck current guidance; select alternatives for platform/compliance constraints. Plaintext, reversible storage, and fast general-purpose hashes are unsuitable. [S20]

### Recovery

**REC-01 — MUST:** Password recovery must return an account-neutral response; throttle requests without letting attackers trivially lock out a victim. Use comparable response timing. [S21]

**REC-02 — MUST:** Issue cryptographically random, sufficiently strong, single-use, expiring recovery tokens. Store them securely, preferably as a hash for random lookup tokens. Bind them to the intended account and purpose. [S21]

**REC-03 — MUST:** Construct reset links from trusted configuration over HTTPS. Prevent token leakage through logs, analytics, third-party content, and referrers. Consume tokens atomically so concurrent requests cannot reuse them. [S21]

**REC-04 — MUST:** After reset, notify the user without including the password; invalidate sessions according to risk and offer appropriate session control. Return to normal login rather than automatically authenticating solely because a reset finished. Password recovery must not bypass MFA. [S21], [S19]

**REC-05 — MUST:** Test expired, reused, malformed, replaced, wrong-account, and concurrently used tokens. Make recovery messages usable, with resend guidance and help when mail fails.

### MFA and passkeys

**MFA-01 — MUST — House baseline:** Require appropriate MFA for production administrative/privileged accounts. Prefer phishing-resistant authenticators where feasible; select assurance requirements for the actual threat model.

**MFA-02 — MUST:** Make enrollment, factor changes, recovery, and removal as carefully protected as login. Require suitable reauthentication for sensitive changes; do not let support or an email link casually bypass stronger factors. [S19]

**MFA-03 — SHOULD:** Provide securely stored, single-use recovery codes or another suitable recovery route. Explain recovery before a device is lost. OTPs must expire, be rate-limited, and resist replay; support paste/autofill for entry.

**MFA-04 — MUST:** With passkeys, validate challenges, origin, relying-party identity, and relevant user-verification requirements through a maintained WebAuthn implementation. Address lost devices and multiple authenticators. Do not claim email or SMS OTP is phishing-resistant. [S18]

## 13 · Sessions, SSO, and account lifecycle

**SES-01 — SHOULD:** Prefer established server-managed sessions or a suitable backend-for-frontend for browser apps. Choose token architecture deliberately; avoid putting long-lived bearer credentials in JavaScript-readable storage by default. [S23]

**SES-02 — MUST:** For cookie sessions, use HTTPS, `Secure`, `HttpOnly`, narrow scope, and an appropriate `SameSite` policy. Rotate session IDs on login/privilege changes; enforce idle and absolute expiry server-side. `HttpOnly` does not make XSS harmless. [S23]

**SES-03 — MUST:** Make logout revoke the relevant server-side session/refresh credential and clear sensitive client state. Test logout, back navigation, expiry, revocation, and switching between accounts. [S23]

**SES-04 — MUST:** Require appropriate reauthentication before changing recovery email, password, MFA, or other sensitive account attributes. Provide session/device management where warranted and notify meaningful security changes. [S19]

**SES-05 — MUST:** Use maintained OAuth/OIDC integrations. Prefer authorization code flow with PKCE; validate redirects and transaction binding, and verify token signature, issuer, audience, and expiry. Use state/nonce as required by the flow. Never put a confidential client secret in a browser or mobile binary. [S24], [S29]

**SES-06 — MUST:** Link identities only with adequate proof of control. Matching an unverified email string is insufficient. Account merging must preserve authorization and prevent takeover.

**SES-07 — MUST:** Define what suspension, cancellation, deletion, team removal, and email changes do to access, shared data, billing, and integrations. Provide accurate user-visible outcomes.

## 14 · Application and API security

Use a small threat model for the feature: what is valuable, who may act, what input is untrusted, which boundaries exist, and how abuse would happen. Use ASVS as a verification catalogue, not a claim that a checklist makes software secure. [S16]

| ID | Requirement | Concrete verification |
|---|---|---|
| SEC-01 | **MUST:** Deny by default and authorize every server-side operation, object, property, and tenant. | A different user/tenant cannot read, edit, export, or enumerate protected data by changing identifiers. [S22] |
| SEC-02 | **MUST:** Validate input schemas, sizes, allowed fields, and business invariants on the server. | Extra fields cannot set roles, prices, ownership, or protected status. |
| SEC-03 | **MUST:** Parameterize database queries; allowlist dynamic identifiers that cannot be parameterized. | Untrusted input cannot change query structure. [S25] |
| SEC-04 | **MUST:** Use context-appropriate output encoding and safe DOM APIs; sanitize intentionally accepted rich HTML. | Stored/reflected content and unsafe URL schemes cannot execute as code. CSP is additional protection. [S26] |
| SEC-05 | **MUST:** Protect state-changing cookie-authenticated requests from CSRF using the framework’s appropriate controls. | Cross-origin form/request attempts fail; GET does not mutate important state. SameSite alone is not a universal solution. [S27] |
| SEC-06 | **MUST:** Treat CORS as a browser sharing policy, not authorization. | Allow only required origins; credentialed access and preflight behavior match the design. [S29] |
| SEC-07 | **MUST:** Validate file type/content, size, filename handling, and upload authorization; isolate untrusted files. | Malicious content cannot execute from a trusted origin; private downloads remain protected. Scan/quarantine as appropriate. [S28] |
| SEC-08 | **MUST:** Constrain server-side URL fetching and network egress. | Block unauthorized internal/metadata destinations, including redirects and DNS tricks; prefer destination allowlists. [S43] |
| SEC-09 | **MUST:** Keep secrets in approved secret storage with least-privilege access, rotation, and revocation. | No secret appears in source, frontend bundles, logs, examples, or build artifacts. [S44] |
| SEC-10 | **MUST:** Rate-limit and bound expensive operations, authentication, exports, uploads, and external calls. | Limits protect users, shared tenants, infrastructure, and spend without creating easy account-lockout abuse. |
| SEC-11 | **MUST:** Use maintained TLS and cryptographic libraries; verify certificates and isolate encryption keys. | No production “skip verification” flags or homemade crypto. |
| SEC-12 | **MUST:** Minimize error detail exposed publicly; retain redacted diagnostics internally. | Exceptions do not reveal credentials, personal records, stack details, or queries. [S45] |
| SEC-13 | **SHOULD:** Set suitable CSP, anti-framing policy, MIME-sniffing prevention, referrer policy, and secure transport policy. | Test application compatibility; do not enable irreversible HSTS scope/preload without validating all affected hosts. |
| SEC-14 | **MUST:** Verify webhook authenticity and handle retries, replay, duplicates, and out-of-order delivery. | A forged or repeated payment notification cannot grant extra credit or duplicate an action. [S46] |
| SEC-15 | **MUST:** Test business logic abuse and concurrency. | Repeated coupons, concurrent purchases, stale permissions, and duplicate requests cannot violate invariants. |
| SEC-16 | **SHOULD:** Assess dependency provenance, supported versions, advisories, and transitive risks; retain lockfiles. | Scans and updates are triaged by reachability/impact, with owners for remediation. [S48] |

Do not expose administrative tools or development endpoints merely because their URLs are obscure. Apply access control to storage, queues, scheduled jobs, backups, and internal dashboards as well as API routes.

## 15 · Privacy, consent, and user control

**PRI-01 — MUST:** Inventory personal data, purpose, recipients, storage locations, retention, and deletion behavior. Collect the minimum useful data; avoid logging form contents or recording sensitive screens by default.

**PRI-02 — MUST:** Provide truthful notices and user controls appropriate to the actual processing. Where consent is required, make it specific, informed, affirmative, and withdrawable. Determine applicable law from users, markets, processing, and contracts. [S47]

**PRI-03 — SHOULD:** Make optional tracking and marketing easy to refuse. Do not use preselected paid extras, misleading consent controls, fabricated urgency, or obstructive cancellation. [S12]

**PRI-04 — MUST:** Implement export, correction, deletion, and retention requirements where applicable; verify the requester appropriately. Explain legal retention, shared records, and backup expiry accurately rather than promising instant erasure everywhere.

**PRI-05 — MUST:** Evaluate analytics, session replay, crash reporters, and SDKs for data collection and third-party transfer. Redact secrets and sensitive personal data before transmission. [S45]

**PRI-06 — SHOULD:** Restrict production data in development and support tooling. Prefer synthetic/redacted test data. Record who can access sensitive information and why.

**PRI-07 — MUST:** Recheck current jurisdiction-specific requirements before deployment. UK GDPR principles are one referenced example, not a universal legal checklist; verify India’s applicable DPDP requirements, EU/UK rules, US sector/state rules, and store policies only where relevant. [S47]

## 16 · Activation, retention, and customer trust

Retention means people return because the product continues to solve their problem. A successful infrequent service may correctly have low daily engagement.

**RET-01 — MUST:** Define the first meaningful success event and the recurring value event. Completing signup or opening a push notification is not necessarily receiving value.

**RET-02 — SHOULD:** Shorten the path to that first success with relevant templates, sample data, sensible defaults, and optional guidance. Avoid mandatory feature tours before useful work. [S50]

**RET-03 — SHOULD:** Preserve drafts, preferences, history, saved work, and useful context securely. Show the next relevant action rather than repeatedly resetting users to a generic starting point.

**RET-04 — SHOULD:** Explain empty states with what the area is for, how to start, and an example when helpful. Distinguish first use, filtered-out results, and data-loading failure.

**RET-05 — MUST:** Keep reminders and notifications relevant, permission-based where required, controllable by channel/frequency, and consistent with quiet hours. Respect unsubscribe, dismissal, and opt-out choices. Avoid sensitive lock-screen content by default.

**RET-06 — SHOULD:** Use progress, milestones, or streaks only when they support the user’s chosen objective. Provide humane recovery; do not penalize breaks or exploit anxiety to increase visits.

**RET-07 — SHOULD:** Provide accessible help, useful support escalation, clear incident messages, and feedback channels. Analyze repeated support problems as product defects, not just support workload.

**RET-08 — MUST:** Make cancellation and account deletion discoverable. Explain consequences and alternatives fairly; do not require a maze, hidden button, or irrelevant survey to leave. [S12]

**RET-09 — SHOULD:** Segment retention by acquisition source, first-use experience, platform, and user need. Investigate churn with task research and optional feedback rather than inventing a cause from analytics alone.

**RET-10 — MUST:** Do not optimize engagement by degrading user control, privacy, accessibility, honesty, or well-being. Evaluate retention changes against guardrail metrics.

## 17 · Measurement and responsible experiments

Use goals → observable signals → metrics. HEART offers useful categories: happiness, engagement, adoption, retention, and task success. Select the categories that match the product; every project does not need all of them. [S11]

| Measure | Operational definition to document |
|---|---|
| Activation | Eligible new users reaching the defined value event within a named time window / eligible new users. |
| Time to first value | Elapsed time from a defined start to that event; include incomplete attempts separately to avoid survivor bias. |
| Cohort retention | Members of a fixed starting cohort performing the return event in a defined later window / original eligible cohort. |
| Task success | Successful task attempts / eligible attempts, with success, abandonment, and retries defined. |
| Customer churn | Customers lost during the period / customers at its start; define cancellations, pauses, and reactivations. |
| Satisfaction | A clearly worded survey with sampling, response rate, and collection timing recorded. |
| Reliability guardrails | Error rate, failed payments, data loss, auth failures, performance, support contacts, opt-outs, and accessibility regressions. |

**MET-01 — MUST:** Define event names, eligibility, denominators, windows, time zone, identity resolution, exclusions, and consent behavior before comparing results. Distinguish exact-day, rolling, and bracketed retention.

**MET-02 — SHOULD:** Use D1/D7/D30 only when the product has a suitable cadence. Monthly invoicing and emergency help should not inherit a daily habit target.

**MET-03 — MUST:** For experiments, state a hypothesis, primary outcome, guardrails, assignment unit, sample plan, and stopping rule. Avoid treating repeated peeking, tiny samples, or many uncorrected comparisons as reliable wins.

**MET-04 — SHOULD:** Pair quantitative evidence with usability research. Report uncertainty and practical effect size; correlations and before/after changes do not by themselves establish causation.

**MET-05 — MUST:** Verify analytics with test events and privacy controls. Dashboards must distinguish unknown/missing values from zero and real users from test/bot traffic.

## 18 · Performance and perceived speed

**PERF-01 — SHOULD:** For websites, target Core Web Vitals at the **75th percentile**, segmented for mobile and desktop: **LCP ≤2.5 s**, **INP ≤200 ms**, **CLS ≤0.1**. Meet all three; these are field targets, not a promise that every request finishes in 200 ms. [S49]

**PERF-02 — MUST:** Separate lab and real-user measurements. A Lighthouse load test cannot establish field INP or whole-product accessibility. Record device/network conditions, route, measurement window, and sample limitations. [S49]

**PERF-03 — SHOULD:** Set explicit budgets for JavaScript, images, fonts, memory, network calls, and API latency on representative devices. Budgets are product decisions, not a universal bundle-size rule.

**PERF-04 — SHOULD:** Send appropriately sized responsive images, compress assets, limit third-party scripts, split/defer nonessential code, reserve media dimensions, and avoid needless font variants. Do not lazy-load the main above-the-fold image blindly.

**PERF-05 — MUST:** Keep interactions responsive during long work. Use appropriate loading feedback, cancellation/retry where feasible, and truthful progress. Never show a saved or paid state before it is confirmed unless an explicit optimistic state can recover safely.

**PERF-06 — SHOULD:** Cache deliberately with correct ownership, invalidation, and freshness. Never allow a shared cache or service worker to expose another user’s private data.

**PERF-07 — SHOULD:** Measure native startup, scrolling/jank, memory, battery, and background activity separately from web metrics. Test midrange/older supported hardware, not only the developer’s machine.

## 19 · Loading, empty, offline, and failure states

**STA-01 — MUST:** Specify applicable default, hover, focus, pressed, selected, disabled, loading, empty, error, success, and permission-denied states for each interactive component.

**STA-02 — MUST:** Distinguish “not loaded,” “nothing exists,” and “could not load.” A failed request must not silently display “You have no orders.”

**STA-03 — SHOULD:** Preserve the last valid data with clear freshness information when appropriate. Explain partial results and synchronization status.

**STA-04 — MUST:** Retry safely using bounded backoff and idempotency as appropriate. Do not repeatedly charge, send mail, or create records because a response was lost.

**STA-05 — SHOULD:** Provide offline reading or queued changes only where it benefits the use case. Define storage security, synchronization, conflict resolution, and the moment a write is actually committed.

**STA-06 — MUST:** Handle interruption, slow service, expired sessions, lost connectivity, permission changes, and stale data without silently discarding important user work.

## 20 · Content, internationalization, and localization

**CNT-01 — SHOULD:** Write concise, concrete product copy. Explain consequences and next steps; use consistent terminology across screens, support, and emails.

**CNT-02 — MUST:** Keep secrets, internal implementation details, and unnecessary jargon out of product flows. Technical detail belongs only where it helps the intended user decide or act.

**CNT-03 — MUST:** Use Unicode, appropriate language metadata, and explicit direction where needed. Separate translatable strings from code; support pluralization and context rather than concatenated sentence fragments. [S51]

**CNT-04 — SHOULD:** Test long translations, right-to-left layouts, non-Latin fonts, names without assumed first/last structure, and culturally appropriate imagery. Mirror directional UI thoughtfully; not every symbol reverses.

**CNT-05 — MUST:** Make currency, units, date/time, locale, and time-zone behavior unambiguous. Distinguish instants from dates and recurring local times; test daylight-saving transitions where relevant.

**CNT-06 — SHOULD:** Make search, sorting, input validation, and formatting appropriate to supported locales. Language detection should not prevent users from choosing another language.

## 21 · Public websites, discovery, and SEO

**SEO-01 — SHOULD:** For public indexable content, use descriptive titles, headings, crawlable links, useful text, and stable URLs. Ensure rendering choices expose the intended content to crawlers. [S52]

**SEO-02 — SHOULD:** Configure canonicals, redirects, sitemaps, indexability, and structured data when appropriate. Structured data must describe visible, truthful content; none of these guarantees ranking. [S52]

**SEO-03 — MUST:** Use authorization for private content. `robots.txt` and `noindex` are not security controls. Prevent accidental indexing of previews, staging, and sensitive routes using the appropriate combination of access restrictions and indexing controls.

**SEO-04 — SHOULD:** Provide useful social previews, favicon/app icons, meaningful 404/410 behavior, and working share links. Avoid manipulative keyword stuffing and intrusive interstitials.

**SEO-05 — MUST:** Keep factual claims, contact information, pricing, and support links accurate and maintainable. Assign content ownership where stale information can cause harm.

## 22 · Payments, subscriptions, and commerce

**PAY-01 — MUST:** Show total cost, currency, billing cadence, trial conversion, delivery constraints, and cancellation/refund terms before commitment. Never fabricate savings or scarcity. [S12]

**PAY-02 — SHOULD:** Prefer a reputable provider’s hosted/tokenized payment flow to reduce direct handling of card data. Determine actual PCI obligations for the integration; outsourcing does not automatically remove every responsibility.

**PAY-03 — MUST:** Calculate authoritative amounts, discounts, entitlements, and stock on the server. Never trust prices or “payment successful” flags supplied by the browser.

**PAY-04 — MUST:** Verify provider callbacks/webhooks, deduplicate, handle delivery out of order, and reconcile with provider state. Use idempotency for payment creation where supported. [S46]

**PAY-05 — MUST:** Handle pending, failed, canceled, refunded, partially refunded, and disputed states. Keep receipts, billing history, and support paths understandable.

**PAY-06 — MUST:** Test renewals, trial end, failed retries, cancellation timing, and concurrent checkout before release. A successful sandbox payment is not evidence that production settlement is configured correctly.

## 23 · Native apps, permissions, and platform behavior

**NAT-01 — MUST:** Use platform secure storage for device-held credentials. Treat the app binary and ordinary local storage as inspectable; do not embed server secrets. Apply relevant MASVS controls. [S17]

**NAT-02 — MUST:** Request camera, location, photos, contacts, notifications, and other permissions at a relevant moment with an explanation. Handle denial, limited access, revocation, and manual alternatives.

**NAT-03 — MUST:** Validate deep links and associated domains, protect sensitive exported components, and use appropriate system authentication/browser flows. [S17]

**NAT-04 — SHOULD:** Follow platform conventions for back navigation, safe areas, selection, menus, sharing, text scaling, and screen-reader navigation. Honor system preferences where appropriate. [S39], [S40]

**NAT-05 — MUST:** Test cold/warm launch, background/foreground transitions, process termination, interrupted network requests, and upgrades with existing data.

**NAT-06 — MUST:** Before distribution, recheck current store policies, permission declarations, privacy disclosures, signing, and account-deletion obligations for the target platform and app category.

## 24 · Engineering quality, delivery, and operations

**ENG-01 — SHOULD:** Choose the simplest maintainable architecture that meets real scale, reliability, security, and team constraints. Do not require microservices, a particular framework, or a database type for every project.

**ENG-02 — MUST:** Keep domain rules, authorization, and important invariants enforceable centrally. Use database constraints and transactions where appropriate; handle concurrency explicitly.

**ENG-03 — SHOULD:** Maintain typed/schema-validated boundaries, clear module responsibilities, reusable components, migration discipline, and documented non-obvious decisions. Reuse vetted libraries rather than recreating fragile infrastructure.

**ENG-04 — MUST:** Separate development, staging, and production configuration and credentials. Make builds reproducible; avoid debug settings in production. [S48]

**ENG-05 — MUST:** Use checks suited to the change: build/type/lint where relevant, unit tests for meaningful logic, integration tests for boundaries, and end-to-end tests for critical user journeys. Preserve unrelated work.

**ENG-06 — MUST:** Test important negative paths: unauthorized access, cross-tenant access, invalid input, timeout, concurrent submission, recovery, and rollback. Test observable behavior rather than mirroring implementation details.

**ENG-07 — SHOULD:** Make deployment incremental and reversible where possible. Validate migrations against representative data; use compatibility windows for mixed app versions.

**OPS-01 — MUST:** Establish who owns production, how incidents are detected, how users get help, and how to roll back. An app is not operationally ready merely because it has a public URL.

**OPS-02 — SHOULD:** Define service indicators and objectives for availability, successful operations, latency, and correctness based on user needs. Avoid inventing “99.99%” targets without cost and recovery planning. [S53]

**OPS-03 — MUST:** Collect useful, redacted diagnostics with correlation identifiers, access controls, retention, and actionable alerts. Avoid raw passwords, tokens, payment details, and unnecessary personal data. [S45]

**OPS-04 — MUST:** For important persistent data, define backup frequency, recovery-point and recovery-time objectives, and verify an actual restore. A configured backup job alone is not proof of recoverability.

**OPS-05 — SHOULD:** Monitor dependency/service changes, certificate expiry, storage and queue growth, mail delivery, external API failure, and spending. Set response ownership and remove unactionable alerts.

**OPS-06 — MUST:** Maintain an incident process, access revocation path, and vulnerability-remediation ownership proportionate to risk. Reassess affected rules after failures and significant changes. [S48]

## 25 · AI features, when present

**AI-01 — MUST:** Treat model output, retrieved content, uploaded files, and tool results as untrusted input. Prompt text is not an authorization boundary. [S54]

**AI-02 — MUST:** Enforce user/tenant permissions outside the model for retrieval and actions. Restrict tool scope, credentials, spend, and data exposure. Require appropriate user authorization for consequential operations. [S54]

**AI-03 — MUST:** Evaluate hallucination, prompt injection, sensitive-data leakage, and harmful failure modes on representative tasks. State uncertainty and provide correction/recovery paths.

**AI-04 — SHOULD:** Offer cancellation, understandable progress, output editing, and a non-AI fallback where important. Do not suggest an external action succeeded before the actual system confirms it.

**AI-05 — MUST:** Clarify data handling and retention for the selected provider and configuration. Do not assume all models or plans have identical privacy, cost, or quality properties.

## 26 · Complete user workflows and access paths

Design both the **journey** across channels and time, and the **flow** through specific product screens. A client’s experience may start in an invitation email and finish with a receipt, support conversation, or exported file. [S56]

**FLOW-01 — MUST:** For each primary task, identify the entry point, required access, prerequisite information, normal steps, successful outcome, exit, and recovery paths. Include returning users and deep links, not only the homepage.

**FLOW-02 — MUST:** Build an access matrix for relevant roles such as visitor, customer, team member, manager, support operator, and administrator. Specify view/create/edit/delete/export/share rights, object ownership, and tenant scope. Enforce the matrix in the backend.

**FLOW-03 — SHOULD:** Make frequent tasks easy to discover from the user’s natural starting point. Use recognizable navigation for broad areas and contextual controls for actions on an object. Command palettes and shortcuts supplement visible access.

**FLOW-04 — MUST:** Include branches for missing prerequisites, no data, rejected input, pending approval, denied access, expired login, lost connectivity, and service failure. Specify what happens to entered data in each branch.

**FLOW-05 — SHOULD:** Let users review consequential information and change an answer without repeating unrelated steps. Return them to the review context after an edit. [S57]

**FLOW-06 — MUST:** End important flows with an accurate confirmation, relevant reference/status, next step, and access to the resulting record or help. A tiny disappearing toast is insufficient as the only receipt for an important transaction. [S58]

**FLOW-07 — SHOULD:** Permit resuming longer tasks where appropriate; explain whether progress is saved locally, to an account, or not yet saved. Protect drafts according to data sensitivity.

**FLOW-08 — MUST:** Test flows by role and entry path: direct URL, email link, notification, internal navigation, browser back, refresh, and a new session. A screen accessible only through the developer’s exact click sequence is unfinished.

### Reusable flow blueprints

| Workflow | Normal path | Recovery / edge paths |
|---|---|---|
| First-time use | Relevant landing → value explanation → necessary access/signup → first useful task → result | Skip optional setup; resume after verification; accessible support |
| Returning use | Login/deep link → intended page → recent/contextual work → useful outcome | Expired session; wrong workspace; deleted destination |
| Find an item | Browse/search → refine → inspect detail → take action | Typo; no results; unavailable item; permission change |
| Create/edit | List or context → editor → validation → save/review → detail/confirmation | Draft; cancel; server error; concurrent edit; retry |
| Consequential submission | Enter information → check answers → commit → receipt/status | Change answer; duplicate request; uncertain/pending result |
| Purchase/subscription | Product/plan → full cost → checkout → provider confirmation → receipt/access | Failure; pending payment; refund; renewal; cancellation |
| Invitation/team access | Invitation → authenticate intended identity → accept scope → workspace | Expired/revoked invite; wrong account; existing membership |
| Recovery | Login → recovery request → out-of-band proof → reset/recover → login | Expired/replayed link; unavailable factor; delayed email |
| Export | Select scope → authorize → generate → secure download | Large job; expired link; partial failure; removed permission |
| Leave/delete | Settings → explain consequences → appropriate verification → confirm → status | Shared data; retention obligations; outstanding billing |

**Acceptance check:** A representative client can find, complete, recover from, and later revisit each primary task without being told which control to click. Record observed failures; do not substitute a developer walkthrough for usability evidence.

## 27 · What belongs where: screen anatomy

The placements below are **house starting points** for common interfaces. Adapt to platform conventions, reading direction, task frequency, information density, and user testing. There is no universal right-hand side for every action.

| Element | Default location / behavior | Why and when to adapt |
|---|---|---|
| Product identity / home | Stable global header or platform navigation | Helps orientation; do not let branding crowd the task. |
| Primary navigation | Top navigation for small sites; sidebar for broad desktop tools; suitable platform navigation on mobile | Keep stable destinations distinct from one-off actions. |
| Current page title | Start of main content, with short context if needed | Makes location and purpose immediately understandable. |
| Breadcrumbs | Above the title for meaningful hierarchical structures | Use for hierarchy, not as a substitute for browser back. |
| Page-level primary action | Near the title or start of the task area | Frequent work stays discoverable; dense tools may use a toolbar. |
| Search | Near the collection it searches; global search in global chrome if it spans the app | State scope clearly. |
| Filters / sorting | Before or beside the result set; compact mobile sheet when suitable | Show active filters and make reset available. |
| Object actions | In the row, card, detail header, or contextual toolbar belonging to that object | Avoid ambiguity about which item will change. |
| Form labels / hints / errors | Adjacent to the relevant field | Preserve association when wrapping or zooming. |
| Form submit / next | After the fields, with secondary back/cancel logically separated | Sticky actions may help long mobile tasks if they never cover content. |
| Destructive actions | Clearly named, separated from routine actions, often within object/settings context | Reduce slips while keeping legitimate removal discoverable. |
| Global account / workspace | Stable account or workspace control in application chrome | Distinguish personal settings from organization administration. |
| Help | Consistent help location plus contextual help near hard decisions | Clients should not lose their draft to get help. |
| Site-wide notice | Reserved area near the top of the layout | Its importance should match its scope; avoid covering navigation. |
| Notification center | Stable accessible location when durable messages justify one | It supplements, rather than replaces, inline feedback. |
| Footer | Supporting, contact, policy, and secondary links | Do not place the only path to a primary task at the bottom. |

**LAY-01 — MUST:** Define a clear reading order and DOM/accessibility order. Visual reordering must not produce a confusing keyboard or screen-reader sequence.

**LAY-02 — SHOULD:** Keep controls near the object or decision they affect. Separate global, page, selection, and item actions so the action scope is clear.

**LAY-03 — MUST:** Labels, prices, selected counts, and consequences must remain understandable in compact layouts. A mobile overflow menu must not become an undiscoverable hiding place for the core task.

**LAY-04 — SHOULD:** Reserve sticky positioning for controls people genuinely need while scrolling. Account for banners, navigation, software keyboards, and assistive-technology focus before making a footer sticky.

**LAY-05 — SHOULD:** Keep the primary task visually dominant. Marketing popups, chat launchers, surveys, and installation prompts must not compete with checkout, recovery, or urgent work.

## 28 · Screen changes, history, and state transitions

**TRN-01 — MUST:** Specify what changes after every meaningful action: route, selected object, data, scroll, focus, URL/history, and feedback. Model asynchronous work explicitly rather than scattering unrelated booleans.

| Transition | Expected behavior |
|---|---|
| Navigate to a new page | Update title/location and route history; orient focus appropriately; normally start at the relevant heading/content. |
| Return to a collection | Restore useful filters, selection, pagination, and scroll when safe. |
| Change a local tab | Update the selected panel and tab semantics; preserve other page context. |
| Expand disclosure | Reveal nearby content without pretending the user navigated away. |
| Submit form | Keep entered data; show pending; move to success only on a confirmed result; expose recoverable errors. |
| Open detail drawer | Preserve the originating collection; define whether the detail has a shareable URL and what Back does. |
| Start background job | Confirm job acceptance separately from completion; provide durable status and failure recovery. |
| Session expires | Explain reauthentication, protect the data, and preserve eligible non-sensitive work. |
| Refresh / reconnect | Reconcile server state; do not repeat consequential writes automatically. |

**TRN-02 — MUST:** Handle out-of-order responses and races. A slow response to an older search must not overwrite a newer query’s results. Cancel or ignore obsolete requests appropriately.

**TRN-03 — MUST:** Define dirty-form behavior. Warn about unsaved work only when real work could be lost; offer save/discard/cancel where appropriate. Avoid repeated false alarms.

**TRN-04 — SHOULD:** Use motion to clarify continuity, hierarchy, or feedback. Keep it restrained and interruptible; honor reduced-motion preferences. Do not hold completed work behind a decorative animation. [S59]

**TRN-05 — MAY — House default:** Start with roughly 100–200 ms for small state feedback and 200–300 ms for larger transitions, then tune to platform guidance and testing. These ranges are not standards or guarantees of usability.

**TRN-06 — MUST:** Deep links and browser/platform Back must behave coherently with drawers, dialogs, tabs, and routing. Do not add a browser-history entry for every hover, toast, or minor animation.

**TRN-07 — MUST:** Test interruptions mid-transition: rapid clicks, navigation away, rotation/resizing, keyboard opening, network loss, and permission revocation.

## 29 · Overlays, drawers, half-screen sheets, and popovers

Choose a surface by task complexity, need for surrounding context, and whether interaction outside must stop. Its visual position does not determine modality. A drawer or bottom sheet can be modal or nonmodal; implement its actual behavior consistently. [S60], [S61]

| Surface | Use when | Avoid when |
|---|---|---|
| Inline expansion | Small details or settings belong directly in context | A separate, complex workflow is required |
| Tooltip | Brief supplementary explanation for a labeled control | Essential instructions, interactive links/forms, or mobile-only discovery |
| Popover | A short contextual choice, picker, or supplementary interactive content | A long multi-step task or information needing a persistent URL |
| Nonmodal side panel | Inspect/edit while referring to the underlying collection | Background interaction would invalidate a critical decision |
| Modal dialog | A focused interruption is necessary and the task is short | Routine navigation, promotional announcements, or long forms |
| Bottom / partial-height sheet | A short mobile action, filter, picker, or contextual detail | A fixed “half-screen” height would clip text, keyboard, or controls |
| Full screen / page | Complex creation, long reading, multi-step editing, or deep-linkable work | A tiny contextual action would need needless navigation |
| Alert dialog | An urgent message needs a decision before work continues | Ordinary success feedback or nonurgent news |

**OVR-01 — MUST:** Declare modal vs nonmodal behavior. A modal blocks background interaction, has an accessible name and proper semantics, and manages focus. A nonmodal panel must not trap focus or falsely use modal semantics. [S42], [S63]

**OVR-02 — MUST:** Provide a visible, accessible dismissal path, sensible Escape/platform Back behavior, and focus restoration. For unsaved changes, dismissal may require a clear save/discard decision. Backdrop taps must not silently discard important work.

**OVR-03 — SHOULD:** Avoid nested modals. Prefer replacing the current dialog, expanding to a page, or using inline secondary content. If unavoidable, explicitly test the active focus boundary, dismissal order, and screen-reader behavior.

**OVR-04 — MUST:** Define layering centrally: normal content → sticky navigation → contextual overlay → active modal/backdrop → emergency feedback as appropriate. Use platform top-layer APIs when suitable; arbitrary huge `z-index` values cannot solve every stacking context.

**OVR-05 — MUST:** Keep overlays within the usable viewport. Reposition anchored popovers on collision; preserve their association with the trigger. Test zoom, scroll, keyboard, RTL, and viewport resizing.

**OVR-06 — MUST:** For sheets, support content-appropriate height and scrolling. Do not impose an exact 50% height as a universal rule. Provide a button alternative to dragging the sheet handle and distinguish scrolling content from moving the sheet. [S61]

**OVR-07 — MUST:** Prevent unwanted background scroll for modal surfaces and restore the original scroll position on close. Keep the dialog’s own content scrollable and its actions reachable.

**OVR-08 — MUST:** Hover/focus content must meet applicable dismissible, hoverable, and persistent behavior. Tooltips keep focus on their trigger and must not contain interactive controls; use a suitable popover/dialog for those. [S64], [S67]

**OVR-09 — MUST:** Prevent overlays from hiding primary actions, error messages, another overlay’s close control, or keyboard focus. Test cookie UI, chat launchers, toasts, and mobile bottom bars together.

## 30 · Notifications, banners, toasts, and alerts

Start with **urgency, scope, required action, and persistence**. Default to the least disruptive surface that makes the message discoverable. Banner and notification systems provide useful patterns, but their exact placement/focus conventions are design-system specific. [S62], [S68]

| Message | Preferred pattern | Placement / lifetime |
|---|---|---|
| Invalid field | Inline error plus summary for complex forms | At the field; until corrected |
| “Copied” / small completed action | Local status or polite toast | Near context or a consistent non-obstructive region; transient |
| Save in progress | Adjacent status / progress | At the action or editor; until resolved |
| Failed save / payment uncertainty | Persistent inline/page message with recovery | At the affected task; not only a disappearing toast |
| Page-level requirement | Inline notice or page banner | Near relevant heading/content; until resolved or acknowledged appropriately |
| Service-wide disruption | Global banner / status area | Consistent site-wide region; deduplicate and update |
| Destructive/urgent decision | Suitable confirmation or alert dialog | Modal only when response is genuinely necessary |
| Background export completed | Notification center, contextual update; optional chosen channel | Durable link/status with access checks |
| New feature / promotion | Optional nonblocking content | Never interrupt a consequential flow |
| Time-sensitive external reminder | Opted-in push/email/SMS as appropriate | Respect preferences, local time, and privacy |

**MSG-01 — MUST:** Every message should identify the event or issue, its relevant consequence, and any available next step. Use severity words/icons as well as color.

**MSG-02 — MUST:** Use suitable announcement semantics. Routine updates are usually polite status messages; reserve assertive alerts for urgency. An `alert` does not need focus or a decision; an `alertdialog` does. [S63]

**MSG-03 — MUST:** Do not auto-focus every background notification. Place focus deliberately for actual navigation, errors, or modal tasks; verify announcements without duplication.

**MSG-04 — SHOULD:** Coalesce repeated messages and cap simultaneous visible toasts. A house default is one visible toast with a short queue; do not lose important messages when suppressing duplicates.

**MSG-05 — MUST:** A disappearing toast cannot be the only route to important information or a required action. Provide durable status/history or keep the notice available. If timing limits apply, satisfy the relevant accessibility requirements.

**MSG-06 — SHOULD:** Give dismiss controls meaningful names and adequate targets. Define whether dismissal lasts for the session, user, message version, or until the underlying problem changes; avoid repeatedly resurfacing irrelevant messages.

**MSG-07 — MUST:** Link notifications to the correct authorized destination. Handle expired/deleted objects, different workspaces, logged-out users, and changed permissions without leaking content.

**MSG-08 — SHOULD:** A notification center, when needed, should show understandable timestamps, read/unread state, grouping, preferences, and bulk controls. Badges should represent real actionable/unread items rather than manufactured urgency.

**MSG-09 — MUST:** Test notification priority under failure: an informational toast must not cover a payment error; chat bubbles and banners must not hide dialog actions or focus.

## 31 · Micro-actions and component behavior

**MIC-01 — MUST:** Specify each micro-action as **trigger → pending/state change → feedback → failure/reversal**. Small actions still need clear meaning, accessibility, and trustworthy persistence.

| Action / component | Required or preferred behavior |
|---|---|
| Save | Distinguish unsaved, saving, saved, and failed; define whether manual save, autosave, or both apply. |
| Autosave | Show actual persistence; avoid saving invalid intermediate states as final; handle conflicts and reconnect. |
| Copy | Confirm only after clipboard success; give a fallback if permission/API support fails. |
| Favorite / bookmark | Expose selected state; optimistic updates may revert with clear feedback on failure. |
| Toggle / switch | Use for an immediate binary setting; if Save is required, make that interaction model explicit. |
| Checkbox | Represent independent selection or consent; a group needs a clear name and mixed-state behavior where relevant. |
| Radio group | Represent one selection among visible options; do not use it as disguised navigation. |
| Tabs | Switch related peer panels; apply the matching keyboard model; use links for actual navigation when appropriate. |
| Accordion | Keep the header operable and expose expanded state; do not hide essential validation without indicating it. |
| Delete / archive | State the item and effect; prefer recoverable deletion/undo when appropriate; irreversible deletion needs suitable review. |
| Undo | Reverse the real operation, not just its animation; disclose limits and preserve an accessible recovery route. |
| Drag / reorder | Provide keyboard/button alternatives; show the proposed result and commit it reliably. |
| Upload | Show restrictions, progress, cancel/retry, per-file errors, and confirmed completion. |
| Download / export | Indicate generation vs download; give useful file type/scope; authorize the actual download. |
| Autocomplete | Follow an accessible combobox pattern; differentiate typed text, active suggestion, and committed value. [S65] |
| Overflow action menu | Name the menu’s context; use the menu-button interaction model where it is genuinely an action menu. [S66] |
| Bulk action | Show selected count and scope, especially “this page” vs “all matches”; handle partial failures explicitly. |
| Refresh | Preserve user context; distinguish current, refreshing, and stale content. |

**MIC-02 — MUST:** Provide keyboard, touch, and assistive-technology operation for the chosen component pattern. Do not apply ARIA menu/grid roles merely for appearance.

**MIC-03 — SHOULD:** Use brief visual feedback to show cause and effect. Avoid confetti, sound, vibration, or animation on routine actions unless justified by the product and user preferences.

**MIC-04 — MUST:** Protect micro-actions against rapid activation, stale state, offline retries, and permission changes. Optimistic feedback must never create false certainty about a charge or irreversible operation.

**MIC-05 — SHOULD:** Hover/pressed/focus states should be consistent and visually distinguishable. Hover is supplementary; focus is essential for keyboard use. Test every state in both themes.

## 32 · Dashboards, tables, charts, and complex client tools

**DATA-01 — SHOULD:** Start a dashboard with the decisions users need to make, not the number of charts available. Show exceptions and next actions with enough context to interpret them.

**DATA-02 — MUST:** Label measures with units, date range, time zone, comparison baseline, and freshness. Distinguish zero, missing, loading, unavailable, and withheld values.

**DATA-03 — SHOULD:** Use tables for precise comparison and charts for patterns; provide accessible summaries or data alternatives. Avoid misleading axes, ambiguous denominators, and decorative 3D effects.

**DATA-04 — MUST:** Preserve table header relationships, sorting state, row-action context, and selection semantics. Do not turn a simple static table into an ARIA grid unless the richer keyboard interaction is implemented.

**DATA-05 — SHOULD:** Allow useful filtering, sorting, search, pagination, and saved views according to the task. Make current scope visible and clearable. A filter must not silently apply to only a loaded page while appearing global.

**DATA-06 — MUST:** Provide appropriate empty and partial-result states. For large/virtualized collections, verify keyboard access, screen-reader exposure, search behavior, and selection across unloaded rows.

**DATA-07 — MUST:** Apply row/column/tenant permissions to on-screen data, charts, counts, exports, drill-downs, and shared links. A chart tooltip must not reveal data hidden by the table.

**DATA-08 — SHOULD:** Let clients move from summary → detail → action → confirmation without losing the original collection context.

## 33 · Human factors and psychology without manipulation

**PSY-01 — SHOULD:** Reduce avoidable cognitive effort: keep necessary context visible, use familiar language, group related information, and avoid making users remember details across steps. [S69]

**PSY-02 — SHOULD:** Distinguish slips from misunderstandings. Prevent slips with spacing and reversible actions; correct misunderstandings with clearer concepts, labels, examples, and feedback.

**PSY-03 — SHOULD:** Match perceived effort to actual effort. State requirements early, show truthful progress, allow saving, and do not present a surprise set of extra steps after claiming completion.

**PSY-04 — SHOULD:** Support confidence through predictable behavior, accurate status, transparent consequences, and consistent control placement. Visual polish should strengthen that trust rather than substitute for it.

**PSY-05 — MUST:** Use defaults carefully. Default to a useful, safe, reversible choice; do not preselect marketing consent, surprise spending, public exposure, or irreversible disclosure.

**PSY-06 — MAY:** Use genuine social proof, reminders, and progress cues when relevant. Treat anchoring, scarcity, gamification, and loss-framed messaging as context-sensitive influences with ethical risks, not automatic conversion tricks.

**PSY-07 — MUST:** Account for stress, interruption, fatigue, limited literacy, and temporary disability. Recovery, security, billing problems, and urgent workflows deserve especially direct language and low memory demand.

**PSY-08 — SHOULD:** Observe real task behavior, not only stated preference. A user may like a beautiful screen but fail the task; success, error recovery, confidence, and accessibility all matter.

**PSY-09 — MUST:** Do not turn popular claims into requirements without evidence: “everyone scans in an F,” “people have an eight-second attention span,” “red always sells,” and “more time in app means success” are not universal rules.

## 34 · DevSecOps and software supply-chain discipline

Security belongs throughout delivery, with concrete ownership and checks proportionate to risk. Tools support a process; adding scanners does not by itself establish secure development.

**DSO-01 — MUST:** Before implementation, identify trust boundaries, data classes, abuse cases, and security acceptance criteria for sensitive changes. Update the threat assessment when identity, payments, integrations, or privileges change.

**DSO-02 — MUST:** Protect repositories and CI/CD configuration with appropriate access controls and review. Untrusted pull-request/build code must not receive production secrets or a privileged deployment identity. [S70]

**DSO-03 — SHOULD:** Prefer short-lived, narrowly scoped workload credentials and isolated build environments. Separate build, approval, and deployment permissions where risk warrants it. [S70]

**DSO-04 — MUST:** Run appropriate secret detection, dependency analysis, code analysis, and infrastructure checks in the delivery path. Add dynamic testing where relevant; triage findings with context, ownership, and deadlines rather than silently ignoring scanner noise. [S70]

**DSO-05 — SHOULD:** Pin dependencies and CI actions appropriately, use trusted registries, track dependencies/SBOMs where useful, and retain artifact provenance. Verify that the artifact deployed is the reviewed/tested artifact. [S71]

**DSO-06 — MUST:** Design deploy credentials and environments to prevent untrusted builds from modifying release artifacts, poisoning shared caches, or escalating into production. [S70]

**DSO-07 — SHOULD:** Use staged release, canary or feature-flag controls when justified; define rollback criteria and who responds. Remove stale flags and obsolete access.

**DSO-08 — MUST:** Handle leaked secrets as compromised: revoke/rotate and assess use. Merely deleting the value from the latest commit is insufficient.

**DSO-09 — MUST:** Track vulnerabilities through validation, remediation, regression verification, and monitored release. Prioritize exploitability, exposure, and business impact; document risk acceptance and review dates.

**DSO-10 — SHOULD:** Rehearse meaningful failure scenarios, including rollback, restore, revoked credentials, third-party outages, and compromised accounts. Turn incident lessons into narrow improvements to controls and tests.

## 35 · Review from every relevant professional perspective

This handbook synthesizes published standards and specialist guidance. It does not claim that multiple professionals personally reviewed the product or that a document replaces years of experience. Use the following perspectives to expose blind spots.

| Perspective | Questions the review must answer | Useful evidence |
|---|---|---|
| Product manager / domain expert | Does this solve the right problem, with a workable business process? | Primary task, outcome metric, domain acceptance |
| UX researcher | Can real clients find, understand, complete, and recover from the task? | Observed usability sessions, journey/flow findings |
| Information architect | Can people predict where features and information belong? | Navigation/search tests, clear labels and scope |
| UI / visual designer | Is hierarchy coherent, readable, and consistent across states/themes? | Rendered screens, token/state review |
| Interaction / motion designer | Are transitions, overlays, focus, gestures, and micro-actions coherent? | Interaction specifications and interruption tests |
| Human-factors specialist | Where do memory demands, slips, stress, and misleading cues cause failure? | Error/recovery observations, cognitive walkthrough |
| Accessibility specialist | Can disabled users complete the whole process? | Manual assistive-technology checks plus automated findings |
| Content designer / localizer | Are instructions, errors, consequences, and translations understandable? | Content review, long-text and RTL checks |
| Frontend / mobile engineer | Does the interface work across supported devices and input methods? | Real-device/browser checks, runtime/performance evidence |
| Backend / data engineer | Are invariants, permissions, transactions, migrations, and data lifecycle correct? | Integration/concurrency tests, schema and access checks |
| Security engineer | Can an attacker bypass authorization, steal credentials, or misuse a workflow? | Threat model and focused abuse/security tests |
| Privacy specialist | Is collection justified and are user controls truthful and implementable? | Data inventory, retention/export/deletion checks |
| DevSecOps engineer | Can source, builds, dependencies, or deployment identity be compromised? | Pipeline permissions, scan triage, artifact provenance |
| QA engineer | Are expected, negative, boundary, and regression paths covered? | Traceable acceptance results, reproducible defects |
| SRE / operations | Will failures be noticed, contained, communicated, and recovered? | SLOs, actionable alerts, rollback and restore evidence |
| Analyst / experimentation specialist | Are metrics meaningful, correctly collected, and interpreted cautiously? | Event validation, cohort definitions, experiment plan |
| Support / customer success | Can clients get help, resolve recurring problems, and leave fairly? | Support paths, incident messaging, churn feedback |

**REV-01 — MUST:** Review all relevant perspectives for major releases; for small changes, select affected perspectives. One person may cover multiple roles, but must not fabricate specialist sign-off.

**REV-02 — SHOULD:** Prioritize failures in this order: safety/data integrity and access control; completion of core tasks; accessibility and recovery; reliability/performance; clarity and visual polish; optional delight. Adjust for actual harm and audience.

**REV-03 — MUST:** Convert review findings into reproducible issues with affected users, steps, expected/actual behavior, severity, owner, and verification evidence. A list of opinions alone is not a quality process.

## 36 · Definition of done and maintenance

**DONE-01 — MUST:** Before design approval, identify primary journeys, data risks, relevant rules, and measurable acceptance criteria. Include loading, error, empty, permission, and narrow-screen states in the design.

**DONE-02 — MUST:** Before implementation is complete, verify affected behavior and relevant accessibility/security requirements. Fix blockers or state them clearly; do not describe untested behavior as working.

**DONE-03 — MUST:** Before release, complete the companion release checklist with evidence, owners, and justified N/A entries. Critical data exposure, privilege bypass, broken recovery, inaccessible primary flows, and unhandled data-loss risks block a production-ready claim.

**DONE-04 — SHOULD:** After release, monitor real outcomes and compare them with the product’s baseline. Use incidents, support, usability studies, and field measurements to refine the product and this rulebook.

**DONE-05 — MUST:** Recheck changing guidance at the point of implementation: security advisories, password-hashing parameters, identity-provider requirements, browser support, platform/store policies, and applicable law. The research date is not a permanent guarantee of currency.

### Practical project record

```text
Product / feature:
Primary users and task:
Platforms and supported versions:
Data sensitivity and applicable markets:
Success event and guardrail metrics:
Relevant rule IDs:
Acceptance checks and evidence:
Exceptions, rationale, compensating controls:
Unverified items and owner:
Release / rollback / recovery owner:
Next review trigger or date:
```

### How Codex uses this book

For app/website work on this Mac, the personal `development-rulebook` skill and the global Codex instruction point to the maintained reference. Read the skill first, select relevant chapters, translate them into acceptance criteria, implement within the authorized scope, and report verification and gaps. This is not authorization to deploy, send messages, access unrelated systems, or redesign unrelated features.

The workspace Markdown and HTML are a portable snapshot. The maintained personal reference is `~/.codex/skills/development-rulebook/references/rulebook.md`. Update the maintained reference and regenerate/synchronize the snapshot when revising the book. Global instructions apply through Codex’s supported instruction-loading mechanism; cloud tasks and other machines need their own copy/configuration. [S55]

## 37 · Art direction and coherent visual identity

Use the [Design & Interaction Atlas](DESIGN-ATLAS.md) and [pattern catalogue](PATTERN-CATALOG.md) for options. These references contain original recipes and a dated source register; they do not certify third-party components.

**ART-01 — MUST:** Establish the affected audience, primary task, brand constraints, real content, platform, and performance/accessibility needs before choosing an expressive treatment. Preserve the existing system unless a change is within scope.

**ART-02 — SHOULD:** For a substantial new visual design, explore two or three coherent directions internally and select using task clarity, identity, content fit, accessibility, and implementation cost. Do not ask the user to choose every routine detail.

**ART-03 — SHOULD:** Make distinctiveness come from specific content, composition, typography, imagery, and a few meaningful interactions. Do not default every project to the same gradient hero, bento grid, glowing border, or glass card.

**ART-04 — MUST:** Keep essential content, commands, reading order, and meaning available independently of decorative animation, canvas, hover, or unusual gestures. A distinctive style must preserve a usable baseline.

**ART-05 — SHOULD:** Define an image grammar—subject, crop, viewpoint, lighting, palette, texture—and use it consistently. Use original or properly licensed assets and truthful product demonstrations.

**ART-06 — MUST:** Distinguish decorative artwork, illustrative sample data, and actual evidence. Do not let a simulated interface, synthetic portrait, map, statistic, or certificate imply a real event, customer, capability, or assurance it does not represent.

**ART-07 — SHOULD:** Use separators to support content rhythm: whitespace, rule, tonal zone, label, or a restrained thematic edge. Check that waves, diagonal cuts, and oversized type do not clip content or disturb reading order at narrow widths.

**ART-08 — MUST:** Evaluate expressive designs with representative content, long text, touch, keyboard, zoom, both relevant themes, and task outcomes. Visual novelty or popularity is not evidence of improved retention or conversion.

## 38 · Interaction choreography and motion systems

**MOT-01 — MUST:** Give each substantial interaction a contract: trigger; before/pending/committed/failed states; feedback; focus/history/scroll; cancellation; repeated input; recovery; and reduced-motion/static alternatives. Reuse chapters 28–31 for behavioral details.

**MOT-02 — SHOULD:** Separate routine functional feedback from occasional expressive moments. Keep repeated operational work quiet; make a signature interaction explain the product or acknowledge a meaningful outcome. [S76], [S77]

**MOT-03 — MUST:** Respect reduced-motion preferences for nonessential movement, including JavaScript, SVG, canvas, video, parallax, and scroll effects. This house baseline extends beyond WCAG AA; WCAG 2.3.3 itself is AAA. Do not rely on one CSS rule to stop every rendering engine. [S78], [S79]

**MOT-04 — MUST:** Apply the relevant pause/stop/hide requirements for automatically moving, blinking, scrolling, or updating content. Provide an appropriate control when the criterion applies; a pause only while hovering is not a general substitute. Avoid flashing effects. [S80], [S01]

**MOT-05 — MUST:** Keep real hit targets, keyboard paths, and accessible state stable. Magnetic buttons, animated cursors, drag, tilt, or swipe must not be the sole route to essential functionality. Pointer ornament must not intercept input.

**MOT-06 — MUST:** Make navigation and completion work without the transition. Test rapid back/forward, direct URLs, repeated actions, interrupted animations, stale network responses, and focus restoration. A motion library does not implement the application's state contract. [S81]

**MOT-07 — SHOULD:** Use consistent motion tokens for duration, easing, distance, and staggering; fit platform conventions and task frequency. Treat the atlas timing ranges as house starting values, never mandatory scientific thresholds.

**MOT-08 — MUST:** For persistent or expensive effects, bound work, stop when not needed, clean up listeners/resources, handle rendering failure, and measure representative devices. A static poster or DOM equivalent must keep the task usable. Prefer the simplest implementation that meets the intent. [S82]

## 39 · Themes, materials, and responsive composition

**DSN-01 — SHOULD:** Separate foundation, semantic, and component tokens. Keep brand, appearance, density, and motion preference independent where useful; avoid unnecessary combinations. Token formats support exchange but do not establish design quality or accessibility. [S83]

**DSN-02 — MUST:** Verify actual foreground/background pairs and state affordances. Mood-board swatches and third-party color scales are not approved accessible pairings. Dynamic backgrounds require predictable text surfaces or demonstrated worst-case contrast.

**DSN-03 — MUST:** Give blur, glass, blend modes, masks, gradients, and unusual shapes a readable fallback. Essential boundaries and focus cannot depend only on shadows, transparency, or subtle texture.

**DSN-04 — SHOULD:** Adapt the composition to content and task: a desktop detail pane may become a routed page; a broad comparison may become labeled stacked groups. Do not merely scale the desktop layout down or hide difficult essential features.

**DSN-05 — MUST:** Keep source/reading order meaningful despite masonry, bento, layering, and asymmetric layouts. Any visual reordering must preserve understandable keyboard and assistive-technology navigation.

**DSN-06 — SHOULD:** Use a bounded fluid typography/spacing system where suitable, then test zoom, long labels, fallback fonts, script coverage, and layout stability. Avoid pure viewport sizing that defeats useful text enlargement.

**DSN-07 — MUST:** Implement light/dark/system as real supported states when in scope, including first paint, preference changes, semantic status colors, media, overlays, and charts. Theme switching must not reset the user's task or draft.

**DSN-08 — SHOULD:** Document the reusable anatomy and allowed variation of each adopted component. Include content limits, responsive transformations, density, states, and ownership so the system remains coherent as features grow.

## 40 · Inspiration, component adoption, and design verification

**LIB-01 — MUST:** Distinguish inspiration, inspected source, adapted component, and verified integration. A directory listing or attractive demo is not evidence of security, performance, accessibility, compatibility, or permission to reuse.

**LIB-02 — MUST:** Before importing, inspect the exact component's author, version/commit, code license, dependencies, and separate asset/font rights. Preserve notices and redistribution restrictions. Recheck changing terms at adoption; community entries may have different terms. [S72], [S84]

**LIB-03 — MUST:** Treat registry commands and downloaded code as third-party inputs. Review package scripts, remote requests, dynamic markup/SVG, event handlers, secret handling, and dependencies relevant to the integration. Follow the existing DevSecOps rules rather than trusting a familiar catalogue name.

**LIB-04 — SHOULD:** Reuse the project's framework, primitives, tokens, and motion engine. Adapt a useful idea instead of accumulating incompatible component systems or shipping a large dependency for one small effect.

**LIB-05 — MUST:** Record a meaningful acceptance check for every adopted enhancement: affected task, expected behavior, fallback, relevant devices/preferences, and actual verification evidence. Unimplemented recipes remain unverified ideas.

**LIB-06 — SHOULD:** Evaluate alternatives against task success, comprehension, error/recovery, performance, and maintenance as well as visual identity. An experiment needs a defined outcome and guardrails; do not promise a conversion increase from an aesthetic trend.

**LIB-07 — MUST:** Keep provenance and review status in the atlas and project adoption record. Do not describe category coverage as inspection of every example, or curated research as coverage of the complete internet.

**LIB-08 — SHOULD:** Revisit chosen sources before implementation and update the library as relevant needs emerge. The library's existence does not authorize purchases, ongoing scraping, package installation, deployment, or changes outside the requested scope.


## Sources and evidence notes

Sources were consulted on **18 September 2026**. Standards supply scoped requirements; official implementation guides and practitioner research inform practices. The particular workflows, ID system, starter palette, sample test matrix, and release gates are our synthesis. They are not quoted external standards or promises of business results. The linked documents contain exceptions and detail beyond this handbook.

| Ref | Source | Used for |
|---|---|---|
| S01 | [W3C: WCAG 2.2][S01] | Web accessibility baseline, conformance scope, input and interaction criteria |
| S02 | [Nielsen Norman Group: ten usability heuristics][S02] | Interaction review principles |
| S03 | [NN/g: aesthetic–usability effect][S03] | Attraction and perceived usability |
| S04 | [NN/g: Fitts’s law][S04] | Target selection and reach |
| S05 | [Laws of UX: Hick’s law][S05] | Practitioner interpretation of choice complexity; a heuristic |
| S06 | [NN/g: proximity][S06] | Visual grouping |
| S07 | [NN/g: similarity][S07] | Consistent visual meaning |
| S08 | [NN/g: common region][S08] | Containers and grouping |
| S09 | [NN/g: progressive disclosure][S09] | Complexity and advanced options |
| S10 | [NN/g: response-time limits][S10] | Approximate perceived response guidance |
| S11 | [Google Research: HEART][S11] | Goals, signals, user-centered metrics |
| S12 | [FTC: Bringing Dark Patterns to Light][S12] | Deceptive design and user autonomy |
| S13 | [Adobe: color combinations][S13] | Palette harmony |
| S14 | [Android Developers: Material 3 theming][S14] | Color roles and theme structure |
| S15 | [GOV.UK: password input][S15] | Reveal toggle and password input behavior |
| S16 | [OWASP: ASVS][S16] | Web verification catalogue; stable 5.0.0 used |
| S17 | [OWASP: MASVS][S17] | Native mobile security |
| S18 | [NIST SP 800-63B-4: authenticators][S18] | Password and authenticator policy |
| S19 | [OWASP: authentication][S19] | Login, MFA, reauthentication |
| S20 | [OWASP: password storage][S20] | Hashing and current baseline parameters |
| S21 | [OWASP: forgot password][S21] | Secure recovery |
| S22 | [OWASP: authorization][S22] | Object/tenant access and deny by default |
| S23 | [OWASP: session management][S23] | Cookies, token exposure, expiry and revocation |
| S24 | [IETF RFC 9700][S24] | OAuth 2.0 security best current practice |
| S25 | [OWASP: SQL injection prevention][S25] | Query parameterization |
| S26 | [OWASP: XSS prevention][S26] | Output encoding and HTML sanitization |
| S27 | [OWASP: CSRF prevention][S27] | State-changing request protection |
| S28 | [OWASP: file upload][S28] | Untrusted upload controls |
| S29 | [OWASP: REST security][S29] | CORS and API token verification |
| S30 | [W3C: accessible authentication][S30] | Cognitive access and assistive mechanisms |
| S31 | [W3C: minimum text contrast][S31] | Numeric contrast targets and exceptions |
| S32 | [W3C: reflow][S32] | 320 CSS px and zoom testing |
| S33 | [W3C: target size minimum][S33] | 24 CSS px and scoped exceptions |
| S34 | [W3C: non-text contrast][S34] | Essential controls and graphics |
| S35 | [W3C: text spacing][S35] | User override testing |
| S36 | [MDN: prefers-color-scheme][S36] | OS theme preference |
| S37 | [MDN: color-scheme][S37] | Browser rendering of supported themes |
| S38 | [web.dev: responsive design basics][S38] | Flexible layouts and content-driven breakpoints |
| S39 | [Apple: UI design tips][S39] | Touch target and platform design guidance |
| S40 | [Android: accessible apps][S40] | Touch targets and native accessibility |
| S41 | [W3C WAI: forms tutorial][S41] | Labels, grouping, and feedback |
| S42 | [W3C APG: modal dialogs][S42] | Focus and modal interaction |
| S43 | [OWASP: SSRF prevention][S43] | Outbound request restrictions |
| S44 | [OWASP: secrets management][S44] | Credential lifecycle |
| S45 | [OWASP: logging][S45] | Redaction and safe diagnostics |
| S46 | [Stripe: webhooks][S46] | Provider-specific example of signatures and duplicate delivery |
| S47 | [ICO: data-protection principles][S47] | UK-specific privacy principles; applicability must be checked |
| S48 | [NIST: SSDF 1.1][S48] | Secure development and vulnerability response practices |
| S49 | [web.dev: Web Vitals][S49] | Field performance thresholds and lab limitations |
| S50 | [NN/g: onboarding tutorials vs contextual help][S50] | Contextual onboarding |
| S51 | [W3C: internationalization quick tips][S51] | Language, Unicode, and direction |
| S52 | [Google Search: SEO starter guide][S52] | Public-content discovery |
| S53 | [Google SRE: service-level objectives][S53] | User-centered reliability objectives |
| S54 | [OWASP: LLM prompt-injection prevention][S54] | AI trust boundaries and defense in depth |
| S55 | [OpenAI: AGENTS.md instructions][S55] | Persistence mechanism and scope |
| S56 | [NN/g: journeys vs flows][S56] | End-to-end journey and screen-flow distinction |
| S57 | [GOV.UK: check answers][S57] | Review and correction before submission |
| S58 | [GOV.UK: confirmation pages][S58] | Completion, receipt, and next steps |
| S59 | [NN/g: purpose of animation][S59] | Continuity, hierarchy, and feedback |
| S60 | [NN/g: modal and nonmodal dialogs][S60] | Interruption and context tradeoffs |
| S61 | [Android: bottom sheets][S61] | Sheet presentation and dismissal implementation |
| S62 | [Carbon: notification patterns][S62] | Notification surface selection |
| S63 | [W3C APG: alert dialogs][S63] | Urgent decisions vs passive announcements |
| S64 | [W3C APG: tooltip pattern][S64] | Supplementary, noninteractive tooltip behavior; pattern is work in progress |
| S65 | [W3C APG: combobox pattern][S65] | Accessible autocomplete behavior |
| S66 | [W3C APG: menu button pattern][S66] | Action-menu keyboard behavior |
| S67 | [W3C: content on hover or focus][S67] | Dismissible, hoverable, persistent content |
| S68 | [GOV.UK: notification banner][S68] | Page-level notice example |
| S69 | [NN/g: cognitive load][S69] | Memory and task effort |
| S70 | [OWASP: CI/CD security][S70] | Delivery pipeline trust boundaries |
| S71 | [SLSA specification 1.2][S71] | Supply-chain provenance and assurance |
| S72 | [21st.dev community directory][S72] | Design expansion; reviewed 2026-09-18 |
| S73 | [Aceternity UI component directory][S73] | Design expansion; reviewed 2026-09-18 |
| S74 | [Magic UI component directory][S74] | Design expansion; reviewed 2026-09-18 |
| S75 | [React Bits official repository][S75] | Design expansion; reviewed 2026-09-18 |
| S76 | [Carbon motion][S76] | Design expansion; reviewed 2026-09-18 |
| S77 | [Fluent motion][S77] | Design expansion; reviewed 2026-09-18 |
| S78 | [Motion accessibility][S78] | Design expansion; reviewed 2026-09-18 |
| S79 | [WCAG animation from interactions (AAA)][S79] | Design expansion; reviewed 2026-09-18 |
| S80 | [WCAG pause stop hide][S80] | Design expansion; reviewed 2026-09-18 |
| S81 | [MDN View Transition API][S81] | Design expansion; reviewed 2026-09-18 |
| S82 | [web.dev animation performance][S82] | Design expansion; reviewed 2026-09-18 |
| S83 | [Design Tokens Format 2025.10][S83] | Design expansion; reviewed 2026-09-18 |
| S84 | [React Bits license][S84] | Design expansion; reviewed 2026-09-18 |

[S01]: https://www.w3.org/TR/WCAG22/
[S02]: https://www.nngroup.com/articles/ten-usability-heuristics/
[S03]: https://www.nngroup.com/articles/aesthetic-usability-effect/
[S04]: https://www.nngroup.com/articles/fitts-law/
[S05]: https://lawsofux.com/hicks-law/
[S06]: https://www.nngroup.com/articles/gestalt-proximity/
[S07]: https://www.nngroup.com/articles/gestalt-similarity/
[S08]: https://www.nngroup.com/articles/common-region/
[S09]: https://www.nngroup.com/articles/progressive-disclosure/
[S10]: https://www.nngroup.com/articles/response-times-3-important-limits/
[S11]: https://research.google/pubs/measuring-the-user-experience-on-a-large-scale-user-centered-metrics-for-web-applications/
[S12]: https://www.ftc.gov/reports/bringing-dark-patterns-light
[S13]: https://helpx.adobe.com/in_hi/illustrator/how-to/experiment-with-color-combinations-hybrid.html
[S14]: https://developer.android.com/develop/ui/compose/designsystems/material3
[S15]: https://design-system.service.gov.uk/components/password-input/
[S16]: https://github.com/OWASP/ASVS
[S17]: https://mas.owasp.org/MASVS/
[S18]: https://pages.nist.gov/800-63-4/sp800-63b/authenticators/
[S19]: https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html
[S20]: https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
[S21]: https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html
[S22]: https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html
[S23]: https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html
[S24]: https://www.rfc-editor.org/rfc/rfc9700.html
[S25]: https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html
[S26]: https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html
[S27]: https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html
[S28]: https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html
[S29]: https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html
[S30]: https://www.w3.org/WAI/WCAG22/Understanding/accessible-authentication-minimum.html
[S31]: https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html
[S32]: https://www.w3.org/WAI/WCAG22/Understanding/reflow.html
[S33]: https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html
[S34]: https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast
[S35]: https://www.w3.org/WAI/WCAG22/Understanding/text-spacing.html
[S36]: https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/At-rules/@media/prefers-color-scheme
[S37]: https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Properties/color-scheme
[S38]: https://web.dev/articles/responsive-web-design-basics
[S39]: https://developer.apple.com/design/tips/
[S40]: https://developer.android.com/guide/topics/ui/accessibility/apps
[S41]: https://www.w3.org/WAI/tutorials/forms/
[S42]: https://www.w3.org/WAI/ARIA/apg/patterns/dialog-modal/
[S43]: https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html
[S44]: https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html
[S45]: https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html
[S46]: https://docs.stripe.com/webhooks
[S47]: https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/data-protection-principles/a-guide-to-the-data-protection-principles/
[S48]: https://csrc.nist.gov/pubs/sp/800/218/final
[S49]: https://web.dev/articles/vitals
[S50]: https://www.nngroup.com/articles/onboarding-tutorials/
[S51]: https://www.w3.org/International/quicktips/index.en
[S52]: https://developers.google.com/search/docs/fundamentals/seo-starter-guide
[S53]: https://sre.google/sre-book/service-level-objectives/
[S54]: https://cheatsheetseries.owasp.org/cheatsheets/LLM_Prompt_Injection_Prevention_Cheat_Sheet.html
[S55]: https://learn.chatgpt.com/docs/agent-configuration/agents-md
[S56]: https://www.nngroup.com/articles/user-journeys-vs-user-flows/
[S57]: https://design-system.service.gov.uk/patterns/check-answers/
[S58]: https://design-system.service.gov.uk/patterns/confirmation-pages/
[S59]: https://www.nngroup.com/articles/animation-purpose-ux/
[S60]: https://www.nngroup.com/articles/modal-nonmodal-dialog/
[S61]: https://developer.android.com/develop/ui/compose/components/bottom-sheets
[S62]: https://carbondesignsystem.com/patterns/notification-pattern/
[S63]: https://www.w3.org/WAI/ARIA/apg/patterns/alertdialog/
[S64]: https://www.w3.org/WAI/ARIA/apg/patterns/tooltip/
[S65]: https://www.w3.org/WAI/ARIA/apg/patterns/combobox/
[S66]: https://www.w3.org/WAI/ARIA/apg/patterns/menu-button/
[S67]: https://www.w3.org/WAI/WCAG22/Understanding/content-on-hover-or-focus.html
[S68]: https://design-system.service.gov.uk/components/notification-banner/
[S69]: https://www.nngroup.com/articles/minimize-cognitive-load/
[S70]: https://cheatsheetseries.owasp.org/cheatsheets/CI_CD_Security_Cheat_Sheet.html
[S71]: https://slsa.dev/spec/v1.2/

[S72]: https://21st.dev/community/components
[S73]: https://ui.aceternity.com/components
[S74]: https://magicui.design/docs/components
[S75]: https://github.com/DavidHDev/react-bits
[S76]: https://carbondesignsystem.com/elements/motion/overview/
[S77]: https://fluent2.microsoft.design/motion
[S78]: https://motion.dev/docs/react-accessibility
[S79]: https://www.w3.org/WAI/WCAG22/Understanding/animation-from-interactions.html
[S80]: https://www.w3.org/WAI/WCAG22/Understanding/pause-stop-hide.html
[S81]: https://developer.mozilla.org/en-US/docs/Web/API/View_Transition_API
[S82]: https://web.dev/articles/animations-guide
[S83]: https://www.designtokens.org/tr/2025.10/format/
[S84]: https://github.com/DavidHDev/react-bits/blob/main/LICENSE.md
