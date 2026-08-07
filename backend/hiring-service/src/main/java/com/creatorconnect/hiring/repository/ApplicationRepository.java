package com.creatorconnect.hiring.repository;

import com.creatorconnect.hiring.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link Application} entity.
 *
 * <p>Provides CRUD access plus the queries backing the two listing use cases
 * and the duplicate guard:
 * <ul>
 *   <li>{@link #findByFreelancerId} — the caller's own applications
 *       ({@code GET /applications/my}), newest first.</li>
 *   <li>{@link #findByProjectId} — a project's incoming applications
 *       ({@code GET /applications/project/{projectId}}), newest first.</li>
 *   <li>{@link #existsByProjectIdAndFreelancerId} — the duplicate-application
 *       guard, mirrored by the {@code unique} constraint on
 *       {@code (project_id, freelancer_id)} at the database level.</li>
 * </ul>
 *
 * <p>Both listings are exposed twice: a plain {@link List} variant and a
 * paginated {@link Page} variant backed by Spring Data's {@link Pageable}
 * support (the controllers use the paginated one). No sort is hard-coded in
 * the query — ordering comes entirely from the {@link Pageable} supplied by
 * the caller, which defaults to {@code createdAt DESC, id DESC} (the
 * deterministic tiebreak used across CreatorConnect).
 */
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    /**
     * Returns the applications submitted by the given freelancer, newest
     * first, in a pageable fashion.
     *
     * @param freelancerId the freelancer's id (matches the JWT {@code userId} claim)
     * @param pageable     the paging/sorting specification (default
     *                     {@code createdAt DESC, id DESC})
     * @return the requested page of the freelancer's applications
     */
    Page<Application> findByFreelancerId(UUID freelancerId, Pageable pageable);

    /**
     * Returns all of a freelancer's applications, newest first (unpaged
     * convenience variant).
     *
     * @param freelancerId the freelancer's id
     * @return the freelancer's applications, newest first
     */
    List<Application> findByFreelancerId(UUID freelancerId);

    /**
     * Returns the applications received by the given project, newest first,
     * in a pageable fashion.
     *
     * @param projectId the project's id
     * @param pageable  the paging/sorting specification (default
     *                  {@code createdAt DESC, id DESC})
     * @return the requested page of the project's applications
     */
    Page<Application> findByProjectId(UUID projectId, Pageable pageable);

    /**
     * Returns all applications for the given project, newest first (unpaged
     * convenience variant).
     *
     * @param projectId the project's id
     * @return the project's applications, newest first
     */
    List<Application> findByProjectId(UUID projectId);

    /**
     * Reports whether the freelancer already applied to the project.
     *
     * @param projectId    the project's id
     * @param freelancerId the freelancer's id
     * @return {@code true} when an application already exists for the pair
     */
    boolean existsByProjectIdAndFreelancerId(UUID projectId, UUID freelancerId);
}
