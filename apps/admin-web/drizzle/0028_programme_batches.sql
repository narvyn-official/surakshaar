CREATE TABLE institution_programme_batches (
 owner TEXT NOT NULL,id TEXT NOT NULL,digest TEXT NOT NULL,worker_ids TEXT NOT NULL CHECK(json_valid(worker_ids)),created_at INTEGER NOT NULL,
 PRIMARY KEY(owner,id),FOREIGN KEY(owner) REFERENCES training_centres(owner)
);
CREATE TRIGGER institution_programme_immutable BEFORE UPDATE ON institution_programme_batches BEGIN SELECT RAISE(ABORT,'institution-programme-immutable'); END;
CREATE TRIGGER institution_assignment_entitlement BEFORE INSERT ON learning_course_assignments
BEGIN
 SELECT CASE WHEN EXISTS(SELECT 1 FROM institution_plans WHERE owner=NEW.owner AND (restricted=1 OR (valid_until IS NOT NULL AND valid_until<=NEW.created_at))) OR EXISTS(SELECT 1 FROM institution_people WHERE owner=NEW.owner AND worker_id=NEW.worker_id AND archived_at IS NOT NULL)
 THEN RAISE(ABORT,'institution-assignment-restricted') END;
END;

CREATE TRIGGER institution_award_membership BEFORE INSERT ON institution_awards
WHEN EXISTS(SELECT 1 FROM institution_people WHERE owner=NEW.owner AND worker_id=NEW.worker_id AND archived_at IS NOT NULL)
BEGIN SELECT RAISE(ABORT,'institution-award-membership'); END;
