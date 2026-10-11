-- A transactional assertion: successful checks leave no rows behind.
CREATE TABLE staff_scope_assertions (allowed INTEGER NOT NULL CONSTRAINT staff_scope_current CHECK(allowed=1));
CREATE TRIGGER staff_scope_assertion_cleanup AFTER INSERT ON staff_scope_assertions
BEGIN DELETE FROM staff_scope_assertions WHERE rowid=NEW.rowid; END;
