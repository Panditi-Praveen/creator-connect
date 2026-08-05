/**
 * JPA entities mapped to the MySQL schema.
 *
 * <p>Contains the {@code Project} aggregate — a creative project posted by a
 * user — and the {@code ProjectStatus} lifecycle enum. Entities use UUID
 * primary keys, Lombok, Bean Validation, and JPA auditing
 * ({@code @CreatedDate} / {@code @LastModifiedDate}).
 */
package com.creatorconnect.project.entity;
