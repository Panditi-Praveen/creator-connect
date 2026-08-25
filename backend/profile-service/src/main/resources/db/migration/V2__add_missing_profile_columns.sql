-- V2: Add missing profile columns not present in the original V1 table creation.
-- These columns were added to V1__create_profiles_table.sql after the migration
-- was first applied to the database, so a separate migration is required to
-- bring the live schema in sync with the current entity.

ALTER TABLE profiles
    ADD COLUMN city VARCHAR(100),
    ADD COLUMN state VARCHAR(100),
    ADD COLUMN country VARCHAR(100),
    ADD COLUMN formatted_address VARCHAR(300);
