/**
 * REST controllers for the Project Service API.
 *
 * <p>Controllers are thin by design: they validate inbound payloads, derive
 * the caller's identity from the authenticated {@code ProjectPrincipal}, and
 * delegate all business logic to the {@code ProjectService} contract.
 */
package com.creatorconnect.project.controller;
