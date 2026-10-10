-- Identity evidence is separate from credentials and personal formative records.
CREATE TABLE identity_policies (
 owner TEXT PRIMARY KEY REFERENCES training_centres(owner),enabled INTEGER NOT NULL CHECK(enabled IN(0,1)),
 notice TEXT NOT NULL,support TEXT NOT NULL,retention_days INTEGER NOT NULL CHECK(retention_days BETWEEN 1 AND 30),
 revision INTEGER NOT NULL,updated_at INTEGER NOT NULL,updated_by TEXT NOT NULL
);
CREATE TABLE identity_sessions (
 id TEXT PRIMARY KEY,owner TEXT NOT NULL,worker_id TEXT NOT NULL,account_id TEXT NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,
 assignment_id TEXT NOT NULL,scenario_id TEXT NOT NULL,policy_revision INTEGER NOT NULL,notice TEXT NOT NULL,support TEXT NOT NULL,
 status TEXT NOT NULL CHECK(status IN('active','submitted','interrupted','withdrawn','accepted','needs-review')),
 practice TEXT NOT NULL CHECK(json_valid(practice)),revision INTEGER NOT NULL DEFAULT 0,
 challenge TEXT,challenge_at INTEGER NOT NULL,frames INTEGER NOT NULL DEFAULT 0,started_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,expires_at INTEGER NOT NULL,
 reviewed_by TEXT,reviewed_at INTEGER,review_note TEXT,appeal TEXT,
 FOREIGN KEY(owner,worker_id) REFERENCES workers(owner,id),FOREIGN KEY(owner,assignment_id) REFERENCES learning_course_assignments(owner,id)
);
CREATE INDEX identity_session_account ON identity_sessions(account_id,started_at DESC);
CREATE INDEX identity_session_owner ON identity_sessions(owner,started_at DESC);
CREATE TABLE identity_frames (
 id TEXT PRIMARY KEY,session_id TEXT NOT NULL REFERENCES identity_sessions(id) ON DELETE CASCADE,
 sequence INTEGER NOT NULL,received_at INTEGER NOT NULL,client_at INTEGER NOT NULL,ciphertext TEXT NOT NULL,
 UNIQUE(session_id,sequence)
);
CREATE INDEX identity_expiry ON identity_sessions(expires_at);
