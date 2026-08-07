/**
 * REST controllers for the Hiring Service API.
 *
 * <p>Controllers are thin by design: they validate inbound payloads, derive
 * the caller's identity from the authenticated {@code HiringPrincipal}, and
 * delegate all business logic to the {@code ApplicationService} contract.
 */
package com.creatorconnect.hiring.controller;
