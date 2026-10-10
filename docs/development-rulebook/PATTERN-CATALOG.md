# Pattern catalogue

Version 2.0 · 18 September 2026

120 original house recipes; 78 mapped to the observed 21st.dev directory categories, plus 42 extended patterns. These are design options, not mandatory features or tested integrations. Cost labels are relative implementation/verification estimates, not benchmarks. Use with the Design Atlas and rulebook chapters 37–40. Source links are discovery or foundation references; they do not assert that a source implements the complete recipe.

## Marketing blocks

### P001 · Quiet release strip

**Category:** Announcements · **Estimated effort:** Low

**Variants:** Dismissible update; maintenance notice; inline launch badge

**Use when:** An update affects the current audience or task.

**Acceptance concern:** Date the message; persist sensible dismissal; avoid blocking essential controls.

**Fallback:** Inline text notice with a detail link.

**Explore:** [Shadcnblocks](https://www.shadcnblocks.com/) · **Maturity:** Original recipe; integration not tested

### P002 · Textured technical illustration

**Category:** ASCII Art · **Estimated effort:** Medium

**Variants:** Monospace silhouette; dithered product; terminal backdrop

**Use when:** Developer/editorial identity benefits from a distinctive illustration.

**Acceptance concern:** Keep real headings in HTML; decorative glyphs must not flood screen readers.

**Fallback:** Static image with appropriate alternative text.

**Explore:** [React Bits](https://github.com/DavidHDev/react-bits) · **Maturity:** Original recipe; integration not tested

### P003 · Layered atmosphere

**Category:** Backgrounds · **Estimated effort:** Medium

**Variants:** Grid; grain; contour; aurora; silhouette

**Use when:** A public page needs depth without competing with content.

**Acceptance concern:** Keep text over a stable surface; bound rendering and contrast.

**Fallback:** Solid tone or compressed static asset.

**Explore:** [Aceternity UI](https://ui.aceternity.com/components) · **Maturity:** Original recipe; integration not tested

### P004 · Purposeful edge treatment

**Category:** Borders · **Estimated effort:** Low

**Variants:** Hairline; inset frame; corner marks; tracing accent

**Use when:** Separate related regions or emphasize one object.

**Acceptance concern:** Do not confuse decoration with focus or selection; keep hit areas intact.

**Fallback:** Stable outline with explicit state label.

**Explore:** [Uiverse](https://uiverse.io/) · **Maturity:** Original recipe; integration not tested

### P005 · Decision-focused action panel

**Category:** Calls to Action · **Estimated effort:** Low

**Variants:** Inline action; closing band; contextual next step

**Use when:** The visitor has enough information to choose.

**Acceptance concern:** State consequence and commitment; avoid repetitive competing primary actions.

**Fallback:** Plain action link or button near explanation.

**Explore:** [Tailark](https://tailark.com/) · **Maturity:** Original recipe; integration not tested

### P006 · Credible customer evidence

**Category:** Clients · **Estimated effort:** Low

**Variants:** Logo row; sector groups; linked case studies

**Use when:** Relevant real customers establish context.

**Acceptance concern:** Use permissioned logos and accurate relationship descriptions.

**Fallback:** Named text list with case-study links.

**Explore:** [Tailark](https://tailark.com/) · **Maturity:** Original recipe; integration not tested

### P007 · Decision matrix

**Category:** Comparisons · **Estimated effort:** Medium

**Variants:** Feature table; before/after; side-by-side variants

**Use when:** Users must understand meaningful differences.

**Acceptance concern:** Use comparable units and dates; avoid hiding unfavorable facts.

**Fallback:** Stacked labeled comparisons.

**Explore:** [Shadcnblocks](https://www.shadcnblocks.com/) · **Maturity:** Original recipe; integration not tested

### P008 · Compact tool launcher

**Category:** Docks · **Estimated effort:** Medium

**Variants:** Floating app dock; contextual action shelf

**Use when:** A small familiar toolset needs frequent access.

**Acceptance concern:** Labels on keyboard/touch; no magnification required to hit targets.

**Fallback:** Stable toolbar or labeled navigation.

**Explore:** [Aceternity UI](https://ui.aceternity.com/components) · **Maturity:** Original recipe; integration not tested

### P009 · Objection-oriented answers

**Category:** FAQs · **Estimated effort:** Low

**Variants:** Grouped questions; search; linked answer sections

**Use when:** Common questions need concise discoverable explanations.

**Acceptance concern:** Keep essential commitments outside collapsed content.

**Fallback:** Expanded headings and answers.

**Explore:** [Tailark](https://tailark.com/) · **Maturity:** Original recipe; integration not tested

### P010 · Capability storytelling

**Category:** Features · **Estimated effort:** Low

**Variants:** Alternating media/text; modular proof; interactive demo

**Use when:** Explain how a capability changes a real workflow.

**Acceptance concern:** Pair claims with truthful examples; do not depend on hover-only content.

**Fallback:** Sequential text and screenshot blocks.

**Explore:** [Shadcnblocks](https://www.shadcnblocks.com/) · **Maturity:** Original recipe; integration not tested

### P011 · Useful destination index

**Category:** Footers · **Estimated effort:** Low

**Variants:** Grouped sitemap; support; compact utility bar

**Use when:** Users seek contact, policies, secondary routes, or reassurance.

**Acceptance concern:** Keep labels meaningful and current; avoid a link graveyard.

**Fallback:** Simple grouped link lists.

**Explore:** [Tailark](https://tailark.com/) · **Maturity:** Original recipe; integration not tested

### P012 · Contextual work explorer

**Category:** Galleries · **Estimated effort:** Medium

**Variants:** Editorial grid; filmstrip; masonry; lightbox

**Use when:** Visual work benefits from browsing and closer inspection.

**Acceptance concern:** Preserve reading order, captions, keyboard navigation, and return position.

**Fallback:** Regular image list with detail links.

**Explore:** [Codrops](https://tympanus.net/codrops/) · **Maturity:** Original recipe; integration not tested

### P013 · Controlled color field

**Category:** Gradients · **Estimated effort:** Medium

**Variants:** Radial spotlight; mesh; duotone; edge glow

**Use when:** Brand atmosphere or a subtle hierarchy cue is useful.

**Acceptance concern:** Test worst-case contrast; don't encode meaning only in a gradient.

**Fallback:** Flat semantic surface.

**Explore:** [Paper Shaders](https://shaders.paper.design/) · **Maturity:** Original recipe; integration not tested

### P014 · Specific opening statement

**Category:** Heroes · **Estimated effort:** Medium

**Variants:** Editorial type; split product; evidence-first; guided demo

**Use when:** The first screen must establish value and next action.

**Acceptance concern:** Keep explanation and action available before optional effects load.

**Fallback:** Text plus optimized product image.

**Explore:** [Tailark](https://tailark.com/) · **Maturity:** Original recipe; integration not tested

### P015 · Reusable interaction behavior

**Category:** Hooks · **Estimated effort:** Medium

**Variants:** Outside-click; resize; reduced-motion; visibility helper

**Use when:** Several components share real behavior.

**Acceptance concern:** Audit cleanup, stale closures, SSR assumptions, and event subscriptions.

**Fallback:** Native event behavior with minimal script.

**Explore:** [shadcn/ui](https://ui.shadcn.com/docs/components) · **Maturity:** Original recipe; integration not tested

### P016 · Art-directed media

**Category:** Images · **Estimated effort:** Low

**Variants:** Context crop; framed screenshot; annotated image; zoom

**Use when:** An image explains content or reinforces identity.

**Acceptance concern:** Provide useful alt text when informative; reserve dimensions; honor rights.

**Fallback:** Responsive static image with caption.

**Explore:** [Codrops](https://tympanus.net/codrops/) · **Maturity:** Original recipe; integration not tested

### P017 · Location-aware explorer

**Category:** Maps · **Estimated effort:** High

**Variants:** Result pins; selected route; region summary

**Use when:** Geography changes a user's decision.

**Acceptance concern:** Provide a synchronized list, address search, clear precision, and permission boundaries.

**Fallback:** Address/result list with links.

**Explore:** [Shadcnblocks](https://www.shadcnblocks.com/) · **Maturity:** Original recipe; integration not tested

### P018 · Optional moving texture

**Category:** Marquees · **Estimated effort:** Medium

**Variants:** Logo ribbon; phrase band; image rail

**Use when:** Nonessential repetition supports a brand moment.

**Acceptance concern:** Don't hide unique information in motion; offer pause when applicable.

**Fallback:** Static wrapped list.

**Explore:** [Magic UI](https://magicui.design/docs/components) · **Maturity:** Original recipe; integration not tested

### P019 · Predictable destination system

**Category:** Navigation Menus · **Estimated effort:** Medium

**Variants:** Simple header; grouped mega-menu; mobile drawer

**Use when:** Visitors need to understand where they can go.

**Acceptance concern:** Keep destination links as links; distinguish disclosure controls; preserve keyboard paths.

**Fallback:** Visible grouped navigation list.

**Explore:** [shadcn/ui](https://ui.shadcn.com/docs/components) · **Maturity:** Original recipe; integration not tested

### P020 · Honest plan chooser

**Category:** Pricing Sections · **Estimated effort:** Medium

**Variants:** Tier cards; usage estimate; comparison; billing selector

**Use when:** Users compare entitlement and total commitment.

**Acceptance concern:** Show currency, period, taxes/fees as applicable, limits, and renewal behavior.

**Fallback:** Readable plan list and comparison table.

**Explore:** [Shadcnblocks](https://www.shadcnblocks.com/) · **Maturity:** Original recipe; integration not tested

### P021 · Local overflow with orientation

**Category:** Scroll Areas · **Estimated effort:** Medium

**Variants:** Code viewport; activity log; bounded list

**Use when:** A bounded region genuinely improves context.

**Acceptance concern:** Avoid nested-scroll traps; show affordance and permit keyboard scrolling.

**Fallback:** Natural document flow.

**Explore:** [shadcn/ui](https://ui.shadcn.com/docs/components) · **Maturity:** Original recipe; integration not tested

### P022 · Bounded procedural art

**Category:** Shaders · **Estimated effort:** High

**Variants:** Fluid color; halftone; reflective material; light field

**Use when:** An optional visual moment warrants rendering cost.

**Acceptance concern:** Measure GPU use; handle context loss; keep content in DOM.

**Fallback:** Poster image or CSS approximation.

**Explore:** [Paper Shaders](https://shaders.paper.design/) · **Maturity:** Original recipe; integration not tested

### P023 · Contextual outcome metrics

**Category:** Stats & KPIs · **Estimated effort:** Low

**Variants:** Metric strip; trend card; target comparison

**Use when:** A number can inform a decision with context.

**Acceptance concern:** Supply denominator, period, source, and meaning; label demo data.

**Fallback:** Static values with explanation.

**Explore:** [Shadcnblocks](https://www.shadcnblocks.com/) · **Maturity:** Original recipe; integration not tested

### P024 · Resumable task progression

**Category:** Steppers · **Estimated effort:** Medium

**Variants:** Labeled stages; check answers; edit previous step

**Use when:** A lengthy task has meaningful stages.

**Acceptance concern:** Preserve drafts and errors; distinguish current, complete, and skipped.

**Fallback:** Numbered sections and Back/Continue buttons.

**Explore:** [shadcn/ui](https://ui.shadcn.com/docs/components) · **Maturity:** Original recipe; integration not tested

### P025 · Real people and roles

**Category:** Team Sections · **Estimated effort:** Low

**Variants:** Portrait grid; expertise list; story cards

**Use when:** People's qualifications or responsibilities matter.

**Acceptance concern:** Use truthful names, roles, and permissioned images.

**Fallback:** Text biographies and contact paths.

**Explore:** [Tailark](https://tailark.com/) · **Maturity:** Original recipe; integration not tested

### P026 · Verifiable customer story

**Category:** Testimonials · **Estimated effort:** Low

**Variants:** Quote with context; case-study card; video evidence

**Use when:** A relevant experience supports an honest claim.

**Acceptance concern:** Get permission; state attribution; no fabricated reviews or misleading selection.

**Fallback:** Static quote with source/context.

**Explore:** [Tailark](https://tailark.com/) · **Maturity:** Original recipe; integration not tested

### P027 · Expressive type treatment

**Category:** Texts · **Estimated effort:** Medium

**Variants:** Editorial scale; highlight; reveal; typographic interlude

**Use when:** A brief headline or brand moment benefits.

**Acceptance concern:** Render complete accessible text; avoid delaying comprehension or duplicating spoken text.

**Fallback:** Plain visible text.

**Explore:** [React Bits](https://github.com/DavidHDev/react-bits) · **Maturity:** Original recipe; integration not tested

### P028 · Meaningful sequence

**Category:** Timelines · **Estimated effort:** Medium

**Variants:** Audit history; project phases; story milestones

**Use when:** Order and change over time explain the subject.

**Acceptance concern:** Label dates/time zones and pending states; avoid implying false causality.

**Fallback:** Chronological list.

**Explore:** [Shadcnblocks](https://www.shadcnblocks.com/) · **Maturity:** Original recipe; integration not tested

### P029 · User-controlled demonstration

**Category:** Videos · **Estimated effort:** High

**Variants:** Product walkthrough; case study; ambient clip

**Use when:** Motion explains something a still cannot.

**Acceptance concern:** Captions/transcript when needed; controls; poster; no unexpected sound.

**Fallback:** Poster plus text explanation.

**Explore:** [Codrops](https://tympanus.net/codrops/) · **Maturity:** Original recipe; integration not tested

## UI components

### P030 · Progressive detail

**Category:** Accordions · **Estimated effort:** Low

**Variants:** Single section; independent sections; grouped detail

**Use when:** Users need optional detail without losing overview.

**Acceptance concern:** Give headings and controls clear names; don't conceal required decisions.

**Fallback:** Expanded sections.

**Explore:** [WAI ARIA patterns](https://www.w3.org/WAI/ARIA/apg/patterns/) · **Maturity:** Original recipe; integration not tested

### P031 · Controllable assistant workspace

**Category:** AI Chats · **Estimated effort:** High

**Variants:** Streaming response; sources; attachment tray; approval card

**Use when:** A conversation is useful for the actual task.

**Acceptance concern:** Show stop/retry/error, distinguish drafts from actions, and enforce backend permissions.

**Fallback:** Form-based task submission and durable result.

**Explore:** [shadcn/ui](https://ui.shadcn.com/docs/components) · **Maturity:** Original recipe; integration not tested

### P032 · Scoped important feedback

**Category:** Alerts · **Estimated effort:** Low

**Variants:** Inline error; page status; consequence warning

**Use when:** A relevant condition needs attention.

**Acceptance concern:** Match urgency to severity; offer resolution without repeated interruption.

**Fallback:** Persistent text near the affected task.

**Explore:** [shadcn/ui](https://ui.shadcn.com/docs/components) · **Maturity:** Original recipe; integration not tested

### P033 · Recognizable identity

**Category:** Avatars · **Estimated effort:** Low

**Variants:** Initials; portrait; small group; presence marker

**Use when:** Identity matters to collaboration or ownership.

**Acceptance concern:** Handle missing images and identical initials; label presence explicitly.

**Fallback:** Name and role text.

**Explore:** [shadcn/ui](https://ui.shadcn.com/docs/components) · **Maturity:** Original recipe; integration not tested

### P034 · Concise state label

**Category:** Badges · **Estimated effort:** Low

**Variants:** Status pill; count; qualification

**Use when:** A short label adds scannable context.

**Acceptance concern:** Don't rely on color; define ambiguous counts and states.

**Fallback:** Plain text label.

**Explore:** [shadcn/ui](https://ui.shadcn.com/docs/components) · **Maturity:** Original recipe; integration not tested

### P035 · Clear action with feedback

**Category:** Buttons · **Estimated effort:** Low

**Variants:** Solid primary; quiet secondary; icon+label; stateful

**Use when:** An operation changes something.

**Acceptance concern:** Keep labels and target stable; support pending/error; avoid nested interactive elements.

**Fallback:** Native button with visible state.

**Explore:** [Uiverse](https://uiverse.io/) · **Maturity:** Original recipe; integration not tested

### P036 · Temporal overview

**Category:** Calendars · **Estimated effort:** High

**Variants:** Month grid; agenda; resource schedule

**Use when:** Users compare events or availability in time.

**Acceptance concern:** Account for locale/time zone, keyboard operation, dense days, and conflicts.

**Fallback:** Agenda list.

**Explore:** [React Aria](https://react-aria.adobe.com/) · **Maturity:** Original recipe; integration not tested

### P037 · Object summary with hierarchy

**Category:** Cards · **Estimated effort:** Low

**Variants:** Editorial card; actionable record; media tile; expandable summary

**Use when:** An object merits a short preview before detail.

**Acceptance concern:** Make primary destination clear; preserve semantics for nested actions.

**Fallback:** Heading, summary, and explicit link.

**Explore:** [shadcn/ui](https://ui.shadcn.com/docs/components) · **Maturity:** Original recipe; integration not tested

### P038 · Optional sequential browsing

**Category:** Carousels · **Estimated effort:** Medium

**Variants:** Manual slides; filmstrip; snap rail

**Use when:** Space is limited and users benefit from sequence.

**Acceptance concern:** Expose controls and position; avoid trapping focus or auto-rotating essential content.

**Fallback:** Static list/grid.

**Explore:** [WAI ARIA patterns](https://www.w3.org/WAI/ARIA/apg/patterns/) · **Maturity:** Original recipe; integration not tested

### P039 · Decision-oriented visualization

**Category:** Charts & Data Viz · **Estimated effort:** High

**Variants:** Trend; distribution; comparison; small multiples

**Use when:** A relationship is easier to see graphically.

**Acceptance concern:** Label units, uncertainty, missingness, source and range; supply accessible data.

**Fallback:** Summary and data table.

**Explore:** [React Aria](https://react-aria.adobe.com/) · **Maturity:** Original recipe; integration not tested

### P040 · Independent selection

**Category:** Checkboxes · **Estimated effort:** Low

**Variants:** Single consent; multi-select; indeterminate parent

**Use when:** Zero or more independent choices apply.

**Acceptance concern:** Separate required consent from optional; handle mixed selection accurately.

**Fallback:** Labeled native checkbox.

**Explore:** [React Aria](https://react-aria.adobe.com/) · **Maturity:** Original recipe; integration not tested

### P041 · Optional pointer ornament

**Category:** Cursors · **Estimated effort:** High

**Variants:** Soft follower; contextual label; image preview

**Use when:** An expressive desktop scene gains context.

**Acceptance concern:** Keep system cursor and stable targets; no essential information follows pointer alone.

**Fallback:** Normal pointer and visible labels.

**Explore:** [React Bits](https://github.com/DavidHDev/react-bits) · **Maturity:** Original recipe; integration not tested

### P042 · Prioritized work overview

**Category:** Dashboards · **Estimated effort:** High

**Variants:** Exception queue; trends; role home; drill-down

**Use when:** Users need an overview to decide their next task.

**Acceptance concern:** Prioritize action over ornamental metrics; include freshness and scope.

**Fallback:** Ordered work list and concise summaries.

**Explore:** [Shadcnblocks](https://www.shadcnblocks.com/) · **Maturity:** Original recipe; integration not tested

### P043 · Flexible date entry

**Category:** Date Pickers · **Estimated effort:** High

**Variants:** Typed date; calendar popover; range; presets

**Use when:** People need to choose dates confidently.

**Acceptance concern:** Accept keyboard entry; specify locale and time zone; explain disabled dates.

**Fallback:** Validated text fields with format help.

**Explore:** [React Aria](https://react-aria.adobe.com/) · **Maturity:** Original recipe; integration not tested

### P044 · Focused bounded decision

**Category:** Dialogs / Modals · **Estimated effort:** Medium

**Variants:** Confirmation; small editor; critical acknowledgement

**Use when:** A short task requires blocking the background.

**Acceptance concern:** Define focus, dismissal, dirty state, and return target; avoid nested modals.

**Fallback:** Dedicated page or inline section.

**Explore:** [WAI ARIA patterns](https://www.w3.org/WAI/ARIA/apg/patterns/) · **Maturity:** Original recipe; integration not tested

### P045 · Compact disclosure

**Category:** Dropdowns · **Estimated effort:** Medium

**Variants:** Action menu; navigation disclosure; choice list

**Use when:** Hidden options are secondary and discoverable.

**Acceptance concern:** Choose correct semantics: actions, destinations, and value selection differ.

**Fallback:** Visible choices or native select.

**Explore:** [Radix Primitives](https://www.radix-ui.com/primitives/docs/overview/introduction) · **Maturity:** Original recipe; integration not tested

### P046 · Useful next step

**Category:** Empty States · **Estimated effort:** Low

**Variants:** First use; no matches; cleared queue; permission denial

**Use when:** The absence of content has a clear cause.

**Acceptance concern:** Explain the specific cause and offer relevant action; do not call an error empty.

**Fallback:** Concise message and action.

**Explore:** [coss ui](https://coss.com/ui) · **Maturity:** Original recipe; integration not tested

### P047 · Hierarchical resource explorer

**Category:** File Trees · **Estimated effort:** High

**Variants:** Nested folders; lazy branches; path breadcrumbs

**Use when:** Real hierarchy is useful for navigation.

**Acceptance concern:** Keep keyboard and selection behavior consistent; handle deep paths.

**Fallback:** Searchable flat list with path.

**Explore:** [React Aria](https://react-aria.adobe.com/) · **Maturity:** Original recipe; integration not tested

### P048 · Recoverable transfer

**Category:** File Uploads · **Estimated effort:** High

**Variants:** Drop zone; browse; queue; preview; resume

**Use when:** Users provide files or media.

**Acceptance concern:** Always include file picker; validate on server; show individual failure and cancellation.

**Fallback:** Native file input with status list.

**Explore:** [coss ui](https://coss.com/ui) · **Maturity:** Original recipe; integration not tested

### P049 · Task-shaped data collection

**Category:** Forms · **Estimated effort:** Medium

**Variants:** Single column; grouped fields; staged flow

**Use when:** Collect only the data needed for the outcome.

**Acceptance concern:** Persistent labels, useful errors, preserved values, and review for consequences.

**Fallback:** Semantic native form.

**Explore:** [React Aria](https://react-aria.adobe.com/) · **Maturity:** Original recipe; integration not tested

### P050 · Optional global story

**Category:** Globes · **Estimated effort:** High

**Variants:** Coverage globe; connected regions; orbital markers

**Use when:** A global narrative benefits from spatial context.

**Acceptance concern:** Don't use a decorative globe as sole evidence of coverage; provide region list.

**Fallback:** Map/image plus text regions.

**Explore:** [Magic UI](https://magicui.design/docs/components) · **Maturity:** Original recipe; integration not tested

### P051 · Hierarchical content modules

**Category:** Grids & Bento · **Estimated effort:** Medium

**Variants:** Asymmetric feature tiles; summary grid; editorial mosaic

**Use when:** Related content differs in importance and shape.

**Acceptance concern:** DOM order must match meaning; don't squeeze long content to preserve a shape.

**Fallback:** Single-column semantic sections.

**Explore:** [Shadcnblocks](https://www.shadcnblocks.com/) · **Maturity:** Original recipe; integration not tested

### P052 · Consistent visual shorthand

**Category:** Icons · **Estimated effort:** Low

**Variants:** Outline; filled; duotone; state pair

**Use when:** A familiar symbol improves scanning.

**Acceptance concern:** Label ambiguous/interactive icons; normalize stroke and optical size; preserve rights.

**Fallback:** Text labels.

**Explore:** [Lucide license](https://lucide.dev/license) · **Maturity:** Original recipe; integration not tested

### P053 · Clear editable values

**Category:** Inputs · **Estimated effort:** Low

**Variants:** Text; search; number; segmented verification

**Use when:** Users must enter and correct data.

**Acceptance concern:** Choose appropriate input type and autocomplete; allow paste; don't mask unexpectedly.

**Fallback:** Labeled native input.

**Explore:** [coss ui](https://coss.com/ui) · **Maturity:** Original recipe; integration not tested

### P054 · Predictable destinations

**Category:** Links · **Estimated effort:** Low

**Variants:** Inline; card destination; external resource

**Use when:** An action navigates to another resource.

**Acceptance concern:** Use meaningful text and valid URLs; distinguish downloads and consequential destinations.

**Fallback:** Underlined text link.

**Explore:** [shadcn/ui](https://ui.shadcn.com/docs/components) · **Maturity:** Original recipe; integration not tested

### P055 · Scannable collections

**Category:** Lists · **Estimated effort:** Low

**Variants:** Dense rows; media list; grouped activity

**Use when:** People compare or scan repeated objects.

**Acceptance concern:** Stable identity, clear sorting, useful truncation, and scoped item actions.

**Fallback:** Semantic list with detail links.

**Explore:** [coss ui](https://coss.com/ui) · **Maturity:** Original recipe; integration not tested

### P056 · Discoverable commands

**Category:** Menus · **Estimated effort:** Medium

**Variants:** Menu button; context menu; overflow actions

**Use when:** Secondary commands should remain reachable.

**Acceptance concern:** Context menu cannot be the only route; avoid hiding frequent tasks.

**Fallback:** Labeled toolbar/actions.

**Explore:** [WAI ARIA patterns](https://www.w3.org/WAI/ARIA/apg/patterns/) · **Maturity:** Original recipe; integration not tested

### P057 · Durable event inbox

**Category:** Notifications · **Estimated effort:** High

**Variants:** Grouped feed; unread filter; channel preferences

**Use when:** Events matter after a transient message disappears.

**Acceptance concern:** Deduplicate, deep-link to authorized context, and distinguish unread from unresolved.

**Fallback:** Activity list with timestamps.

**Explore:** [Shadcnblocks](https://www.shadcnblocks.com/) · **Maturity:** Original recipe; integration not tested

### P058 · Legible changing values

**Category:** Numbers · **Estimated effort:** Medium

**Variants:** Tabular values; gentle transition; unit suffix

**Use when:** Metrics update and comparison matters.

**Acceptance concern:** Render real final value immediately for assistive tech; don't animate critical balances misleadingly.

**Fallback:** Static number and timestamp.

**Explore:** [Magic UI](https://magicui.design/docs/components) · **Maturity:** Original recipe; integration not tested

### P059 · First useful outcome

**Category:** Onboarding · **Estimated effort:** Medium

**Variants:** Checklist; contextual hint; sample workspace

**Use when:** New users need help reaching value.

**Acceptance concern:** Skip optional steps; return later; avoid compulsory tours and fake completion.

**Fallback:** Clear first-task instructions.

**Explore:** [Mobbin](https://mobbin.com/) · **Maturity:** Original recipe; integration not tested

### P060 · Navigable result sets

**Category:** Paginations · **Estimated effort:** Medium

**Variants:** Page links; load more; cursor navigation

**Use when:** Result size exceeds a useful view.

**Acceptance concern:** Preserve filter/history state; show position and handle removed records.

**Fallback:** Next/previous links with context.

**Explore:** [shadcn/ui](https://ui.shadcn.com/docs/components) · **Maturity:** Original recipe; integration not tested

### P061 · Anchored supplemental work

**Category:** Popovers · **Estimated effort:** Medium

**Variants:** Filter editor; small help panel; quick preview

**Use when:** A bounded task benefits from staying beside its origin.

**Acceptance concern:** Handle collision, focus, outside click, and keyboard; use dialog semantics if modal.

**Fallback:** Inline panel.

**Explore:** [Base UI](https://base-ui.com/react/overview/quick-start) · **Maturity:** Original recipe; integration not tested

### P062 · Identity and account context

**Category:** Profiles · **Estimated effort:** Medium

**Variants:** Public bio; workspace role; editable account

**Use when:** Identity or preferences affect the task.

**Acceptance concern:** Separate public/private information; clearly scope account versus workspace edits.

**Fallback:** Labeled account page.

**Explore:** [Shadcnblocks](https://www.shadcnblocks.com/) · **Maturity:** Original recipe; integration not tested

### P063 · Truthful task status

**Category:** Progress · **Estimated effort:** Medium

**Variants:** Measured bar; stage list; background-job tracker

**Use when:** Work takes long enough to need status.

**Acceptance concern:** Distinguish measured and indeterminate progress; expose failure and cancellation.

**Fallback:** Text stage and durable job link.

**Explore:** [shadcn/ui](https://ui.shadcn.com/docs/components) · **Maturity:** Original recipe; integration not tested

### P064 · Mutually exclusive choice

**Category:** Radio Groups · **Estimated effort:** Low

**Variants:** Inline options; choice cards; segmented choice

**Use when:** Exactly one option in a known set applies.

**Acceptance concern:** Use real selection semantics and labels; no hidden preselected paid commitments.

**Fallback:** Native radio group.

**Explore:** [React Aria](https://react-aria.adobe.com/) · **Maturity:** Original recipe; integration not tested

### P065 · Find with context

**Category:** Search Bars · **Estimated effort:** High

**Variants:** Suggestions; scoped search; recent queries; command entry

**Use when:** Users know something about the destination.

**Acceptance concern:** Label scope; support empty/no-result/error states; don't leak recent sensitive queries.

**Fallback:** Submit-based search form.

**Explore:** [React Aria](https://react-aria.adobe.com/) · **Maturity:** Original recipe; integration not tested

### P066 · Compact value choice

**Category:** Selects · **Estimated effort:** Medium

**Variants:** Native select; searchable combobox; grouped options

**Use when:** A known set supplies a value.

**Acceptance concern:** Prefer simpler control when adequate; test long options, typing, and mobile.

**Fallback:** Native select or radio group.

**Explore:** [Base UI](https://base-ui.com/react/overview/quick-start) · **Maturity:** Original recipe; integration not tested

### P067 · Stable workspace orientation

**Category:** Sidebars · **Estimated effort:** Medium

**Variants:** Expanded navigation; compact rail; contextual inspector

**Use when:** Repeated work benefits from consistent context.

**Acceptance concern:** Current route and collapse state stay clear; small screens need reachable alternatives.

**Fallback:** Labeled mobile navigation/drawer.

**Explore:** [coss ui](https://coss.com/ui) · **Maturity:** Original recipe; integration not tested

### P068 · Trustworthy account entry

**Category:** Sign Ins · **Estimated effort:** High

**Variants:** Password/passkey; SSO; account chooser

**Use when:** Returning users need access with recovery.

**Acceptance concern:** Reveal/hide, autofill/paste, recovery, useful safe errors; avoid decorative friction.

**Fallback:** Straightforward labeled login form.

**Explore:** [Shadcnblocks](https://www.shadcnblocks.com/) · **Maturity:** Original recipe; integration not tested

### P069 · Minimal account creation

**Category:** Sign ups · **Estimated effort:** High

**Variants:** Invitation; progressive profile; SSO

**Use when:** An account is justified by the next task.

**Acceptance concern:** Explain benefits and required data; verify identity appropriately; preserve progress.

**Fallback:** Short form with explicit terms and next step.

**Explore:** [Shadcnblocks](https://www.shadcnblocks.com/) · **Maturity:** Original recipe; integration not tested

### P070 · Approximate range adjustment

**Category:** Sliders · **Estimated effort:** Medium

**Variants:** Single value; range; comparison scrubber

**Use when:** Continuous or approximate selection is natural.

**Acceptance concern:** Provide keyboard and precise numeric alternative where precision matters.

**Fallback:** Number input or presets.

**Explore:** [React Aria](https://react-aria.adobe.com/) · **Maturity:** Original recipe; integration not tested

### P071 · Bounded waiting cue

**Category:** Spinner Loaders · **Estimated effort:** Low

**Variants:** Inline spinner; skeleton; stage status

**Use when:** A short unknown duration needs feedback.

**Acceptance concern:** Don't imply determinate progress; expose slow/failure state rather than spinning forever.

**Fallback:** Text status with retry when available.

**Explore:** [shadcn/ui](https://ui.shadcn.com/docs/components) · **Maturity:** Original recipe; integration not tested

### P072 · Reliable structured comparison

**Category:** Tables · **Estimated effort:** High

**Variants:** Sortable table; sticky key column; selectable rows

**Use when:** Rows share attributes users compare.

**Acceptance concern:** Keep headers and meaning; selection/bulk actions respect permissions; scope sticky overflow.

**Fallback:** Accessible table or labeled record list.

**Explore:** [Shadcnblocks](https://www.shadcnblocks.com/) · **Maturity:** Original recipe; integration not tested

### P073 · Related views in one context

**Category:** Tabs · **Estimated effort:** Medium

**Variants:** Content panels; view switcher; vertical categories

**Use when:** Users switch between peer content panels.

**Acceptance concern:** Distinguish route links from tabs; implement keyboard state; preserve meaningful selection.

**Fallback:** Headed sections or ordinary navigation.

**Explore:** [WAI ARIA patterns](https://www.w3.org/WAI/ARIA/apg/patterns/) · **Maturity:** Original recipe; integration not tested

### P074 · Manageable descriptors

**Category:** Tags · **Estimated effort:** Low

**Variants:** Filter chips; removable labels; taxonomy

**Use when:** Descriptors help organize or narrow content.

**Acceptance concern:** Name remove actions; retain visible active-filter context and reset.

**Fallback:** Text labels with separate controls.

**Explore:** [coss ui](https://coss.com/ui) · **Maturity:** Original recipe; integration not tested

### P075 · Comfortable longer entry

**Category:** Text Areas · **Estimated effort:** Low

**Variants:** Auto-grow; preview; counter; draft

**Use when:** People compose meaningful text.

**Acceptance concern:** Don't resize disruptively; preserve drafts and errors; explain limits before loss.

**Fallback:** Labeled native textarea.

**Explore:** [coss ui](https://coss.com/ui) · **Maturity:** Original recipe; integration not tested

### P076 · Brief acknowledgement

**Category:** Toasts · **Estimated effort:** Medium

**Variants:** Saved; copied; reversible dismissal

**Use when:** Noncritical feedback can be transient.

**Acceptance concern:** Never sole record of critical failure; don't steal focus; give undo enough time or durable access.

**Fallback:** Inline status beside the action.

**Explore:** [shadcn/ui](https://ui.shadcn.com/docs/components) · **Maturity:** Original recipe; integration not tested

### P077 · Immediate binary setting

**Category:** Toggles · **Estimated effort:** Low

**Variants:** Switch; pressed tool; visibility choice

**Use when:** The user changes a clear two-state setting.

**Acceptance concern:** Label state and save failure; distinguish toggle buttons from switches and submit-based choices.

**Fallback:** Labeled checkbox with clear persistence.

**Explore:** [Base UI](https://base-ui.com/react/overview/quick-start) · **Maturity:** Original recipe; integration not tested

### P078 · Supplementary explanation

**Category:** Tooltips · **Estimated effort:** Low

**Variants:** Icon label; concise clarification; shortcut hint

**Use when:** Nonessential extra explanation aids discovery.

**Acceptance concern:** No interactive controls or indispensable instructions; support focus and dismissal.

**Fallback:** Visible help text.

**Explore:** [WAI ARIA patterns](https://www.w3.org/WAI/ARIA/apg/patterns/) · **Maturity:** Original recipe; integration not tested

## Art & surfaces

### P079 · Paper grain editorial

**Category:** Art & surfaces · **Estimated effort:** Low

**Variants:** Warm paper; restrained grain; ink frame

**Use when:** Human, crafted brands and long-form stories.

**Acceptance concern:** Keep texture subtle and away from fine text; avoid large raster downloads.

**Fallback:** Flat warm neutral.

**Explore:** [Haikei](https://haikei.app/) · **Maturity:** Original recipe; integration not tested

### P080 · Topographic storytelling

**Category:** Art & surfaces · **Estimated effort:** Medium

**Variants:** Contour fields; elevation bands; route motif

**Use when:** Place, systems, or provenance is part of the story.

**Acceptance concern:** Don't present invented contours as actual measured geography.

**Fallback:** Static line illustration.

**Explore:** [Haikei](https://haikei.app/) · **Maturity:** Original recipe; integration not tested

### P081 · Isometric system cutaway

**Category:** Art & surfaces · **Estimated effort:** High

**Variants:** Layered system; room cutaway; process stack

**Use when:** Explain relationships that photography cannot show.

**Acceptance concern:** Add a labeled diagram/list; don't make reading depend on rotation.

**Fallback:** Flat annotated diagram.

**Explore:** [Spline documentation](https://docs.spline.design/) · **Maturity:** Original recipe; integration not tested

### P082 · Collage with discipline

**Category:** Art & surfaces · **Estimated effort:** Medium

**Variants:** Cutout subjects; paper layers; editorial captions

**Use when:** A creative brand has a strong image collection.

**Acceptance concern:** Unify crop, lighting, and edge treatment; manage image rights.

**Fallback:** Single strong image and caption.

**Explore:** [Codrops](https://tympanus.net/codrops/) · **Maturity:** Original recipe; integration not tested

### P083 · Technical schematic art

**Category:** Art & surfaces · **Estimated effort:** Medium

**Variants:** Exploded diagram; node map; circuit field

**Use when:** A technical product needs an explanatory visual identity.

**Acceptance concern:** Separate real architecture from conceptual illustration; label abstraction.

**Fallback:** Static SVG plus prose.

**Explore:** [Codrops](https://tympanus.net/codrops/) · **Maturity:** Original recipe; integration not tested

### P084 · Glass over a controlled surface

**Category:** Art & surfaces · **Estimated effort:** High

**Variants:** Frosted toolbar; translucent preview; acrylic panel

**Use when:** Layer relationships need subtle depth.

**Acceptance concern:** Test all allowed backgrounds; border/focus remain visible without blur.

**Fallback:** Opaque elevated surface.

**Explore:** [Paper Shaders](https://shaders.paper.design/) · **Maturity:** Original recipe; integration not tested

### P085 · Clay and tactile objects

**Category:** Art & surfaces · **Estimated effort:** Medium

**Variants:** Soft volume; embossed illustration; material chips

**Use when:** Friendly creative/learning products benefit from tactile art.

**Acceptance concern:** Keep controls recognizable and contrasted; don't use shadow alone as a boundary.

**Fallback:** Flat illustration and explicit borders.

**Explore:** [Codrops](https://tympanus.net/codrops/) · **Maturity:** Original recipe; integration not tested

### P086 · Monochrome with semantic accent

**Category:** Art & surfaces · **Estimated effort:** Low

**Variants:** Grayscale structure; single brand accent; status palette

**Use when:** Dense products need quiet hierarchy.

**Acceptance concern:** Keep error/warning/success distinct from branding and understandable without color.

**Fallback:** Neutral UI with labeled states.

**Explore:** [Radix Colors](https://www.radix-ui.com/colors/docs/palette-composition/understanding-the-scale) · **Maturity:** Original recipe; integration not tested

## Separators

### P087 · Whitespace and rule

**Category:** Separators · **Estimated effort:** Low

**Variants:** Open gap; hairline; section number

**Use when:** Content needs breathing room and a clear break.

**Acceptance concern:** Spacing must represent grouping consistently, including on mobile.

**Fallback:** Simple heading and margin.

**Explore:** [21st separator example](https://21st.dev/community/components/base-ui/separator-1) · **Maturity:** Original recipe; integration not tested

### P088 · Wave or contour bridge

**Category:** Separators · **Estimated effort:** Low

**Variants:** Wave; layered curve; topographic transition

**Use when:** Related story sections need a thematic boundary.

**Acceptance concern:** Keep decorative geometry out of text; bound height at narrow widths.

**Fallback:** Flat color-zone boundary.

**Explore:** [Haikei](https://haikei.app/) · **Maturity:** Original recipe; integration not tested

### P089 · Architectural edge

**Category:** Separators · **Estimated effort:** Low

**Variants:** Diagonal cut; step; notch; corner reveal

**Use when:** A precise or industrial art direction needs rhythm.

**Acceptance concern:** No clipped copy or interactive areas; maintain real document order.

**Fallback:** Rectangular section.

**Explore:** [Haikei](https://haikei.app/) · **Maturity:** Original recipe; integration not tested

### P090 · Editorial interlude

**Category:** Separators · **Estimated effort:** Low

**Variants:** Large numeral; short quote; chapter label

**Use when:** A long narrative benefits from a meaningful pause.

**Acceptance concern:** Use actual heading hierarchy; avoid an empty full-screen obstruction.

**Fallback:** Normal subheading.

**Explore:** [Codrops](https://tympanus.net/codrops/) · **Maturity:** Original recipe; integration not tested

## Micro interactions

### P091 · Press and release feedback

**Category:** Micro interactions · **Estimated effort:** Low

**Variants:** Small scale; surface change; icon response

**Use when:** Confirm that an input was received.

**Acceptance concern:** Keep target stable and action immediate; preserve keyboard focus.

**Fallback:** Instant state change.

**Explore:** [Fluent 2 motion](https://fluent2.microsoft.design/motion) · **Maturity:** Original recipe; integration not tested

### P092 · Save with honest recovery

**Category:** Micro interactions · **Estimated effort:** Medium

**Variants:** Pending label; saved timestamp; retry

**Use when:** People edit data that can fail to persist.

**Acceptance concern:** Distinguish local draft and server save; avoid fake success animation.

**Fallback:** Visible save status and retry.

**Explore:** [Carbon motion](https://carbondesignsystem.com/elements/motion/overview/) · **Maturity:** Original recipe; integration not tested

### P093 · Reversible optimistic action

**Category:** Micro interactions · **Estimated effort:** High

**Variants:** Favorite; archive; local reorder

**Use when:** Low-risk frequent changes benefit from immediacy.

**Acceptance concern:** Track rollback and concurrent changes; never fake confirmed payment or permission change.

**Fallback:** Wait for confirmation and retain retry.

**Explore:** [shadcn/ui](https://ui.shadcn.com/docs/components) · **Maturity:** Original recipe; integration not tested

### P094 · Copy with confirmation

**Category:** Micro interactions · **Estimated effort:** Low

**Variants:** Copy reference; code; share link

**Use when:** Users transfer content to another tool.

**Acceptance concern:** Handle clipboard failure; confirm exact copied value type without exposing secrets.

**Fallback:** Selectable text and manual copy.

**Explore:** [coss ui](https://coss.com/ui) · **Maturity:** Original recipe; integration not tested

### P095 · Inline editable record

**Category:** Micro interactions · **Estimated effort:** Medium

**Variants:** Edit in place; confirm/cancel; draft indicator

**Use when:** Small repeated edits benefit from preserved context.

**Acceptance concern:** Define Enter/Escape, validation, dirty state, and stale-value conflicts.

**Fallback:** Dedicated edit form.

**Explore:** [coss ui](https://coss.com/ui) · **Maturity:** Original recipe; integration not tested

### P096 · Pointer light as ornament

**Category:** Micro interactions · **Estimated effort:** Medium

**Variants:** Soft spotlight; edge reflection; gentle tilt

**Use when:** A limited product/portfolio card invites exploration.

**Acceptance concern:** Never move the actual hit target; disable for coarse pointer and reduced motion.

**Fallback:** Static highlight.

**Explore:** [Aceternity UI](https://ui.aceternity.com/components) · **Maturity:** Original recipe; integration not tested

### P097 · Icon continuity

**Category:** Micro interactions · **Estimated effort:** Low

**Variants:** Play/pause morph; expand/collapse; save check

**Use when:** An icon transition reinforces actual state.

**Acceptance concern:** Keep label/state accessible; don't delay command execution.

**Fallback:** Instant icon and label change.

**Explore:** [Motion accessibility](https://motion.dev/docs/react-accessibility) · **Maturity:** Original recipe; integration not tested

### P098 · Purposeful completion moment

**Category:** Micro interactions · **Estimated effort:** Medium

**Variants:** Brief check; subtle burst; next-action reveal

**Use when:** A real meaningful milestone deserves acknowledgement.

**Acceptance concern:** Don't celebrate routine clicks or sensitive failures; no repeated blocking spectacle.

**Fallback:** Success text and next action.

**Explore:** [Fluent 2 motion](https://fluent2.microsoft.design/motion) · **Maturity:** Original recipe; integration not tested

### P099 · Drag with explicit alternatives

**Category:** Micro interactions · **Estimated effort:** High

**Variants:** Reorder list; kanban move; upload pickup

**Use when:** Spatial movement improves a real workflow.

**Acceptance concern:** Provide move buttons/menu and announce outcome; handle cancellation and permissions.

**Fallback:** Move-to action and list order controls.

**Explore:** [React Aria](https://react-aria.adobe.com/) · **Maturity:** Original recipe; integration not tested

### P100 · Density without losing meaning

**Category:** Micro interactions · **Estimated effort:** Medium

**Variants:** Compact/comfy rows; detail toggle

**Use when:** Experts need more information while others need space.

**Acceptance concern:** Keep target requirements and text readability; persist user choice appropriately.

**Fallback:** One readable density.

**Explore:** [Open Props](https://open-props.style/) · **Maturity:** Original recipe; integration not tested

## Macro interactions

### P101 · List-to-detail continuity

**Category:** Macro interactions · **Estimated effort:** High

**Variants:** Shared image; expanding summary; routed detail

**Use when:** Users benefit from seeing where an item went.

**Acceptance concern:** Deep links, back/scroll, focus, and rapid navigation must work without animation.

**Fallback:** Immediate routed detail.

**Explore:** [MDN View Transition API](https://developer.mozilla.org/en-US/docs/Web/API/View_Transition_API) · **Maturity:** Original recipe; integration not tested

### P102 · Master/detail workspace

**Category:** Macro interactions · **Estimated effort:** High

**Variants:** List beside detail; responsive inspector

**Use when:** Frequent scanning and inspection should preserve context.

**Acceptance concern:** Keep selected item and unsaved edits safe; avoid tiny panes.

**Fallback:** Separate list and detail routes.

**Explore:** [Mobbin](https://mobbin.com/) · **Maturity:** Original recipe; integration not tested

### P103 · Adaptive sheet to full page

**Category:** Macro interactions · **Estimated effort:** High

**Variants:** Compact quick action; tall sheet; mobile editor

**Use when:** The same task must suit different viewport heights.

**Acceptance concern:** No fixed half-screen assumption; handle keyboard, zoom, dismissal, draft.

**Fallback:** Dedicated full page.

**Explore:** [Base UI](https://base-ui.com/react/overview/quick-start) · **Maturity:** Original recipe; integration not tested

### P104 · Command palette with discovery

**Category:** Macro interactions · **Estimated effort:** High

**Variants:** Global search; recent objects; scoped commands

**Use when:** Experienced users benefit from fast task access.

**Acceptance concern:** Keep ordinary navigation; label scope and consequences; enforce permissions.

**Fallback:** Search page and visible menus.

**Explore:** [shadcn/ui](https://ui.shadcn.com/docs/components) · **Maturity:** Original recipe; integration not tested

### P105 · Filter-to-result continuity

**Category:** Macro interactions · **Estimated effort:** Medium

**Variants:** Applied chips; result count; URL state

**Use when:** Exploration should preserve context and shareability.

**Acceptance concern:** Cancel stale requests; keep focus stable; explain zero results.

**Fallback:** Submit filters with a result heading.

**Explore:** [React Aria](https://react-aria.adobe.com/) · **Maturity:** Original recipe; integration not tested

### P106 · Background job journey

**Category:** Macro interactions · **Estimated effort:** High

**Variants:** Queue; progress; notify; durable result

**Use when:** Long work should not block other tasks.

**Acceptance concern:** Expose cancellation limits, partial failure, retries, and notification preferences.

**Fallback:** Jobs page with status polling/manual refresh.

**Explore:** [Shadcnblocks](https://www.shadcnblocks.com/) · **Maturity:** Original recipe; integration not tested

### P107 · Draft-to-review-to-receipt

**Category:** Macro interactions · **Estimated effort:** High

**Variants:** Autosave; check answers; submit; confirmation

**Use when:** Consequential tasks need correction and proof.

**Acceptance concern:** Clarify committed versus draft data; deduplicate submission; recover session expiry.

**Fallback:** Explicit staged forms with save and receipt.

**Explore:** [Mobbin](https://mobbin.com/) · **Maturity:** Original recipe; integration not tested

### P108 · Progressive permission request

**Category:** Macro interactions · **Estimated effort:** Medium

**Variants:** Explain value; request at use; denied path

**Use when:** A permission is needed for a user-chosen feature.

**Acceptance concern:** Never gate unrelated features or repeatedly nag after denial.

**Fallback:** Manual entry or file picker where viable.

**Explore:** [Mobbin](https://mobbin.com/) · **Maturity:** Original recipe; integration not tested

### P109 · Comparison workspace

**Category:** Macro interactions · **Estimated effort:** High

**Variants:** Pin candidates; remove; aligned attributes

**Use when:** Users compare multiple options over time.

**Acceptance concern:** Preserve selection across exploration; cap complexity; label differences fairly.

**Fallback:** Side-by-side table or stacked summaries.

**Explore:** [Shadcnblocks](https://www.shadcnblocks.com/) · **Maturity:** Original recipe; integration not tested

### P110 · Coordinated map and list

**Category:** Macro interactions · **Estimated effort:** High

**Variants:** Select result; highlight pin; preserve viewport

**Use when:** Geographic search needs accessible task parity.

**Acceptance concern:** Avoid unexpected zoom/scroll on every hover; clarify location precision.

**Fallback:** Full-featured list.

**Explore:** [Mobbin](https://mobbin.com/) · **Maturity:** Original recipe; integration not tested

### P111 · Scroll narrative with escape

**Category:** Macro interactions · **Estimated effort:** High

**Variants:** Chapter progress; bounded sticky visual; reveal

**Use when:** A public explanation benefits from a sequence.

**Acceptance concern:** Keep native scroll, reading order, keyboard access, and a skip route.

**Fallback:** Fully visible linear story.

**Explore:** [GSAP matchMedia](https://gsap.com/docs/v3/GSAP/gsap.matchMedia()/) · **Maturity:** Original recipe; integration not tested

### P112 · Context-preserving view switch

**Category:** Macro interactions · **Estimated effort:** Medium

**Variants:** Grid/list; calendar/agenda; board/table

**Use when:** Different views answer different questions about the same data.

**Acceptance concern:** Retain filters, selection, sorting where meaningful; explain losses.

**Fallback:** Single reliable view.

**Explore:** [MDN View Transition API](https://developer.mozilla.org/en-US/docs/Web/API/View_Transition_API) · **Maturity:** Original recipe; integration not tested

### P113 · Collaborative presence and conflict

**Category:** Macro interactions · **Estimated effort:** High

**Variants:** Presence chips; edit locks; merge prompt

**Use when:** Concurrent work creates real coordination needs.

**Acceptance concern:** Presence isn't authorization; protect identity, handle disconnect, and expose conflict.

**Fallback:** Version check and explicit conflict resolution.

**Explore:** [Shadcnblocks](https://www.shadcnblocks.com/) · **Maturity:** Original recipe; integration not tested

### P114 · Undo history and recovery

**Category:** Macro interactions · **Estimated effort:** High

**Variants:** Recent changes; restore snapshot; reversible batch

**Use when:** Users make many edits or consequential mistakes.

**Acceptance concern:** State undo scope/time and nonreversible actions; check current permission on restore.

**Fallback:** Confirmation and recoverable version history.

**Explore:** [Mobbin](https://mobbin.com/) · **Maturity:** Original recipe; integration not tested

## Advanced art

### P115 · User-controlled 3D object

**Category:** Advanced art · **Estimated effort:** High

**Variants:** Rotate product; exploded view; material preview

**Use when:** Spatial inspection materially aids understanding.

**Acceptance concern:** Provide keyboard controls, reset, poster, real dimensions, and bounded rendering.

**Fallback:** Multi-angle images and specification table.

**Explore:** [Spline documentation](https://docs.spline.design/) · **Maturity:** Original recipe; integration not tested

### P116 · Generative identity system

**Category:** Advanced art · **Estimated effort:** High

**Variants:** Seeded motifs; data-shaped covers; constrained variation

**Use when:** A brand needs many related original visuals.

**Acceptance concern:** Keep seeds stable; don't encode sensitive data; ensure consistent crop/contrast.

**Fallback:** Pre-generated approved assets.

**Explore:** [Haikei](https://haikei.app/) · **Maturity:** Original recipe; integration not tested

### P117 · Image comparison reveal

**Category:** Advanced art · **Estimated effort:** Medium

**Variants:** Before/after slider; split image; toggle

**Use when:** Differences between two genuine states matter.

**Acceptance concern:** Label each state and date; include keyboard slider and side-by-side alternative.

**Fallback:** Two captioned images.

**Explore:** [Aceternity UI](https://ui.aceternity.com/components) · **Maturity:** Original recipe; integration not tested

### P118 · Kinetic headline with restraint

**Category:** Advanced art · **Estimated effort:** Medium

**Variants:** Word reveal; short scramble; underline draw

**Use when:** A short noncritical headline benefits from one entrance.

**Acceptance concern:** Full text available immediately; no repeated scramble, flashing, or layout jump.

**Fallback:** Static heading.

**Explore:** [React Bits](https://github.com/DavidHDev/react-bits) · **Maturity:** Original recipe; integration not tested

### P119 · Ambient shader vignette

**Category:** Advanced art · **Estimated effort:** High

**Variants:** Dithered glow; water-like field; material study

**Use when:** An optional brand scene is worth its resource cost.

**Acceptance concern:** No motion behind important reading; stop offscreen and on preference change.

**Fallback:** Static poster with same composition.

**Explore:** [Paper Shaders](https://shaders.paper.design/) · **Maturity:** Original recipe; integration not tested

### P120 · Diagram that explains itself

**Category:** Advanced art · **Estimated effort:** High

**Variants:** Step-highlighted nodes; selectable layers; playback

**Use when:** A process is clearer when readers choose a step.

**Acceptance concern:** Expose every step in text; pause/reset; no knowledge hidden in animation.

**Fallback:** Labeled static diagram and ordered explanation.

**Explore:** [Codrops](https://tympanus.net/codrops/) · **Maturity:** Original recipe; integration not tested

## Art directions

Mood swatches are not verified UI contrast pairs. Each direction needs semantic light/dark tokens and actual content checks.

### T01 · Editorial precision

**Fits:** Publishing, professional service, case studies

**Composition:** Strong type scale, deliberate asymmetry, captions, thin rules

**Palette:** Ink, ivory, one decisive accent · Mood swatches: #F6F2E9, #20241F, #A3432F

**Type and motion:** Serif display plus neutral sans; short fades

**Watch:** Avoid tiny captions and forced full-screen headlines.

### T02 · Calm workspace

**Fits:** Operational apps, admin, management

**Composition:** Stable shell, restrained cards, strong table hierarchy

**Palette:** Cool neutral surfaces and semantic state colors · Mood swatches: #F3F5F7, #172334, #275DA8

**Type and motion:** Readable sans, tabular numerals; immediate feedback

**Watch:** Avoid excessive panels and accent competition.

### T03 · Technical blueprint

**Fits:** Developer, infrastructure, systems

**Composition:** Grids, annotations, diagrammatic illustrations

**Palette:** Deep ink, cool paper, electric accent · Mood swatches: #EEF4FA, #152D43, #17616A

**Type and motion:** Sans body with limited mono; line/step reveals

**Watch:** Decorative schematics must not masquerade as actual architecture.

### T04 · Warm humanist

**Fits:** Education, hospitality, community

**Composition:** Generous rhythm, rounded but clear controls, real people

**Palette:** Cream, earthy accent, calm neutrals · Mood swatches: #FAF3E8, #322A23, #8D4D2C

**Type and motion:** Humanist sans; gentle short movement

**Watch:** Do not use warmth as an excuse for weak text contrast.

### T05 · Monochrome luxury

**Fits:** Premium products, architecture, portfolio

**Composition:** Large photography, quiet framing, disciplined whitespace

**Palette:** Black/white plus a restrained material color · Mood swatches: #F7F7F2, #181818, #856340

**Type and motion:** Elegant display face with robust body; sparse transitions

**Watch:** Keep prices, labels, and navigation fully legible.

### T06 · Bold typographic

**Fits:** Events, creative studios, campaigns

**Composition:** Oversized type, sharp contrast, clear blocks

**Palette:** One saturated field and controlled neutral · Mood swatches: #F5F021, #181A1B, #E9E7E0

**Type and motion:** Heavy display; restrained entry motion

**Watch:** Avoid all-caps paragraphs and clipped translated text.

### T07 · Structured brutalist

**Fits:** Culture, independent products, experimental brand

**Composition:** Visible rules, hard edges, explicit controls

**Palette:** Paper, ink, loud accent · Mood swatches: #F3EDE1, #1E211D, #B63827

**Type and motion:** Plain strong type; direct state changes

**Watch:** Preserve familiar control behavior and calm reading areas.

### T08 · Soft tactile

**Fits:** Learning, creative tools, friendly onboarding

**Composition:** Soft illustration, material hints, clear surface boundaries

**Palette:** Pastels used as surfaces, deep readable text · Mood swatches: #F0E7F6, #35263F, #765082

**Type and motion:** Rounded accents; restrained springs

**Watch:** Neumorphic low-contrast controls are not an acceptable default.

### T09 · Controlled glass

**Fits:** Media tools, spatial interfaces, polished overlays

**Composition:** Translucent chrome above controlled imagery

**Palette:** Neutral glass plus solid semantic layers · Mood swatches: #E6F0F4, #152F3B, #27748B

**Type and motion:** System-like sans; subtle elevation

**Watch:** Needs opaque fallback and tested contrast over every backdrop.

### T10 · Organic landscape

**Fits:** Sustainability, travel, place-led stories

**Composition:** Contours, natural textures, image-led sections

**Palette:** Stone, forest, restrained clay · Mood swatches: #F1EFE4, #25382A, #8B482B

**Type and motion:** Readable serif/sans pairing; minimal parallax

**Watch:** Do not imply environmental evidence through green styling alone.

### T11 · Retro digital

**Fits:** Games, creative developer brands

**Composition:** Pixel accents, dither, terminal motifs

**Palette:** Dark ink and limited bright accents · Mood swatches: #151E20, #E4F0D0, #94BF3F

**Type and motion:** Mono headlines only; optional short reveals

**Watch:** Avoid CRT flicker, illegible pixel body type, and fake console behavior.

### T12 · Scientific clarity

**Fits:** Research, data tools, evidence products

**Composition:** Annotations, confidence bands, compact comparisons

**Palette:** Neutral ground, deliberate categorical scales · Mood swatches: #F1F5F7, #1D2D37, #326C87

**Type and motion:** Clear sans and tabular data; calm updates

**Watch:** Show uncertainty and provenance, not just visual precision.

### T13 · Cinematic product

**Fits:** Visual products, launch pages

**Composition:** Large controlled media, sparse supporting copy

**Palette:** Dark stage, bright subject, limited accent · Mood swatches: #10141C, #EDEEF1, #BC6C36

**Type and motion:** Strong display; user-controlled media

**Watch:** Maintain lightweight first paint and meaningful posters.

### T14 · Playful modular

**Fits:** Learning, consumer discovery, creative teams

**Composition:** Color-coded modules, simple shapes, expressive illustration

**Palette:** Several controlled surface hues with dark text · Mood swatches: #FFF1CF, #2A2940, #6A4997

**Type and motion:** Friendly sans; occasional meaningful celebration

**Watch:** Do not turn every task into a game or every tile into a button.

### T15 · Architectural minimal

**Fits:** Real estate, built environment, professional portfolios

**Composition:** Precise grid, generous image ratios, measured rhythm

**Palette:** Limestone, graphite, restrained green · Mood swatches: #EDEBE3, #2F3331, #496054

**Type and motion:** Editorial headings; short geometry transitions

**Watch:** Keep essential metadata beside images and visible on mobile.

### T16 · Craft collage

**Fits:** Arts, community, independent brands

**Composition:** Cutouts, paper edges, annotated layers

**Palette:** Warm paper and a narrow ink palette · Mood swatches: #F4E4C8, #332C30, #9F3D37

**Type and motion:** Expressive headings, simple body; limited layered reveals

**Watch:** Control visual noise and asset provenance.

### T17 · Chromatic atmosphere

**Fits:** Creative technology, select marketing moments

**Composition:** Mesh fields, soft glow, stable foreground panels

**Palette:** Two or three adjacent hues with neutral text surfaces · Mood swatches: #1B2140, #DBE8F5, #8173D9

**Type and motion:** Clean type; static or slow optional atmosphere

**Watch:** Avoid glowing financial/admin forms and uncontrolled contrast.

### T18 · Accessible civic

**Fits:** Public services, high-stakes forms

**Composition:** Clear headings, explicit actions, predictable pages

**Palette:** High-contrast semantic roles, understated brand · Mood swatches: #F5F7FA, #16283D, #185594

**Type and motion:** Plain readable type; mostly instant state changes

**Watch:** Novelty must not raise the learning cost of essential tasks.
