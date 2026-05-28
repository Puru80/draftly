-- User Profile & Secure Credential Store
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       encrypted_refresh_token TEXT NOT NULL,
                       custom_signature TEXT,
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Active Conversational Threads Caching
CREATE TABLE email_threads (
                               id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
                               gmail_thread_id VARCHAR(255) UNIQUE NOT NULL,
                               subject VARCHAR(500),
                               last_synchronized_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Draft Repository Tracking States
CREATE TABLE drafts (
                        id BIGSERIAL PRIMARY KEY,
                        thread_id BIGINT REFERENCES email_threads(id) ON DELETE CASCADE,
                        suggested_body TEXT NOT NULL,
                        current_tone VARCHAR(50) NOT NULL DEFAULT 'CONCISE',
                        status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, EDITED, REJECTED
                        idempotency_key VARCHAR(255) UNIQUE,
                        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Telemetry & System Auditing
CREATE TABLE audit_logs (
                            id BIGSERIAL PRIMARY KEY,
                            user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
                            action_type VARCHAR(100) NOT NULL,
                            execution_status VARCHAR(50) NOT NULL,
                            log_payload TEXT,
                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);