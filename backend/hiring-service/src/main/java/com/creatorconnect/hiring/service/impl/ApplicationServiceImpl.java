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
 *       ({@link DuplicateApplicationException}).</li>
 *   <li><b>View</b> — a freelancer sees only their own applications; a
 *       {@code CREATOR} sees a project's applications (verifying the creator
 *       actually owns the project is deferred to Day 6 — that data lives in
 *       the Project Service).</li>
 *   <li><b>Decide</b> — only {@code CREATOR}s may update status, and only
 *       {@code ACCEPTED} / {@code REJECTED} are valid decisions on a
 *       {@code PENDING} application.</li>
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

    /**
     * Creates the service with its collaborators.
     *
     * @param applicationRepository the application data access layer
     * @param applicationMapper     the entity/DTO mapper
     */
    public ApplicationServiceImpl(ApplicationRepository applicationRepository,
                                  ApplicationMapper applicationMapper) {
        this.applicationRepository = applicationRepository;
        this.applicationMapper = applicationMapper;
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
        // TODO(Day 6): verify the project exists via the Project Service
        // (GET /projects/{id}) before accepting an application.
        if (applicationRepository.existsByProjectIdAndFreelancerId(request.getProjectId(), freelancerId)) {
            throw new DuplicateApplicationException("You have already applied to this project");
        }
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
    public Page<ApplicationResponse> getApplicationsForProject(String role, UUID projectId, Pageable pageable) {
        if (!ROLE_CREATOR.equalsIgnoreCase(role)) {
            throw new ApplicationAccessDeniedException("Only creators can view applications for a project");
        }
        // TODO(Day 6): verify the caller owns the project via the Project
        // Service before exposing its applications.
        return applicationRepository.findByProjectId(projectId, pageable)
                .map(applicationMapper::toResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ApplicationResponse updateStatus(String role, UUID applicationId,
                                            UpdateApplicationStatusRequest request) {
        if (!ROLE_CREATOR.equalsIgnoreCase(role)) {
            throw new ApplicationAccessDeniedException("Only creators can update application status");
        }
        // TODO(Day 6): verify the caller owns the application's project via
        // the Project Service before allowing the decision.
        ApplicationStatus requested = request.getStatus();
        if (requested != ApplicationStatus.ACCEPTED && requested != ApplicationStatus.REJECTED) {
            throw new ApplicationValidationException("Status must be ACCEPTED or REJECTED");
        }
        Application application = findApplication(applicationId);
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
}
