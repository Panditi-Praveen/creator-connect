/**
 * Inbound request payloads for the Hiring Service.
 *
 * <p>Contains the create payload for applying to a project
 * ({@code ApplicationRequest}) and the creator decision payload
 * ({@code UpdateApplicationStatusRequest}). Identity fields such as
 * {@code freelancerId} are deliberately absent — they are derived from the
 * authenticated JWT, never from the client.
 */
package com.creatorconnect.hiring.dto.request;
