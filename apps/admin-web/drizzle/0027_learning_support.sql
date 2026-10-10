CREATE TABLE learning_support_links (
 id TEXT PRIMARY KEY,account_id TEXT NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,worker_id TEXT NOT NULL,
 label TEXT NOT NULL,invited_email TEXT NOT NULL,supporter_id TEXT REFERENCES auth_user(id) ON DELETE CASCADE,
 invite_hash TEXT UNIQUE,invite_expires_at INTEGER,expires_at INTEGER NOT NULL,created_at INTEGER NOT NULL,revoked_at INTEGER,
 FOREIGN KEY(worker_id) REFERENCES learning_profile_accounts(worker_id) ON DELETE CASCADE
);
CREATE INDEX learning_support_owner ON learning_support_links(account_id,created_at DESC);
CREATE INDEX learning_support_viewer ON learning_support_links(supporter_id,expires_at);
