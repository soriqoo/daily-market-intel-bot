CREATE TABLE IF NOT EXISTS job_run (
   run_date TEXT PRIMARY KEY,
   status TEXT NOT NULL,
   payload_hash TEXT NOT NULL,
   sent_at TEXT,
   error TEXT
);
