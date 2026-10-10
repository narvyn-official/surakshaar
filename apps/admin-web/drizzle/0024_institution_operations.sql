-- Additive institutional administration. Existing learner evidence stays unchanged.
CREATE TABLE institution_units (
 owner TEXT NOT NULL REFERENCES training_centres(owner), id TEXT NOT NULL, kind TEXT NOT NULL CHECK(kind IN('branch','department','cohort')),
 name TEXT NOT NULL, parent_id TEXT, archived_at INTEGER, created_at INTEGER NOT NULL,
 PRIMARY KEY(owner,id), UNIQUE(owner,kind,name), FOREIGN KEY(owner,parent_id) REFERENCES institution_units(owner,id)
);
CREATE TABLE institution_people (
 owner TEXT NOT NULL,worker_id TEXT NOT NULL,employee_number TEXT NOT NULL,unit_id TEXT,archived_at INTEGER,
 PRIMARY KEY(owner,worker_id),UNIQUE(owner,employee_number),FOREIGN KEY(owner,worker_id) REFERENCES workers(owner,id),FOREIGN KEY(owner,unit_id) REFERENCES institution_units(owner,id)
);
CREATE TABLE institution_policies (
 owner TEXT NOT NULL REFERENCES training_centres(owner),version INTEGER NOT NULL,body TEXT NOT NULL CHECK(json_valid(body)),
 created_by TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(owner,version)
);
CREATE TRIGGER institution_policy_immutable BEFORE UPDATE ON institution_policies BEGIN SELECT RAISE(ABORT,'institution-policy-immutable'); END;
CREATE TABLE institution_plans (
 owner TEXT PRIMARY KEY REFERENCES training_centres(owner),name TEXT NOT NULL,learner_limit INTEGER NOT NULL CHECK(learner_limit BETWEEN 1 AND 10000),
 valid_until INTEGER,restricted INTEGER NOT NULL DEFAULT 0,reason TEXT NOT NULL,updated_at INTEGER NOT NULL,updated_by TEXT NOT NULL
);
CREATE TABLE institution_imports (
 owner TEXT NOT NULL,id TEXT NOT NULL,digest TEXT NOT NULL,result TEXT NOT NULL CHECK(json_valid(result)),created_at INTEGER NOT NULL,
 PRIMARY KEY(owner,id)
);
CREATE UNIQUE INDEX institution_employee_number ON institution_people(owner,employee_number COLLATE NOCASE);
CREATE TRIGGER institution_learner_quota BEFORE INSERT ON workers
WHEN EXISTS(SELECT 1 FROM institution_plans p WHERE p.owner=NEW.owner AND (p.restricted=1 OR (p.valid_until IS NOT NULL AND p.valid_until<=unixepoch()*1000) OR p.learner_limit<=(SELECT COUNT(*) FROM workers w LEFT JOIN institution_people i ON i.owner=w.owner AND i.worker_id=w.id WHERE w.owner=NEW.owner AND i.archived_at IS NULL)))
BEGIN SELECT RAISE(ABORT,'institution-quota-reached'); END;

CREATE TABLE local_procedures (
 owner TEXT NOT NULL,id TEXT NOT NULL,title TEXT NOT NULL,body TEXT NOT NULL,sources TEXT NOT NULL,language TEXT NOT NULL,
 author_id TEXT NOT NULL,created_at INTEGER NOT NULL,status TEXT NOT NULL CHECK(status IN('draft','submitted','approved','rejected','withdrawn')),
 revision INTEGER NOT NULL DEFAULT 1,reviewer_id TEXT,reviewed_at INTEGER,review_note TEXT,qualification TEXT,
 PRIMARY KEY(owner,id),FOREIGN KEY(owner) REFERENCES training_centres(owner)
);
CREATE TABLE local_procedure_events (
 owner TEXT NOT NULL,id TEXT NOT NULL,procedure_id TEXT NOT NULL,actor TEXT NOT NULL,action TEXT NOT NULL,note TEXT NOT NULL,at INTEGER NOT NULL,
 PRIMARY KEY(owner,id),FOREIGN KEY(owner,procedure_id) REFERENCES local_procedures(owner,id)
);
CREATE TABLE identity_references (
 owner TEXT NOT NULL,id TEXT NOT NULL,worker_id TEXT NOT NULL,account_id TEXT NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,
 ciphertext TEXT NOT NULL,digest TEXT NOT NULL,status TEXT NOT NULL CHECK(status IN('pending','approved','rejected','replaced','withdrawn')),
 created_at INTEGER NOT NULL,expires_at INTEGER NOT NULL,reviewed_by TEXT,reviewed_at INTEGER,review_note TEXT,
 PRIMARY KEY(owner,id),FOREIGN KEY(owner,worker_id) REFERENCES workers(owner,id)
);
CREATE UNIQUE INDEX identity_reference_current ON identity_references(owner,worker_id) WHERE status='approved';
CREATE INDEX identity_reference_account ON identity_references(account_id,created_at DESC);
ALTER TABLE identity_sessions ADD COLUMN reference_id TEXT;
ALTER TABLE learning_course_assignments ADD COLUMN policy_version INTEGER;
CREATE TRIGGER institution_assignment_policy AFTER INSERT ON learning_course_assignments
BEGIN UPDATE learning_course_assignments SET policy_version=(SELECT MAX(version) FROM institution_policies WHERE owner=NEW.owner) WHERE owner=NEW.owner AND id=NEW.id; END;

-- Examiner roles may review formative submissions, but cannot assign courses or
-- edit learner records. Existing trainer feedback permission remains intact.
DROP TRIGGER launch_review_guard;
CREATE TRIGGER launch_review_guard BEFORE INSERT ON learning_review_events
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM learning_course_submissions s JOIN learner_links l ON l.owner=s.owner AND l.worker_id=s.worker_id AND l.user_id=s.account_id JOIN centre_approvals c ON c.owner=s.owner AND c.status='approved' WHERE s.owner=NEW.owner AND s.id=NEW.submission_id AND s.withdrawn_at IS NULL)
 THEN RAISE(ABORT,'launch-review-access-conflict') END;
 SELECT CASE WHEN NEW.outcome='correction-request' AND NOT EXISTS(SELECT 1 FROM learning_course_submissions WHERE owner=NEW.owner AND id=NEW.submission_id AND account_id=NEW.actor_id)
 THEN RAISE(ABORT,'launch-review-access-conflict') END;
 SELECT CASE WHEN NEW.outcome<>'correction-request' AND (EXISTS(SELECT 1 FROM learning_course_submissions WHERE owner=NEW.owner AND id=NEW.submission_id AND account_id=NEW.actor_id) OR (NEW.actor_id<>NEW.owner AND NOT EXISTS(SELECT 1 FROM team_members WHERE owner=NEW.owner AND user_id=NEW.actor_id AND active=1 AND role IN ('admin','trainer','examiner','assessor'))))
 THEN RAISE(ABORT,'launch-self-review-or-role-conflict') END;
 SELECT CASE WHEN COALESCE(NEW.previous_event_id,'')<>COALESCE((SELECT id FROM learning_review_events WHERE owner=NEW.owner AND submission_id=NEW.submission_id ORDER BY created_at DESC,id DESC LIMIT 1),'')
 THEN RAISE(ABORT,'launch-review-stale-parent') END;
END;
