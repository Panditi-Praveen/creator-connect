package com.creatorconnect.project.repository;

import com.creatorconnect.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link Project} entity.
 *
 * <p>Provides CRUD access plus the two listing queries required by the project
 * use cases:
 * <ul>
 *   <li>{@link #findAllByOrderByCreatedAtDesc()} — the browse feed
 *       ({@code GET /projects}), newest first.</li>
 *   <li>{@link #findByUserIdOrderByCreatedAtDesc(UUID)} — the caller's own
 *       projects ({@code GET /projects/my}), newest first.</li>
 * </ul>
 *
 * <p>Both listings sort by {@code createdAt DESC, id DESC}. The secondary
 * {@code id} sort key makes the order <em>deterministic</em>: two projects
 * created within the same clock tick (identical {@code createdAt}) would
 * otherwise come back in an arbitrary order — a real problem on Windows,
 * whose system clock can be coarser than the gap between two successive
 * inserts.
 */
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    /**
     * Returns every project, most recently created first.
     *
     * @return the full project list (newest first, deterministic)
     */
    @Query("select p from Project p order by p.createdAt desc, p.id desc")
    List<Project> findAllByOrderByCreatedAtDesc();

    /**
     * Returns all projects owned by the given user, most recently created
     * first.
     *
     * @param userId the owning user's id (matches the JWT {@code userId} claim)
     * @return the user's projects (newest first, deterministic); empty when
     *         the user has none
     */
    @Query("select p from Project p where p.userId = :userId order by p.createdAt desc, p.id desc")
    List<Project> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
}
