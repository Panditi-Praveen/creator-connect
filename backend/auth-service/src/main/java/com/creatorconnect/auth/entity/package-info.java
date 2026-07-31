/**
 * JPA entities mapped to the MySQL schema.
 *
 * <p>Contains the {@code User} aggregate — the core identity record for all
 * platform users. Entities use UUID primary keys, Lombok, and JPA auditing
 * ({@code @CreatedDate} / {@code @LastModifiedDate}).
 */
package com.creatorconnect.auth.entity;
