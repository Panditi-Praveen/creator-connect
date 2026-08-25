-- V3: Add remaining missing profile columns not present in the original V1
-- table creation. latitude and longitude are DOUBLE (mapped as
-- java.lang.Double by Hibernate), profile_image_path is VARCHAR(500).

ALTER TABLE profiles
    ADD COLUMN latitude DOUBLE,
    ADD COLUMN longitude DOUBLE,
    ADD COLUMN profile_image_path VARCHAR(500);
