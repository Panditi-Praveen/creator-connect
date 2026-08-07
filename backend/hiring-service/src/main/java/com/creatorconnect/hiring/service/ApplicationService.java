package com.creatorconnect.hiring.service;

import com.creatorconnect.hiring.dto.request.ApplicationRequest;
import com.creatorconnect.hiring.dto.request.UpdateApplicationStatusRequest;
import com.creatorconnect.hiring.dto.response.ApplicationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Hiring Service use cases — the business logic contract layer.
 *
 * <p>Exposes the operations the Hiring Service API supports. Implementations
 * live in {@code service.impl}; the interface decouples the controller from
 * concrete logic (SOLID — dependency inversion).
 *
 * <p>Ownership discipline: the {@code freelancerId} / {@code role} parameters
 * are always derived from the JWT, never from the request body, so a caller
 * can only apply as themselves, withdraw their own applications, and act
 * within their own role.
 */
public interface ApplicationService {

    /**
     * Applies the given freelancer to the project in the request.
     *
     * @param freelancerId the applying freelancer's id (from the JWT)
     * @param role         the caller's role from the JWT
     * @param request      the validated create payload
     * @return the persisted application projection
     * @throws com.creatorconnect.hiring.exception.ApplicationAccessDeniedException
     *         when the caller is not a FREELANCER
     * @throws com.creatorconnect.hiring.exception.DuplicateApplicationException
     *         when the freelancer already applied to the project
     */
    ApplicationResponse apply(UUID freelancerId, String role, ApplicationRequest request);

    /**
     * Returns the caller's own applications (newest first, paginated).
     *
     * @param freelancerId the caller's id (from the JWT)
     * @param pageable     the paging/sorting specification
     * @return the requested page of the caller's applications
     */
    Page<ApplicationResponse> getMyApplications(UUID freelancerId, Pageable pageable);

    /**
     * Returns the applications received by a project (newest first,
     * paginated).
     *
     * <p>Requires the {@code CREATOR} role. Verifying that the caller owns the
     * project is deferred to Day 6 (Hiring Service &harr; Project Service
     * integration) — the project's owner data lives in the Project Service.
     *
     * @param role      the caller's role from the JWT
     * @param projectId the project's id
     * @param pageable  the paging/sorting specification
     * @return the requested page of the project's applications
     * @throws com.creatorconnect.hiring.exception.ApplicationAccessDeniedException
     *         when the caller is not a CREATOR
     */
    Page<ApplicationResponse> getApplicationsForProject(String role, UUID projectId, Pageable pageable);

    /**
     * Assigns a new status to an application (creator decision).
     *
     * <p>Only {@code ACCEPTED} and {@code REJECTED} are valid decisions, and
     * only on applications that are still {@code PENDING}.
     *
     * @param role          the caller's role from the JWT
     * @param applicationId the application to decide on
     * @param request       the validated decision payload
     * @return the updated application projection
     * @throws com.creatorconnect.hiring.exception.ApplicationAccessDeniedException
     *         when the caller is not a CREATOR
     * @throws com.creatorconnect.hiring.exception.ApplicationNotFoundException
     *         when no application has the given id
     * @throws com.creatorconnect.hiring.exception.ApplicationValidationException
     *         when the requested status is not ACCEPTED or REJECTED
     * @throws com.creatorconnect.hiring.exception.ApplicationStatusConflictException
     *         when the application is no longer pending
     */
    ApplicationResponse updateStatus(String role, UUID applicationId, UpdateApplicationStatusRequest request);

    /**
     * Withdraws the application with the given id (soft-delete: the status
     * becomes {@code WITHDRAWN} and the record stays visible to the project's
     * creator).
     *
     * @param freelancerId  the caller's id (from the JWT)
     * @param applicationId the application to withdraw
     * @throws com.creatorconnect.hiring.exception.ApplicationNotFoundException
     *         when no application has the given id
     * @throws com.creatorconnect.hiring.exception.ApplicationAccessDeniedException
     *         when the caller is not the application's freelancer
     * @throws com.creatorconnect.hiring.exception.ApplicationStatusConflictException
     *         when the application is no longer pending
     */
    void withdraw(UUID freelancerId, UUID applicationId);
}
