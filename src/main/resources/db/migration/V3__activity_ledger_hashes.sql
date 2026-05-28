ALTER TABLE admin_activity_logs ADD COLUMN current_hash VARCHAR(64) DEFAULT '';
ALTER TABLE admin_activity_logs ADD COLUMN previous_hash VARCHAR(64) DEFAULT '';
