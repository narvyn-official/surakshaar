CREATE TABLE tutor_hour_budget (
  account TEXT NOT NULL,
  hour INTEGER NOT NULL,
  count INTEGER NOT NULL CHECK(count BETWEEN 1 AND 60),
  PRIMARY KEY(account,hour)
);
