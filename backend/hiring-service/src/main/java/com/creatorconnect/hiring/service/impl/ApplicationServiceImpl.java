package com.creatorconnect.hiring.service.impl;

import com.creatorconnect.hiring.dto.request.ApplicationRequest;
import com.creatorconnect.hiring.dto.request.UpdateApplicationStatusRequest;
import com.creatorconnect.hiring.dto.response.ApplicationResponse;
import com.creatorconnect.hiring.entity.Application;
import com.creatorconnect.hiring.entity.ApplicationStatus;
import com.creatorconnect.hiring.exception.ApplicationAccessDeniedException;
import com.creatorconnect.hiring.exception.ApplicationNotFoundException;
import com.creatorconnect.hiring.exception.ApplicationStatusConflictException;
import com.creatorconnect.hiring.exception.ApplicationValidationException;
import com.creatorconnect.hiring.exception.DuplicateApplicationException;
import com.creatorconnect.hiring.feign.ProjectClientService;
import com.creatorconnect.hiring.feign.ProjectResponse;
import com.creatorconnect.hiring.mapper.ApplicationMapper;
import com.creatorconnect.hiring.repository.ApplicationRepository;
import com.creatorconnect.hiring.service.ApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Concrete {@link ApplicationService} implementation.
 *
 * <p>Owns the application lifecycle with these rules:
 * <ol>
 *   <li><b>Apply</b> — only {@code FREELANCER}s may apply; the caller's
 *       {@code userId} (from the JWT) becomes the {@code freelancerId}; a
 *       second application for the same project is rejected
 *       ({@link DuplicateApplicationException}). The project must exist in
 *       the Project Service (verified via OpenFeign).</li>
 *   <li><b>View</b> — a freelancer sees only their own applications; a
 *       {@code CREATOR} sees a project's applications only if they own the
 *       project (owner data is fetched from the Project Service via
 *       OpenFeign).</li>
 *   <li><b>Decide</b> — only {@code CREATOR}s may update status, only on
 *       their own projects, and only {@code ACCEPTED} / {@code REJECTED} are
 *       valid decisions on a {@code PENDING} application.</li>
 *   <li><b>Withdraw</b> — only the application's own freelancer may withdraw
 *       it, and only while it is {@code PENDING}.</li>
 * </ol>
 *
 * <p>Dependencies are injected through the constructor only (no field
 * injection). Write operations run inside one {@code @Transactional} boundary
 * so a failure rolls back cleanly.
 */
@Service
public class ApplicationServiceImpl implements ApplicationService {

    private static final String ROLE_FREELANCER = "FREELANCER";
    private static final String ROLE_CREATOR = "CREATOR";

    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;
    private final ProjectClientService projectClientService;

    /**
     * Creates the service with its collaborators.
     *
     * @param applicationRepository the application data access layer
     * @param applicationMapper     the entity/DTO mapper
     * @param projectClientService  the Project Service client used to verify
     *                              project existence and ownership
     */
    public ApplicationServiceImpl(ApplicationRepository applicationRepository,
                                  ApplicationMapper applicationMapper,
                                  ProjectClientService projectClientService) {
        this.applicationRepository = applicationRepository;
        this.applicationMapper = applicationMapper;
        this.projectClientService = projectClientService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ApplicationResponse apply(UUID freelancerId, String role, ApplicationRequest request) {
        if (!ROLE_FREELANCER.equalsIgnoreCase(role)) {
            throw new ApplicationAccessDeniedException("Only freelancers can apply to projects");
        }
        if (applicationRepository.existsByProjectIdAndFreelancerId(request.getProjectId(), freelancerId)) {
            throw new DuplicateApplicationException("You have already applied to this project");
        }
        // Verify the project exists in the Project Service before accepting
        // the application (404 when it does not). Kept after the local
        // duplicate check so repeat applicants never pay the network call.
        projectClientService.getProject(request.getProjectId());
        Application application = applicationRepository.save(
                applicationMapper.toEntity(freelancerId, request));
        return applicationMapper.toResponse(application);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getMyApplications(UUID freelancerId, Pageable pageable) {
        return applicationRepository.findByFreelancerId(freelancerId, pageable)
                .map(applicationMapper::toResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getApplicationsForProject(UUID creatorId, String role, UUID projectId,
                                                              Pageable pageable) {
        if (!ROLE_CREATOR.equalsIgnoreCase(role)) {
            throw new ApplicationAccessDeniedException("Only creators can view applications for a project");
        }
        requireProjectOwner(creatorId, projectId);
        return applicationRepository.findByProjectId(projectId, pageable)
                .map(applicationMapper::toResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ApplicationResponse updateStatus(UUID creatorId, String role, UUID applicationId,
                                            UpdateApplicationStatusRequest request) {
        if (!ROLE_CREATOR.equalsIgnoreCase(role)) {
            throw new ApplicationAccessDeniedException("Only creators can update application status");
        }
        ApplicationStatus requested = request.getStatus();
        if (requested != ApplicationStatus.ACCEPTED && requested != ApplicationStatus.REJECTED) {
            throw new ApplicationValidationException("Status must be ACCEPTED or REJECTED");
        }
        Application application = findApplication(applicationId);
        requireProjectOwner(creatorId, application.getProjectId());
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new ApplicationStatusConflictException(
                    "Only pending applications can be decided on (current status: " + application.getStatus() + ")");
        }
        application.setStatus(requested);
        return applicationMapper.toResponse(applicationRepository.save(application));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void withdraw(UUID freelancerId, UUID applicationId) {
        Application application = findApplication(applicationId);
        if (!application.getFreelancerId().equals(freelancerId)) {
            throw new ApplicationAccessDeniedException("You can only withdraw your own applications");
        }
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new ApplicationStatusConflictException(
                    "Only pending applications can be withdrawn (current status: " + application.getStatus() + ")");
        }
        application.setStatus(ApplicationStatus.WITHDRAWN);
        applicationRepository.save(application);
    }

    /**
     * Loads an application by id or fails with {@code 404}.
     *
     * @param applicationId the application's id
     * @return the persisted application
     * @throws ApplicationNotFoundException when no application has the given id
     */
    private Application findApplication(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + applicationId));
    }

    /**
     * Verifies that the caller owns the project with the given id.
     *
     * <p>The project's owner is fetched from the Project Service via OpenFeign
     * and compared against the caller's {@code userId}. Failures surface as
     * {@code 404} (project missing) or {@code 403} (not the owner).
     *
     * @param userId    the caller's id (from the JWT)
     * @param projectId the project's id
     * @throws com.creatorconnect.hiring.exception.ProjectNotFoundException
     *         when the project does not exist in the Project Service
     * @throws ApplicationAccessDeniedException when the caller is not the
     *         project's owner
     */
    private void requireProjectOwner(UUID userId, UUID projectId) {
        ProjectResponse project = projectClientService.getProject(projectId);
        if (!project.getUserId().equals(userId)) {
            throw new ApplicationAccessDeniedException("You do not own this project");
        }
    }
}
