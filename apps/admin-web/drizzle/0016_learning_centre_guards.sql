-- Re-check educator authority at commit time, after any concurrent role suspension.
CREATE TRIGGER launch_assignment_access BEFORE INSERT ON learning_course_assignments
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM centre_approvals WHERE owner=NEW.owner AND status='approved')
 OR (NEW.created_by<>NEW.owner AND NOT EXISTS(SELECT 1 FROM team_members WHERE owner=NEW.owner AND user_id=NEW.created_by AND active=1 AND role IN ('admin','trainer')))
 THEN RAISE(ABORT,'launch-review-access-conflict') END;
END;
