-- V3: Create notifications table for hiring-service

CREATE TABLE IF NOT EXISTS notifications (
    id CHAR(36) NOT NULL,
    recipient_id CHAR(36) NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(500),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    related_resource_id CHAR(36),
    related_resource_type VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_id ON notifications(recipient_id);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_read ON notifications(recipient_id, is_read);
