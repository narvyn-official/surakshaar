-- Public equipment discovery is private to the requesting workspace. It grants no source approval.
CREATE TABLE safety_searches (
 id TEXT PRIMARY KEY, owner TEXT NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,
 requested_by TEXT NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,
 cache_key TEXT NOT NULL, model TEXT NOT NULL, host TEXT NOT NULL, created_at INTEGER NOT NULL,
 status TEXT NOT NULL CHECK(status IN ('pending','completed','failed')),
 results TEXT NOT NULL DEFAULT '[]', error TEXT
);
CREATE INDEX safety_searches_owner ON safety_searches(owner,created_at DESC);
CREATE INDEX safety_searches_cache ON safety_searches(owner,cache_key,created_at DESC);

-- A link records a review dependency, never a change to the curriculum.
CREATE TABLE safety_lesson_links (
 id TEXT PRIMARY KEY, owner TEXT NOT NULL REFERENCES auth_user(id) ON DELETE CASCADE,
 source_id TEXT NOT NULL REFERENCES safety_sources(id), content_version TEXT NOT NULL,
 scenario_id TEXT NOT NULL, step_id TEXT NOT NULL,
 aspect TEXT NOT NULL CHECK(aspect IN ('instruction','model-detail','assessment')),
 rationale TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 1,
 created_by TEXT NOT NULL, created_at INTEGER NOT NULL,
 reviewed_version_id TEXT REFERENCES safety_versions(id),
 decision TEXT CHECK(decision IN ('no-change','revision-needed')),
 review_notes TEXT NOT NULL DEFAULT '', reviewed_by TEXT, reviewed_at INTEGER,
 UNIQUE(owner,source_id,content_version,scenario_id,step_id,aspect)
);
CREATE INDEX safety_lesson_links_owner ON safety_lesson_links(owner,created_at DESC);
