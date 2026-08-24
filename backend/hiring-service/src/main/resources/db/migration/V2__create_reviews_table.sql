-- V2: Create reviews table for hiring-service

CREATE TABLE IF NOT EXISTS reviews (
    id CHAR(36) NOT NULL,
    project_id CHAR(36) NOT NULL,
    creator_id CHAR(36) NOT NULL,
    freelancer_id CHAR(36) NOT NULL,
    rating INT NOT NULL,
    review_text TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_reviews PRIMARY KEY (id),
    CONSTRAINT uk_reviews_project_freelancer UNIQUE (project_id, freelancer_id)
);

CREATE INDEX IF NOT EXISTS idx_reviews_freelancer_id ON reviews(freelancer_id);
