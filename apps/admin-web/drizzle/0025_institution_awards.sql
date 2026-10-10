CREATE TABLE institution_signing_keys (
 owner TEXT NOT NULL,kid TEXT NOT NULL,public_jwk TEXT NOT NULL,private_ciphertext TEXT NOT NULL,created_at INTEGER NOT NULL,retired_at INTEGER,
 PRIMARY KEY(owner,kid),UNIQUE(kid),FOREIGN KEY(owner) REFERENCES training_centres(owner)
);
CREATE UNIQUE INDEX institution_active_key ON institution_signing_keys(owner) WHERE retired_at IS NULL;
CREATE TABLE institution_awards (
 owner TEXT NOT NULL,id TEXT NOT NULL,assignment_id TEXT NOT NULL,worker_id TEXT NOT NULL,account_id TEXT NOT NULL,
 kid TEXT NOT NULL,payload TEXT NOT NULL,signature TEXT NOT NULL,issued_by TEXT NOT NULL,issued_at INTEGER NOT NULL,expires_at INTEGER NOT NULL,
 revoked_at INTEGER,revocation_reason TEXT,replaced_by TEXT,
 PRIMARY KEY(owner,id),UNIQUE(id),FOREIGN KEY(owner,assignment_id) REFERENCES learning_course_assignments(owner,id),FOREIGN KEY(owner,kid) REFERENCES institution_signing_keys(owner,kid)
);
CREATE UNIQUE INDEX institution_award_active ON institution_awards(owner,assignment_id) WHERE revoked_at IS NULL AND replaced_by IS NULL;
CREATE TRIGGER institution_award_immutable BEFORE UPDATE OF owner,id,assignment_id,worker_id,account_id,kid,payload,signature,issued_by,issued_at,expires_at ON institution_awards
BEGIN SELECT RAISE(ABORT,'institution-award-immutable'); END;
CREATE TRIGGER institution_award_authority BEFORE INSERT ON institution_awards
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM centre_approvals WHERE owner=NEW.owner AND status='approved') OR NOT EXISTS(SELECT 1 FROM team_members WHERE owner=NEW.owner AND user_id=NEW.issued_by AND active=1 AND role IN('certifier','assessor'))
 THEN RAISE(ABORT,'institution-award-authority') END;
 SELECT CASE WHEN NEW.issued_by=NEW.account_id OR NOT EXISTS(SELECT 1 FROM learner_links l JOIN learning_course_assignments a ON a.owner=l.owner AND a.worker_id=l.worker_id WHERE a.owner=NEW.owner AND a.id=NEW.assignment_id AND a.cancelled_at IS NULL AND l.user_id=NEW.account_id AND l.worker_id=NEW.worker_id)
 THEN RAISE(ABORT,'institution-award-authority') END;
 SELECT CASE WHEN EXISTS(SELECT 1 FROM institution_plans WHERE owner=NEW.owner AND (restricted=1 OR (valid_until IS NOT NULL AND valid_until<=NEW.issued_at)))
 THEN RAISE(ABORT,'institution-quota-reached') END;
END;
