package com.creatorconnect.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing so that {@code @CreatedDate} and
 * {@code @LastModifiedDate} fields on entities (e.g. {@code Project}) are
 * populated automatically on persist/update.
 *
 * <p>Without this configuration, auditing annotations are silently ignored and
 * the timestamp columns remain {@code NULL}.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
