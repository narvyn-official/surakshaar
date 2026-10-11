ALTER TABLE team_members ADD COLUMN scope_mode TEXT NOT NULL DEFAULT 'all' CHECK(scope_mode IN ('all','units'));
CREATE TABLE team_member_units (
 owner TEXT NOT NULL,
 email TEXT NOT NULL,
 unit_id TEXT NOT NULL,
 PRIMARY KEY(owner,email,unit_id),
 FOREIGN KEY(owner,email) REFERENCES team_members(owner,email) ON DELETE CASCADE,
 FOREIGN KEY(owner,unit_id) REFERENCES institution_units(owner,id)
);
CREATE INDEX institution_people_unit_scope ON institution_people(owner,unit_id,worker_id);
