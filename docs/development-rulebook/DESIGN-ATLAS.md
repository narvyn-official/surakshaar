# Design & Interaction Atlas

Version 2.0 · Research checked 18 September 2026 · Companion to the Development Rulebook

This is a curated design vocabulary and selection process, not an exhaustive copy of the internet. It covers all 78 top-level category labels observed in 21st.dev's community directory on the research date, with additional patterns for complete products. Category coverage is not an audit of every component in those categories. The pattern recipes and combinations are our original synthesis; source links identify places to explore or implementation guidance, not endorsements of every recipe.

Use the searchable **design-atlas.html**, the detailed **PATTERN-CATALOG.md**, the structured **design-atlas.json**, and the source register **DESIGN-SOURCES.md**. The main rulebook's chapters 37–40 define the required safeguards. Existing accessibility, security, workflow, and recovery rules still apply.

## 01 · Choose a direction before a component

Start with audience, task, brand, content, platform, constraints, and the experience we want people to remember. Use actual content early. Select one dominant visual language, a compatible supporting language, and a small number of distinctive moments. This is a house design method, not a psychological law.

For a substantial new interface, compare two or three coherent directions internally before implementation. Each direction should show a real key screen, a narrow layout, its typography and material choices, and one interaction. A collection of attractive isolated cards is not a design direction. Keep the user's existing brand unless a change is requested or justified.

Assess candidates on task clarity, brand relevance, distinctiveness, accessibility, responsive behavior, speed, content resilience, and maintenance. Rate each as strong, acceptable, weak, or unknown, with evidence. Accessibility barriers, unusable navigation, unclear licensing, and unsafe imports are rejection gates, even if the visual score is high. These gates apply to adoption, not to browsing an example for ideas.

Prefer a signature detail rooted in the product: a document's provenance unfolding into evidence, a property floor plan becoming a room list, or an inventory item revealing its movement history. Inventing an unfamiliar button convention rarely improves a routine task.

## 02 · Design families and composition

The catalogue contains 18 art directions. Their palette swatches communicate mood only; they are not approved foreground/background pairs. Build semantic tokens and test actual combinations in every state. A visual style and an appearance mode are different decisions: an editorial system can have both light and dark modes.

| Composition | Appropriate use | Adaptation |
|---|---|---|
| Editorial opening | Strong story, expert service, portfolio | Large headline plus clear explanation; ensure reading order survives asymmetry. |
| Product demonstration | An unfamiliar workflow | Show a truthful before/action/after sequence; label sample data. |
| Split hero | Different strengths in text and image | Stack according to meaning on narrow screens; preserve the first action. |
| Modular grid | Several related capabilities | Vary size by importance; do not force unrelated content into equal tiles. |
| Evidence-led page | Trust-sensitive purchases | Place proof beside claims; link scope, date, and detailed evidence. |
| Chapter narrative | Complex explanation | Provide chapter links and readable content without motion. |
| Workspace shell | Repeated operational tasks | Stable navigation, page actions, list/detail relationship, quiet background. |
| Explorer | Search and comparison | Persistent query/filter context; result count and reset; accessible list alternative to maps. |
| Canvas + inspector | Spatial or visual editing | Preserve pan/zoom state; offer searchable objects and keyboard operations. |
| Guided flow | Consequential multi-step task | Back, draft, validation, review, submit, receipt, and resumption are part of the design. |

Marketing page starting sequence: value → product evidence → capabilities → substantiated proof → pricing/commitment → objections → next step. Reorder to fit the buying decision; this is not a fixed conversion formula. Add blog, resources, integrations, security, contact, legal, or comparison pages when they serve real needs. Do not hide essential costs in an accordion.

Operational screen starting structure: identity/navigation → page title/context → primary action → scoped filters → working content → selection/item actions → durable status. Responsive behavior may change presentation while preserving the task and permissions. Do not shrink a desktop dashboard until it fits.

## 03 · An interaction contract

Specify every substantial interaction using this compact record:

```text
Pattern / purpose / affected role:
Entry: pointer, keyboard, touch, route, notification, or system event
Before → pending → committed / failed / cancelled:
Visible feedback and accessible announcement:
Focus, scroll, history, selection, and draft behavior:
Interruption: repeated action, fast navigation, stale response, offline
Exit, undo, retry, recovery, and backend authority:
Narrow layout / coarse pointer / reduced motion / static fallback:
Budget and evidence: actual device, build, measurements, remaining gaps
```

### Micro-interaction vocabulary

Use press feedback, selection indication, inline validation, save acknowledgement, copy feedback, upload progress, reversible dismissal, keyboard focus, drag pickup/drop feedback, and meaningful completion. The action's truth comes from its actual state; animation cannot make a failed request look successful.

Use pointer spotlights, subtle tilt, magnetic ornament, border tracing, icon morphs, number transitions, and haptic/sound cues selectively. Keep the actual hit target stable. Never remove the system cursor or require hover to discover an essential action. Haptics require platform support; sound must be optional and context appropriate.

### Macro-interaction vocabulary

Use list-to-detail continuity, master/detail panes, staged onboarding, map/list coordination, batch review, comparison workspaces, responsive drawers, full-screen mobile editors, guided configuration, draft resumption, and background jobs with durable destinations. These often improve a product more than decorative hover effects.

For route or shared-element transitions, update real application state, history, page title, and focus deliberately. Animation is progressive enhancement. A detail screen must still work as a direct URL. Rapid navigation and interruption must settle to the latest legitimate state rather than leave a ghost overlay.

### Motion starting values: house defaults

| Purpose | Starting point | Adaptation |
|---|---|---|
| Press/focus feedback | 80–140 ms | Feedback starts immediately; don't delay the action. |
| Small state change | 140–220 ms | Minimize distance and repeated bounce. |
| Panel/container transition | 200–320 ms | Follow distance, device, and platform conventions. |
| Deliberate story moment | 300–600 ms | Skip or reduce for repeated visits and task-heavy contexts. |
| Stagger | 20–40 ms offsets, short total sequence | Don't accumulate delay across long lists. |

These are tunable house ranges, not standards or research guarantees. Respect existing platform tokens first. Use ease-out for entering attention, a coherent in/out curve for repositioning, and restrained springs where physical continuity helps. Excessive bounce can undermine precision. Design a static or short-fade alternative, not merely a slightly slower version of a large movement.

Motion and GSAP document preference-aware implementations; MDN documents view transitions. CSS transforms and opacity are often a useful first choice for efficient animation, but actual rendering cost must be measured. See D19–D23 in the source register. WCAG 2.2.2 has specific conditions for pausing automatically moving or updating content; WCAG 2.3.3 is AAA, while our reduced-motion baseline is a house requirement. Do not conflate the two. See D26–D27.

## 04 · Art, backgrounds, sections, and separators

Choose an image grammar: subject, crop, lighting, viewpoint, texture, palette, and semantic purpose. Maintain it across photography, illustration, screenshots, and 3D. Art direction should survive replacement with actual content. Commission or create original assets where identity matters; keep provenance and rights. Never imply that a synthetic screenshot, testimonial portrait, certificate, or scene documents a real customer or event.

Background options range from solid/tonal surfaces, fine grids, dots, paper grain, contour lines, topographic maps, mesh gradients, layered silhouettes, halftone, ASCII, and diagram fields to bounded shader/particle scenes. Prefer an exported static asset when it expresses the same idea. Place text on predictable contrast surfaces; a beautiful moving gradient is not a readable surface by default.

Separator choices include whitespace, a hairline, color-zone change, small section number, typographic interlude, wave, diagonal cut, stepped edge, torn-paper edge, contour bridge, and a connector path between related ideas. Decorative edges are not semantic headings. Reserve sufficient vertical spacing so diagonal or wave cuts do not collide with content on narrow screens. Avoid animating every boundary.

Use progressive detail: a calm first paint, optional richer content when useful, and a lightweight fallback when the effect is unavailable. A WebGL scene needs a meaningful poster/DOM alternative, resource cleanup, context-loss handling, and a bounded resolution. Stop offscreen work. Keep decorative layers pointer-inert and hidden from assistive technology; informative visuals need accessible equivalents.

## 05 · Theme architecture beyond a dark toggle

Define foundation → semantic → component tokens. Useful semantic roles include canvas, surface, elevated surface, text, muted text, border, focus, accent, on-accent, selection, danger, warning, success, and information. Add spacing, typography, radius, shadow, density, duration, and easing tokens. Components consume roles rather than naming a hard-coded blue.

Keep brand, appearance (light/dark/system), density, and motion as independent axes where users benefit. Do not build every possible combination without a need. High contrast/forced-colors support and text resizing are behaviors to verify, not just extra palettes. Resolve the stored preference before first paint where feasible and handle preference changes. Use platform-safe fallbacks.

Dark mode needs its own surface hierarchy, subdued decorative brightness, adjusted imagery, and tested chart colors. It is not an inversion filter. Avoid relying on shadows alone to distinguish elevated surfaces. Translucent glass must be legible over all permitted backgrounds, with opaque fallbacks; a single screenshot is insufficient evidence.

Use fluid type/spacing within readable bounds, and test long translated strings. Use tabular numerals for aligned numeric comparisons when the selected font supports them. Font licensing, loading, script coverage, fallbacks, and layout shift are part of typography selection. D30–D34 provide tools and token guidance, not automatic accessibility approval.

## 06 · Recipes for different products

| Product | Coherent starting combination | Signature opportunity | Keep restrained |
|---|---|---|---|
| Operations / property management | Calm workspace, exception queue, saved views, detail pane | A room or record opens into its activity/evidence timeline | Decorative backgrounds behind tables; repeated route spectacles |
| Evidence / inventory / traceability | Technical editorial public pages, structured workspace, provenance timeline | Trace an object's verified handoffs with a static list equivalent | Decorative maps implying actual locations; fabricated live activity |
| Consumer commerce | Product photography, variant picker, comparison, cart drawer | Material/color preview with clear availability and price updates | Motion near checkout, unrequested upsells, false scarcity |
| Professional service | Warm editorial type, projects, process, real people | A case study that progressively reveals decisions and outcomes | Generic stock illustrations and unsupported proof |
| Developer tool | Technical diagrams, real code examples, command palette | Interactive request/response explanation with editable safe examples | Fake terminal commands, animated text that cannot be copied |
| Education | Humanist layout, step progression, worked examples | Manipulable diagram with verbal explanation and reset | Punitive streaks and decorative reward loops |
| Creative portfolio | Asymmetric editorial grid, art direction, contextual case studies | One memorable gallery-to-story transition | Cursor replacement and scroll interception |
| Public / essential service | Familiar forms, check answers, clear progress and receipt | Excellent recovery and plain language | Experimental interaction conventions |

These are unimplemented recipes, not audits or change approvals for PGonTrack, Veristow, or any other application.

## 07 · From inspiration to a maintained component

1. Select the task and a coherent direction. Browse the relevant atlas entries and linked primary sources; avoid searching randomly for effects.
2. Inspect the actual chosen demo and source. Check author, exact version, license, dependencies, remote assets, trackers, dynamic HTML, SVG, styles, and event listeners. Registry installation commands may execute third-party code; inspect before running.
3. Translate into our project's semantics and tokens. Prefer existing native controls or accessible primitives. Do not add a second framework or animation engine for a small effect without a clear benefit.
4. Build the static baseline and full data states first. Then add the selected enhancement. Give each imported component an owner and a path for updates.
5. Verify keyboard/touch, focus, screen-reader output where relevant, text zoom, theme/contrast, reduced motion, narrow/short screens, rendering and cleanup, real content, offline/error, and interrupted actions. Measure the affected journey against its baseline.
6. Keep the source/version/license record and actual evidence. Label inspiration, inspected source, adapted component, and verified integration as different maturity levels. Retire patterns that underperform or become unsafe.

### Adoption record

```text
Project / task / pattern IDs / owner:
Source URL / author / version or commit / reviewed date:
Rights: code license + assets + fonts + attribution + redistribution limits
Dependencies / remote requests / security findings:
Changed semantics / tokens / responsive behavior / fallbacks:
Acceptance checks / actual evidence / unresolved issues:
Update or replacement plan:
```

Do not copy a competitor's distinctive brand, proprietary assets, or whole page. Inspiration does not convey reuse permission. Free access, public code, a registry listing, and a permissive license are different facts. For example, React Bits currently includes a Commons Clause restriction; unDraw has restrictions beyond ordinary project use. Check the exact selected material at adoption. Nothing from those libraries is redistributed in this atlas.

## 08 · Maintenance and honest scope

This research covers public directories, selected documentation, and source/license pages. Some sources needed a repository or search fallback. Paid libraries and individual demos were not comprehensively accessed or tested. Awwwards, Refero, Rive accessibility, the initial Carbon URL, Motion Primitives, and the Three.js responsive page were unavailable or not sufficiently readable in this pass; no implementation claim depends on them. Carbon's current motion page was found and read separately.

Recheck a chosen source before implementation and review the atlas when a new project needs a different platform or design family. Do not auto-install packages, scrape galleries continuously, purchase assets, or modify apps simply because this library exists. The installed skill selects relevant references automatically during authorized design/development work on this Mac.
