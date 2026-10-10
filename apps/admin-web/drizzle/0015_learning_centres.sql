-- Launch-course formative work is isolated from industrial credentials.
CREATE TABLE learning_course_assignments (
 owner TEXT NOT NULL, id TEXT NOT NULL, worker_id TEXT NOT NULL,
 course_id TEXT NOT NULL CHECK(course_id IN ('fire','first-aid')),
 content_version TEXT NOT NULL, audience TEXT NOT NULL CHECK(audience IN ('school-awareness','adult-learning')),
 scenario_ids TEXT NOT NULL CHECK(json_valid(scenario_ids)),
 catalogue_snapshot TEXT NOT NULL CHECK(json_valid(catalogue_snapshot)),
 mode TEXT NOT NULL CHECK(mode IN ('guided','independent')),
 due_at INTEGER NOT NULL, created_at INTEGER NOT NULL, created_by TEXT NOT NULL,
 note TEXT NOT NULL, cancelled_at INTEGER, cancel_reason TEXT,
 PRIMARY KEY(owner,id), FOREIGN KEY(owner,worker_id) REFERENCES workers(owner,id)
);
CREATE TRIGGER launch_assignment_identity BEFORE INSERT ON learning_course_assignments
WHEN EXISTS(SELECT 1 FROM learning_course_assignments a WHERE a.owner=NEW.owner AND a.id=NEW.id AND (a.worker_id<>NEW.worker_id OR a.course_id<>NEW.course_id OR a.content_version<>NEW.content_version OR a.audience<>NEW.audience OR a.scenario_ids<>NEW.scenario_ids OR a.mode<>NEW.mode OR a.due_at<>NEW.due_at OR a.note<>NEW.note))
BEGIN SELECT RAISE(ABORT,'launch-assignment-identity-conflict'); END;
CREATE TRIGGER launch_assignment_immutable BEFORE UPDATE OF owner,id,worker_id,course_id,content_version,audience,scenario_ids,catalogue_snapshot,mode,due_at,created_at,created_by,note ON learning_course_assignments
BEGIN SELECT RAISE(ABORT,'launch-assignment-immutable'); END;
CREATE INDEX launch_assignment_learner ON learning_course_assignments(owner,worker_id,created_at DESC,id DESC);
CREATE TABLE learning_course_submissions (
 owner TEXT NOT NULL,id TEXT NOT NULL,assignment_id TEXT NOT NULL,
 worker_id TEXT NOT NULL,account_id TEXT NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,
 attempt_id TEXT NOT NULL,source_digest TEXT NOT NULL,scenario_id TEXT NOT NULL,
 evidence TEXT NOT NULL CHECK(json_valid(evidence)),created_at INTEGER NOT NULL,withdrawn_at INTEGER,
 PRIMARY KEY(owner,id), UNIQUE(owner,assignment_id,attempt_id,source_digest),
 FOREIGN KEY(owner,assignment_id) REFERENCES learning_course_assignments(owner,id)
);
CREATE INDEX launch_submission_assignment ON learning_course_submissions(owner,assignment_id,created_at DESC,id DESC);
CREATE TRIGGER launch_submission_guard BEFORE INSERT ON learning_course_submissions
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM learning_course_assignments a JOIN learner_links l ON l.owner=a.owner AND l.worker_id=a.worker_id JOIN centre_approvals c ON c.owner=a.owner AND c.status='approved' WHERE a.owner=NEW.owner AND a.id=NEW.assignment_id AND a.worker_id=NEW.worker_id AND a.cancelled_at IS NULL AND l.user_id=NEW.account_id)
 THEN RAISE(ABORT,'launch-sharing-access-conflict') END;
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM learning_practice_snapshots s WHERE s.account_id=NEW.account_id AND s.id=NEW.attempt_id AND s.worker_id=NEW.worker_id AND s.digest=NEW.source_digest)
 THEN RAISE(ABORT,'launch-sharing-source-conflict') END;
END;
CREATE TRIGGER launch_submission_immutable BEFORE UPDATE OF owner,id,assignment_id,worker_id,account_id,attempt_id,source_digest,scenario_id,evidence,created_at ON learning_course_submissions
BEGIN SELECT RAISE(ABORT,'launch-shared-evidence-immutable'); END;
CREATE TRIGGER launch_submission_withdrawal BEFORE UPDATE OF withdrawn_at ON learning_course_submissions
WHEN OLD.withdrawn_at IS NOT NULL OR NEW.withdrawn_at IS NULL
BEGIN SELECT RAISE(ABORT,'launch-withdrawal-immutable'); END;
CREATE TABLE learning_review_events (
 owner TEXT NOT NULL,id TEXT NOT NULL,submission_id TEXT NOT NULL,actor_id TEXT NOT NULL,
 previous_event_id TEXT,outcome TEXT NOT NULL CHECK(outcome IN ('reviewed','repeat-practice','needs-information','correction-request')),
 note TEXT NOT NULL,created_at INTEGER NOT NULL,
 PRIMARY KEY(owner,id), FOREIGN KEY(owner,submission_id) REFERENCES learning_course_submissions(owner,id)
);
CREATE INDEX launch_review_time ON learning_review_events(owner,submission_id,created_at DESC,id DESC);
CREATE TRIGGER launch_review_guard BEFORE INSERT ON learning_review_events
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM learning_course_submissions s JOIN learner_links l ON l.owner=s.owner AND l.worker_id=s.worker_id AND l.user_id=s.account_id JOIN centre_approvals c ON c.owner=s.owner AND c.status='approved' WHERE s.owner=NEW.owner AND s.id=NEW.submission_id AND s.withdrawn_at IS NULL)
 THEN RAISE(ABORT,'launch-review-access-conflict') END;
 SELECT CASE WHEN NEW.outcome='correction-request' AND NOT EXISTS(SELECT 1 FROM learning_course_submissions WHERE owner=NEW.owner AND id=NEW.submission_id AND account_id=NEW.actor_id)
 THEN RAISE(ABORT,'launch-review-access-conflict') END;
 SELECT CASE WHEN NEW.outcome<>'correction-request' AND (EXISTS(SELECT 1 FROM learning_course_submissions WHERE owner=NEW.owner AND id=NEW.submission_id AND account_id=NEW.actor_id) OR (NEW.actor_id<>NEW.owner AND NOT EXISTS(SELECT 1 FROM team_members WHERE owner=NEW.owner AND user_id=NEW.actor_id AND active=1 AND role IN ('admin','trainer'))))
 THEN RAISE(ABORT,'launch-self-review-or-role-conflict') END;
 SELECT CASE WHEN COALESCE(NEW.previous_event_id,'')<>COALESCE((SELECT id FROM learning_review_events WHERE owner=NEW.owner AND submission_id=NEW.submission_id ORDER BY created_at DESC,id DESC LIMIT 1),'')
 THEN RAISE(ABORT,'launch-review-stale-parent') END;
END;
CREATE TRIGGER launch_review_immutable BEFORE UPDATE ON learning_review_events
BEGIN SELECT RAISE(ABORT,'launch-review-events-immutable'); END;
CREATE TABLE learning_course_observations (
 owner TEXT NOT NULL,id TEXT NOT NULL,assignment_id TEXT NOT NULL,instructor_id TEXT NOT NULL,learner_account_id TEXT NOT NULL,
 observed_at INTEGER NOT NULL,setting TEXT NOT NULL CHECK(setting IN ('in-person-training','remote-simulation')),
 rubric TEXT NOT NULL CHECK(json_valid(rubric)),outcome TEXT NOT NULL CHECK(outcome IN ('observed','further-practice')),
 note TEXT NOT NULL,created_at INTEGER NOT NULL,
 PRIMARY KEY(owner,id),FOREIGN KEY(owner,assignment_id) REFERENCES learning_course_assignments(owner,id)
);
CREATE TRIGGER launch_observation_guard BEFORE INSERT ON learning_course_observations
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM learning_course_assignments a JOIN learner_links l ON l.owner=a.owner AND l.worker_id=a.worker_id JOIN centre_approvals c ON c.owner=a.owner AND c.status='approved' WHERE a.owner=NEW.owner AND a.id=NEW.assignment_id AND a.cancelled_at IS NULL AND l.user_id=NEW.learner_account_id AND l.user_id<>NEW.instructor_id AND NEW.observed_at>=a.created_at)
 THEN RAISE(ABORT,'launch-review-access-conflict') END;
 SELECT CASE WHEN NEW.instructor_id<>NEW.owner AND NOT EXISTS(SELECT 1 FROM team_members WHERE owner=NEW.owner AND user_id=NEW.instructor_id AND active=1 AND role IN ('admin','trainer'))
 THEN RAISE(ABORT,'launch-review-access-conflict') END;
END;
CREATE TRIGGER launch_observation_immutable BEFORE UPDATE ON learning_course_observations
BEGIN SELECT RAISE(ABORT,'launch-observations-immutable'); END;
CREATE TABLE learning_inbox_state (
 account_id TEXT NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,item_id TEXT NOT NULL,
 read_at INTEGER,dismissed_at INTEGER,updated_at INTEGER NOT NULL,
 PRIMARY KEY(account_id,item_id)
);
