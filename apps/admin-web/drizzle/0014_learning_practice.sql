-- Personal learning is distinct from centre certification and never read by its issuers.
CREATE TABLE learning_profile_accounts (
  worker_id TEXT PRIMARY KEY NOT NULL,
  account_id TEXT NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,
  bound_at INTEGER NOT NULL
);
CREATE TABLE learning_practice_heads (
  account_id TEXT NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,
  id TEXT NOT NULL,
  worker_id TEXT NOT NULL REFERENCES learning_profile_accounts(worker_id) ON DELETE CASCADE,
  course_id TEXT NOT NULL CHECK(course_id IN ('fire','first-aid')),
  content_version TEXT NOT NULL,
  digest TEXT NOT NULL,
  updated_at INTEGER NOT NULL,
  payload TEXT NOT NULL CHECK(json_valid(payload)),
  PRIMARY KEY(account_id,id)
);
CREATE INDEX learning_practice_profile_time ON learning_practice_heads(account_id,worker_id,updated_at DESC,id DESC);
CREATE TABLE learning_practice_snapshots (
  account_id TEXT NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,
  id TEXT NOT NULL,
  worker_id TEXT NOT NULL REFERENCES learning_profile_accounts(worker_id) ON DELETE CASCADE,
  course_id TEXT NOT NULL,
  content_version TEXT NOT NULL,
  digest TEXT NOT NULL,
  previous_digest TEXT,
  updated_at INTEGER NOT NULL,
  imported_at INTEGER NOT NULL,
  payload TEXT NOT NULL CHECK(json_valid(payload)),
  PRIMARY KEY(account_id,id,digest)
);
CREATE TRIGGER learning_practice_snapshot_guard BEFORE INSERT ON learning_practice_snapshots
WHEN NOT EXISTS(SELECT 1 FROM learning_practice_snapshots WHERE account_id=NEW.account_id AND id=NEW.id AND digest=NEW.digest)
BEGIN
  SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM learning_profile_accounts WHERE worker_id=NEW.worker_id AND account_id=NEW.account_id)
    THEN RAISE(ABORT,'learning-profile-account-conflict') END;
  SELECT CASE WHEN EXISTS(SELECT 1 FROM learner_links WHERE worker_id=NEW.worker_id AND user_id IS NOT NULL AND user_id<>NEW.account_id)
    THEN RAISE(ABORT,'learning-profile-account-conflict') END;
  SELECT CASE WHEN EXISTS(SELECT 1 FROM learning_practice_heads WHERE account_id=NEW.account_id AND id=NEW.id AND (worker_id<>NEW.worker_id OR course_id<>NEW.course_id OR content_version<>NEW.content_version))
    THEN RAISE(ABORT,'learning-record-identity-conflict') END;
  SELECT CASE WHEN EXISTS(SELECT 1 FROM learning_practice_heads WHERE account_id=NEW.account_id AND id=NEW.id AND (NEW.previous_digest IS NULL OR digest<>NEW.previous_digest))
    OR (NEW.previous_digest IS NOT NULL AND NOT EXISTS(SELECT 1 FROM learning_practice_heads WHERE account_id=NEW.account_id AND id=NEW.id))
    THEN RAISE(ABORT,'learning-record-stale-parent') END;
  SELECT CASE WHEN length(CAST(NEW.payload AS BLOB))>200000 OR (SELECT count(*) FROM learning_practice_snapshots WHERE account_id=NEW.account_id)>=10000
    OR COALESCE((SELECT sum(length(CAST(payload AS BLOB))) FROM learning_practice_snapshots WHERE account_id=NEW.account_id),0)+length(CAST(NEW.payload AS BLOB))>20000000
    THEN RAISE(ABORT,'learning-record-storage-limit') END;
END;
CREATE TRIGGER learning_practice_snapshot_apply AFTER INSERT ON learning_practice_snapshots
BEGIN
  INSERT INTO learning_practice_heads(account_id,id,worker_id,course_id,content_version,digest,updated_at,payload)
    VALUES(NEW.account_id,NEW.id,NEW.worker_id,NEW.course_id,NEW.content_version,NEW.digest,NEW.updated_at,NEW.payload)
    ON CONFLICT(account_id,id) DO UPDATE SET digest=excluded.digest,updated_at=excluded.updated_at,payload=excluded.payload;
END;
CREATE TRIGGER learning_practice_snapshot_immutable BEFORE UPDATE ON learning_practice_snapshots
BEGIN SELECT RAISE(ABORT,'learning-snapshots-are-immutable'); END;

CREATE TABLE learning_sync_budget (
  account_id TEXT NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,
  minute INTEGER NOT NULL,
  count INTEGER NOT NULL CHECK(count BETWEEN 1 AND 20),
  PRIMARY KEY(account_id,minute)
);
