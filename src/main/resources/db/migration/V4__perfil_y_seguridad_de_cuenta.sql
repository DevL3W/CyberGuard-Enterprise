ALTER TABLE app_users ADD COLUMN email VARCHAR(150);
ALTER TABLE app_users ADD COLUMN job_title VARCHAR(100);
ALTER TABLE app_users ADD COLUMN password_changed_at TIMESTAMPTZ;
