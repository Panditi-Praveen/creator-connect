-- V1: Create profiles table for profile-service

CREATE TABLE IF NOT EXISTS profiles (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    headline VARCHAR(150),
    bio TEXT,
    profile_image_url VARCHAR(500),
    profile_image_path VARCHAR(500),
    location VARCHAR(100),
    latitude DOUBLE,
    longitude DOUBLE,
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    formatted_address VARCHAR(300),
    website VARCHAR(300),
    linkedin VARCHAR(300),
    github VARCHAR(300),
    skills VARCHAR(1000),
    experience INT,
    available_for_hire BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_profiles PRIMARY KEY (id),
    CONSTRAINT uk_profiles_user_id UNIQUE (user_id)
);
