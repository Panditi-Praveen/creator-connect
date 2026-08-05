/**
 * Inbound request payloads for the Project Service.
 *
 * <p>Annotated with Jakarta Bean Validation constraints; failures surface as
 * {@code 400 BAD_REQUEST} via the global exception handler. The owning
 * {@code userId} is never part of a request DTO — it always comes from the JWT.
 */
package com.creatorconnect.project.dto.request;
