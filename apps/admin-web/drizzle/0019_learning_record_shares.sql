-- Explicit public metadata shares of private formative snapshots. No credential table is read or written.
CREATE TABLE learning_record_shares (
 account_id TEXT NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,
 id TEXT NOT NULL,worker_id TEXT NOT NULL,attempt_id TEXT NOT NULL,source_digest TEXT NOT NULL,
 token_hash TEXT NOT NULL UNIQUE CHECK(length(token_hash)=64),
 metadata TEXT NOT NULL CHECK(json_valid(metadata)),created_at INTEGER NOT NULL,expires_at INTEGER NOT NULL,revoked_at INTEGER,
 PRIMARY KEY(account_id,id),
 FOREIGN KEY(account_id,attempt_id,source_digest) REFERENCES learning_practice_snapshots(account_id,id,digest) ON DELETE CASCADE,
 CHECK(expires_at>created_at AND expires_at<=created_at+2592000000)
);
CREATE INDEX learning_record_shares_owner ON learning_record_shares(account_id,worker_id,attempt_id,created_at DESC);
CREATE TRIGGER learning_record_share_guard BEFORE INSERT ON learning_record_shares
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM learning_practice_snapshots s JOIN learning_profile_accounts p ON p.worker_id=s.worker_id AND p.account_id=s.account_id WHERE s.account_id=NEW.account_id AND s.id=NEW.attempt_id AND s.digest=NEW.source_digest AND s.worker_id=NEW.worker_id AND json_extract(s.payload,'$.finished')=1)
 THEN RAISE(ABORT,'learning-share-source-conflict') END;
 SELECT CASE WHEN EXISTS(SELECT 1 FROM learner_links WHERE worker_id=NEW.worker_id AND user_id IS NOT NULL AND user_id<>NEW.account_id)
 THEN RAISE(ABORT,'learning-share-source-conflict') END;
 SELECT CASE WHEN (SELECT count(*) FROM learning_record_shares WHERE account_id=NEW.account_id)>=2000 OR (SELECT count(*) FROM learning_record_shares WHERE account_id=NEW.account_id AND revoked_at IS NULL AND expires_at>NEW.created_at)>=20 OR (SELECT count(*) FROM learning_record_shares WHERE account_id=NEW.account_id AND attempt_id=NEW.attempt_id AND revoked_at IS NULL AND expires_at>NEW.created_at)>=5
 THEN RAISE(ABORT,'learning-share-limit') END;
END;
CREATE TRIGGER learning_record_share_immutable BEFORE UPDATE OF account_id,id,worker_id,attempt_id,source_digest,token_hash,metadata,created_at,expires_at ON learning_record_shares
BEGIN SELECT RAISE(ABORT,'learning-share-immutable'); END;
CREATE TRIGGER learning_record_share_revoke BEFORE UPDATE OF revoked_at ON learning_record_shares
WHEN OLD.revoked_at IS NOT NULL OR NEW.revoked_at IS NULL
BEGIN SELECT RAISE(ABORT,'learning-share-revocation-immutable'); END;
