ALTER TABLE identity_sessions ADD COLUMN review_outcome TEXT;
ALTER TABLE identity_frames ADD COLUMN challenge_id TEXT;
ALTER TABLE identity_frames ADD COLUMN image_digest TEXT;
CREATE UNIQUE INDEX identity_used_challenge ON identity_frames(session_id,challenge_id) WHERE challenge_id IS NOT NULL;
CREATE TABLE institution_attempt_starts (
 owner TEXT NOT NULL,assignment_id TEXT NOT NULL,scenario_id TEXT NOT NULL,session_id TEXT PRIMARY KEY,started_at INTEGER NOT NULL,
 FOREIGN KEY(owner,assignment_id) REFERENCES learning_course_assignments(owner,id)
);
INSERT INTO institution_attempt_starts SELECT owner,assignment_id,scenario_id,id,started_at FROM identity_sessions;
CREATE INDEX institution_attempt_count ON institution_attempt_starts(owner,assignment_id,scenario_id);
CREATE TRIGGER identity_start_guard BEFORE INSERT ON identity_sessions
BEGIN
 SELECT CASE WHEN NEW.reference_id IS NULL OR NOT EXISTS(SELECT 1 FROM identity_references r JOIN learner_links l ON l.owner=r.owner AND l.worker_id=r.worker_id AND l.user_id=r.account_id WHERE r.owner=NEW.owner AND r.id=NEW.reference_id AND r.worker_id=NEW.worker_id AND r.account_id=NEW.account_id AND r.status='approved' AND r.expires_at>NEW.started_at)
 THEN RAISE(ABORT,'identity-reference-required') END;
 SELECT CASE WHEN (SELECT COUNT(*) FROM institution_attempt_starts WHERE owner=NEW.owner AND assignment_id=NEW.assignment_id AND scenario_id=NEW.scenario_id)>=COALESCE((SELECT json_extract(p.body,'$.maximumAttempts') FROM learning_course_assignments a JOIN institution_policies p ON p.owner=a.owner AND p.version=a.policy_version WHERE a.owner=NEW.owner AND a.id=NEW.assignment_id),3)
 THEN RAISE(ABORT,'identity-attempt-limit') END;
END;
CREATE TRIGGER identity_start_count AFTER INSERT ON identity_sessions
BEGIN INSERT INTO institution_attempt_starts VALUES(NEW.owner,NEW.assignment_id,NEW.scenario_id,NEW.id,NEW.started_at); END;
CREATE TRIGGER institution_restore_quota BEFORE UPDATE OF archived_at ON institution_people
WHEN OLD.archived_at IS NOT NULL AND NEW.archived_at IS NULL AND EXISTS(SELECT 1 FROM institution_plans p WHERE p.owner=NEW.owner AND p.learner_limit<=(SELECT COUNT(*) FROM workers w LEFT JOIN institution_people i ON i.owner=w.owner AND i.worker_id=w.id WHERE w.owner=NEW.owner AND i.archived_at IS NULL))
BEGIN SELECT RAISE(ABORT,'institution-quota-reached'); END;
