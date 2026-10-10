# Home and Learn navigation

Home is the selected learner's starting point: it offers one valid saved practice to continue, completed/in-progress counts, the latest completed practice and an account entry. Learn is the course catalog. The earlier industrial library remains under Learn. Home no longer repeats the catalog cards.

## Behavior and boundaries

- Continuation validates the selected learner, current content and saved engine state. The newest usable decision, casualty or Fire room practice wins. Room resumes preserve its original presentation, scenario and coaching mode; casualty resumes use the exact attempt ID.
- School context does not offer adult casualty or Fire room continuation. The awareness scenario remains available. Viewing Home never changes a practice result or creates certification evidence.
- Account entry explicitly opens the account destination. A saved local session is described as a saved sign-in, not a verified server connection. Offline learning remains available to guests.
- Home, Learn and Progress have distinct icon/label navigation and selected state. Returning from the industrial library goes to Learn. Recreation preserves the current destination and history.
- Short wide screens use a two-column Home and side-by-side courses, with horizontal icon/label navigation. Redundant headings are omitted in that layout. Controls stay native and at least 48dp. Larger content uses explicit pages without swipe scrolling; the Android 10–12 Back fallback now returns to the previous Home content page before leaving the app.

## Verification

The new synthetic-only `HomeLearningNavigationTest` covers bilingual tab separation, profile isolation, resume destination/exact attempt, invalid old-content exclusion, school restrictions, account destination, recreation, Back and label/target reachability. Tests restore the original learner/language/font settings and delete only their own fixture records.

Initial normal and 200% portrait runs each passed 3 tests. The first 200% landscape screenshot review identified an inefficient headings-only first page; this led to the adaptive layout above. A provisional combined build then passed the strengthened landscape case and both Hindi views fitted on one screen. The broader original navigation batch exposed two stale test assumptions about a header Back button on Learn; those tests now use system Back at that top-level destination.

Final app SHA-256: `0a2877b11d9424232d36d6733ec56b0e208808053b490185929c23b346db19c8`.
Final Android test APK: `ffee52e585513dde25ebc5e23de1c8d2a41495f61163240a0895c2157e564bb8`.

| Native check | Result |
| --- | --- |
| Home fixtures at 200% landscape, including all completed-state actions visible before paging | 3/3 passed, 12.615s |
| Standard Home, original Main navigation and system-Back history | 9/9 passed, 60.792s |
| Home fixtures at 200% portrait on the preceding candidate `ea86575c…` | 3/3 passed, 11.106s; the final source change affected compact landscape only |

The final landscape captures were visually reviewed for empty, resumed and completed Home, and Hindi Learn. All relevant actions are visible without scrolling; the tested completed Home stays on one page. A screenshot-only clipping issue in the first compact version was fixed by shortening the completed-practice summary, and the test now asserts all three Home actions are visible before any reveal helper runs.

Fourteen final-candidate screenshots and exact instrumentation output are under `artifacts/surakshaar-0.14.0/validation/home/`, with screenshots in `final/`. Useful captures:

- `final/home-empty-en-portrait-1.0.png`
- `final/home-catalog-en-portrait-1.0.png`
- `final/home-empty-hi-landscape-2.0.png`
- `final/home-catalog-hi-landscape-2.0.png`
- `final/home-completed-landscape-2.0.png`

After testing, font scale was 1.0, rotation 0 and automatic rotation enabled. The original learner and language were restored and synthetic fixture rows removed. Emulator ownership passed to the account-test agent.

## Limits

These are Android emulator UI and persisted-state checks. They are not a physical AR tracking test, real TalkBack session, clinical review or production account-service validation.
