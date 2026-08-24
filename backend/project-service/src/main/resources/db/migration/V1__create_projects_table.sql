-- V1: Create projects table for project-service

CREATE TABLE IF NOT EXISTS projects (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    skills_required VARCHAR(10000),
    budget DECIMAL(12,2) NOT NULL,
    duration VARCHAR(50) NOT NULL,
    experience_level VARCHAR(50) NOT NULL,
    location VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    application_deadline DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_projects PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_projects_user_id ON projects(user_id);
