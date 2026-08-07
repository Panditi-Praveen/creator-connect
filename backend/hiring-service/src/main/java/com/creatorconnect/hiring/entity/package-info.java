/**
 * JPA entities mapped to the MySQL schema.
 *
 * <p>Contains the {@code Application} aggregate — a freelancer's application
 * to a project — and the {@code ApplicationStatus} lifecycle enum. Entities
 * use UUID primary keys, Lombok, Bean Validation, and JPA auditing
 * ({@code @CreatedDate} / {@code @LastModifiedDate}).
 */
package com.creatorconnect.hiring.entity;
