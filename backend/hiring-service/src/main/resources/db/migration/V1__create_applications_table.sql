-- V1: Create applications table for hiring-service

CREATE TABLE IF NOT EXISTS applications (
    id CHAR(36) NOT NULL,
    project_id CHAR(36) NOT NULL,
    freelancer_id CHAR(36) NOT NULL,
    proposal TEXT NOT NULL,
    expected_budget DECIMAL(12,2) NOT NULL,
    estimated_duration VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_applications PRIMARY KEY (id),
    CONSTRAINT uk_applications_project_freelancer UNIQUE (project_id, freelancer_id)
);

CREATE INDEX IF NOT EXISTS idx_applications_freelancer_id ON applications(freelancer_id);
CREATE INDEX IF NOT EXISTS idx_applications_project_id ON applications(project_id);
