-- Additive only: leaves issuer trust, certification policy and historical evidence untouched.
CREATE TABLE IF NOT EXISTS tutor_budget (
  account TEXT NOT NULL,
  minute INTEGER NOT NULL,
  count INTEGER NOT NULL CHECK(count BETWEEN 1 AND 10),
  PRIMARY KEY(account,minute)
);
