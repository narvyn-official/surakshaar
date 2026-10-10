# Independent Node storage review

Reviewed the new Node/SQLite D1 adapter, runtime binding, migration runner and installed Drizzle D1 call path. The review used disposable local databases only; it did not operate the VPS or use existing account data.

## Verified

`apps/admin-web/tests/sqlite-runtime-review.test.ts` adds three isolated regression tests. Alongside the adapter's existing three tests, all six passed on Node 26.5.0. TypeScript and focused ESLint passed.

- Real BetterAuth and Drizzle use the real auth schema and all repository SQL migrations. Two account sessions resolve to their own users. Signing out one account invalidates its token while preserving the other session. Deleting the second account cascades its session.
- Real learning snapshot triggers reject a different account using a bound profile. A failure in the second batch statement rolls back the first snapshot and its derived head. Conditional audit inserts follow the preceding DML change count, including zero-row updates.
- The migration runner reapplies no existing migrations on its second run, makes a backup before modifying an existing database, creates the test database with mode 0600, rejects changed migration history and rolls back a partially executed failing migration.
- Existing tests cover bound SQL-like text, null values, duplicate ordered raw column names, foreign keys, foreign-database batch rejection, unsupported values and batch rollback.

## Findings

The initial Node engine floor admitted runtimes without `StatementSync.columns()` and `setReturnArrays()`. Node's documentation identifies these as available from 22.16.0, with `setReturnArrays` also introduced in 24.0.0. The deployment owner raised the supported runtime to 24.13.1 and added a startup preflight. [Node SQLite documentation](https://nodejs.org/api/sqlite.html#statementsetreturnarraysenabled)

The auth configuration reads `CF-Connecting-IP`. The deployment owner reports that the new proxy overwrites that value from the trusted connection address. That proxy state was not independently inspected by this review.

The adapter implements the application-used D1 subset. Two differences were reported to its owner: missing positional parameters become NULL under native SQLite, and read-only SELECT metadata may retain the preceding DML change count. Existing reviewed application calls use complete parameters and inspect change counts on DML, but these observations should not be represented as full D1 compatibility. [D1 prepared statement API](https://developers.cloudflare.com/d1/worker-api/prepared-statements/)

## Limits

These checks establish local storage and session behavior. They do not establish internet availability, production mail delivery, load capacity, complete endpoint authorization coverage or a repository-wide security audit. The separate production smoke test and native account workflow must cover deployed routing and actual Android integration.
