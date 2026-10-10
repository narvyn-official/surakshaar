-- Public sources are platform-managed. Reviews and jobs belong to one approved centre.
CREATE TABLE safety_sources (
 id TEXT PRIMARY KEY, title TEXT NOT NULL, publisher TEXT NOT NULL, url TEXT NOT NULL UNIQUE,
 topic TEXT NOT NULL CHECK(topic IN ('fire','first-aid')), jurisdiction TEXT NOT NULL,
 rationale TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'proposed' CHECK(status IN ('proposed','approved','disabled')),
 revision INTEGER NOT NULL DEFAULT 1, changed_at INTEGER NOT NULL, changed_by TEXT, decision_note TEXT NOT NULL DEFAULT '',
 last_checked_at INTEGER, last_success_at INTEGER, last_error TEXT, latest_hash TEXT
);
CREATE TABLE safety_versions (
 id TEXT PRIMARY KEY, source_id TEXT NOT NULL REFERENCES safety_sources(id), source_revision INTEGER NOT NULL,
 title TEXT NOT NULL, publisher TEXT NOT NULL, source_url TEXT NOT NULL, published_date TEXT,
 retrieved_at INTEGER NOT NULL, content_hash TEXT NOT NULL, content TEXT NOT NULL, excerpt TEXT NOT NULL,
 previous_excerpt TEXT, topic TEXT NOT NULL, jurisdiction TEXT NOT NULL,
 change_kind TEXT NOT NULL CHECK(change_kind IN ('baseline','changed')),
 origin TEXT NOT NULL CHECK(origin IN ('tinyfish-fetch','fixture'))
);
CREATE INDEX safety_versions_source ON safety_versions(source_id,source_revision);
CREATE INDEX safety_versions_recent ON safety_versions(retrieved_at DESC,id DESC);
CREATE TABLE safety_reviews (
 owner TEXT NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,
 version_id TEXT NOT NULL REFERENCES safety_versions(id),
 status TEXT NOT NULL CHECK(status IN ('new','needs-review','approved','rejected')),
 notes TEXT NOT NULL, revision INTEGER NOT NULL, reviewed_by TEXT NOT NULL, reviewed_at INTEGER NOT NULL,
 PRIMARY KEY(owner,version_id)
);
CREATE TABLE safety_jobs (
 id TEXT PRIMARY KEY, owner TEXT NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,
 requested_by TEXT NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,
 created_at INTEGER NOT NULL, finished_at INTEGER,
 status TEXT NOT NULL CHECK(status IN ('queued','running','completed','partial','failed','cancelled')),
 error TEXT
);
CREATE UNIQUE INDEX safety_one_active_job ON safety_jobs(owner) WHERE status IN ('queued','running');
CREATE TABLE safety_job_items (
 job_id TEXT NOT NULL REFERENCES safety_jobs(id) ON DELETE CASCADE,
 source_id TEXT NOT NULL REFERENCES safety_sources(id), source_revision INTEGER NOT NULL,
 status TEXT NOT NULL DEFAULT 'queued' CHECK(status IN ('queued','running','succeeded','unchanged','failed')),
 attempts INTEGER NOT NULL DEFAULT 0, error TEXT, next_attempt_at INTEGER NOT NULL DEFAULT 0,
 lease_token TEXT, lease_until INTEGER, PRIMARY KEY(job_id,source_id)
);
CREATE TABLE safety_worker_slots (slot INTEGER PRIMARY KEY, token TEXT, expires_at INTEGER NOT NULL DEFAULT 0, heartbeat_at INTEGER);
INSERT INTO safety_worker_slots(slot) VALUES(1),(2),(3);
CREATE TABLE safety_budget (day TEXT PRIMARY KEY, calls INTEGER NOT NULL, cost_micros INTEGER NOT NULL);
CREATE TABLE safety_schedules (
 owner TEXT PRIMARY KEY REFERENCES auth_user(id) ON DELETE CASCADE,
 interval_hours INTEGER NOT NULL DEFAULT 168 CHECK(interval_hours BETWEEN 24 AND 720),
 enabled INTEGER NOT NULL DEFAULT 0 CHECK(enabled IN (0,1)), next_run_at INTEGER,
 changed_by TEXT NOT NULL, changed_at INTEGER NOT NULL
);
CREATE TABLE safety_source_audit (
 id TEXT PRIMARY KEY, source_id TEXT NOT NULL REFERENCES safety_sources(id),
 actor TEXT NOT NULL, action TEXT NOT NULL, note TEXT NOT NULL, at INTEGER NOT NULL, revision INTEGER NOT NULL
);
CREATE TRIGGER safety_version_immutable BEFORE UPDATE ON safety_versions BEGIN SELECT RAISE(ABORT,'safety-version-immutable'); END;
CREATE TRIGGER safety_source_audit_immutable BEFORE UPDATE ON safety_source_audit BEGIN SELECT RAISE(ABORT,'safety-audit-immutable'); END;

INSERT INTO safety_sources(id,title,publisher,url,topic,jurisdiction,rationale,changed_at) VALUES
 ('ndma-preparedness','Disaster preparedness: dos and don’ts','National Disaster Management Authority (India)','https://sachet.ndma.gov.in/DosDont','fire','India','Official Indian preparedness reference; includes other hazards. Review the fire section and extraction before approval.',0),
 ('usfa-extinguishers','Choosing and using fire extinguishers','U.S. Fire Administration','https://www.usfa.fema.gov/prevention/home-fires/prepare-for-fire/fire-extinguishers/index.html','fire','United States','Extinguisher selection and safe use. US fire classes and emergency numbers require local interpretation.',0),
 ('usfa-smoke-alarms','Smoke alarms','U.S. Fire Administration','https://www.usfa.fema.gov/prevention/home-fires/prepare-for-fire/smoke-alarms/','fire','United States','Detection and maintenance reference for staff; local codes take precedence.',0),
 ('osha-extinguishers','Portable extinguishers: extinguisher basics','Occupational Safety and Health Administration','https://www.osha.gov/etools/evacuation-plans-procedures/emergency-standards/portable-extinguishers/about','fire','United States','Workplace extinguisher reference. US regulatory guidance is not Indian law.',0),
 ('redcross-first-aid','First aid steps','American Red Cross','https://www.redcross.org/take-a-class/first-aid/performing-first-aid/first-aid-steps','first-aid','United States','Public first-aid sequence for staff review; emergency numbers and course scope require local adaptation.',0),
 ('sja-cpr','How to do CPR','St John Ambulance','https://www.sja.org.uk/first-aid-advice/cpr/','first-aid','United Kingdom','CPR reference; not clinical sign-off or a substitute for practical training.',0),
 ('sja-recovery','Recovery position','St John Ambulance','https://www.sja.org.uk/first-aid-advice/recovery-position/','first-aid','United Kingdom','Unresponsive, normally breathing casualty reference; requires qualified local review.',0),
 ('aha-updates','Guideline highlights and focused updates','American Heart Association','https://cpr.heart.org/en/resuscitation-science/guidelines-updates-and-highlights','first-aid','United States / international science','Publication index for resuscitation and first-aid changes. Collect the index only; linked documents require separate source approval.',0);
