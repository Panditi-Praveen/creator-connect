package com.creatorconnect.project.repository;

import com.creatorconnect.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link Project} entity.
 *
 * <p>Provides CRUD access plus the two derived queries required by the project
 * use cases:
 * <ul>
 *   <li>{@link #findAllByOrderByCreatedAtDesc()} — the browse feed
 *       ({@code GET /projects}), newest first.</li>
 *   <li>{@link #findByUserIdOrderByCreatedAtDesc(UUID)} — the caller's own
 *       projects ({@code GET /projects/my}), newest first.</li>
 * </ul>
 */
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    /**
     * Returns every project, most recently created first.
     *
     * @return the full project list (newest first)
     */
    List<Project> findAllByOrderByCreatedAtDesc();

    /**
     * Returns all projects owned by the given user, most recently created
     * first.
     *
     * @param userId the owning user's id (matches the JWT {@code userId} claim)
     * @return the user's projects (newest first); empty when the user has none
     */
    List<Project> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
