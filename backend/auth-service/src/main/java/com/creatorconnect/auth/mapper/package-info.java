/**
 * Object mappers converting between entities and DTOs.
 *
 * <p>Keeps entity-to-DTO conversion in one place (e.g.
 * {@code UserMapper#toResponse(User)}), preventing entities from leaking into
 * API responses. Implemented manually for explicitness — no reflection-based
 * mapping library.
 */
package com.creatorconnect.auth.mapper;
