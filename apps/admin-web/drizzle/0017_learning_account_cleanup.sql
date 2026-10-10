-- Account deletion must not be blocked by review children of a shared snapshot.
-- Institutional assignment definitions remain; learner evidence and observation
-- notes are removed from these new tables when that learner account is deleted.
CREATE TRIGGER launch_account_cleanup BEFORE DELETE ON auth_user
BEGIN
 DELETE FROM learning_review_events WHERE EXISTS(
  SELECT 1 FROM learning_course_submissions s
  WHERE s.owner=learning_review_events.owner AND s.id=learning_review_events.submission_id AND s.account_id=OLD.id
 );
 DELETE FROM learning_course_observations WHERE learner_account_id=OLD.id;
END;
