-- Preserve existing read/dismiss state only when its old event ID identifies one tenant.
-- Ambiguous legacy IDs remain unread rather than silently applying another institution's state.
WITH objects(owner,item_id) AS (
 SELECT owner,'assignment:'||id FROM learning_course_assignments
 UNION ALL SELECT owner,'cancelled:'||id FROM learning_course_assignments
 UNION ALL SELECT owner,'submission:'||id FROM learning_course_submissions
 UNION ALL SELECT owner,'review:'||id FROM learning_review_events
 UNION ALL SELECT owner,'observation:'||id FROM learning_course_observations
), unique_objects AS (
 SELECT MIN(owner) AS owner,item_id FROM objects GROUP BY item_id HAVING COUNT(DISTINCT owner)=1
)
INSERT INTO learning_inbox_state(account_id,item_id,read_at,dismissed_at,updated_at)
SELECT s.account_id,o.owner||':'||s.item_id,s.read_at,s.dismissed_at,s.updated_at
FROM learning_inbox_state s JOIN unique_objects o ON o.item_id=s.item_id
WHERE 1 ON CONFLICT(account_id,item_id) DO NOTHING;
DELETE FROM learning_inbox_state WHERE item_id LIKE 'assignment:%' OR item_id LIKE 'cancelled:%' OR item_id LIKE 'submission:%' OR item_id LIKE 'review:%' OR item_id LIKE 'observation:%';
