package com.creatorconnect.project.repository;

import com.creatorconnect.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link Project} entity.
 *
 * <p>Provides CRUD access plus the two listing queries backing the project
 * feed use cases:
 * <ul>
 *   <li>{@link #findAllByFilters} — the browse feed
 *       ({@code GET /projects}), newest first.</li>
 *   <li>{@link #findByUserIdAndFilters} — the caller's own projects
 *       ({@code GET /projects/my}), newest first.</li>
 * </ul>
 *
 * <p>Both listings accept the same optional feed filters ({@code category},
 * {@code skill}, {@code budgetMin}/{@code budgetMax},
 * {@code experienceLevel}, {@code location}, {@code keyword}). Every filter
 * parameter is {@code null}-safe: a {@code null} value disables that
 * condition, and passing all {@code null}s returns the unfiltered feed. All
 * filters are ANDed together and evaluated in the database — no results are
 * loaded into memory.
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
     * Returns the browse feed, most recently created first, filtered by the
     * given optional criteria.
     *
     * <p>The {@code skill} filter matches against the persisted JSON
     * {@code skillsRequired} array (case-insensitive substring — a project
     * needing "After Effects" matches {@code skill=after}). The {@code keyword}
     * filter matches the title or the description.
     *
     * @param category        exact category (case-insensitive) or {@code null}
     * @param skill           required-skill substring or {@code null}
     * @param budgetMin       inclusive lower budget bound or {@code null}
     * @param budgetMax       inclusive upper budget bound or {@code null}
     * @param experienceLevel exact experience level (case-insensitive) or {@code null}
     * @param location        exact location (case-insensitive) or {@code null}
     * @param keyword         title/description substring or {@code null}
     * @return the filtered feed (newest first, deterministic); empty when no
     *         project matches
     */
    @Query("""
            select p from Project p
            where (:category is null or lower(p.category) = lower(:category))
              and (:skill is null or lower(cast(p.skillsRequired as string)) like lower(concat('%', :skill, '%')))
              and (:budgetMin is null or p.budget >= :budgetMin)
              and (:budgetMax is null or p.budget <= :budgetMax)
              and (:experienceLevel is null or lower(p.experienceLevel) = lower(:experienceLevel))
              and (:location is null or lower(p.location) = lower(:location))
              and (:keyword is null or lower(p.title) like lower(concat('%', :keyword, '%'))
                   or lower(p.description) like lower(concat('%', :keyword, '%')))
            order by p.createdAt desc, p.id desc
            """)
    List<Project> findAllByFilters(@Param("category") String category,
                                   @Param("skill") String skill,
                                   @Param("budgetMin") BigDecimal budgetMin,
                                   @Param("budgetMax") BigDecimal budgetMax,
                                   @Param("experienceLevel") String experienceLevel,
                                   @Param("location") String location,
                                   @Param("keyword") String keyword);

    /**
     * Returns the projects owned by the given user, most recently created
     * first, filtered by the same optional criteria as the browse feed.
     *
     * @param userId          the owning user's id (matches the JWT {@code userId} claim)
     * @param category        exact category (case-insensitive) or {@code null}
     * @param skill           required-skill substring or {@code null}
     * @param budgetMin       inclusive lower budget bound or {@code null}
     * @param budgetMax       inclusive upper budget bound or {@code null}
     * @param experienceLevel exact experience level (case-insensitive) or {@code null}
     * @param location        exact location (case-insensitive) or {@code null}
     * @param keyword         title/description substring or {@code null}
     * @return the user's filtered projects (newest first, deterministic);
     *         empty when the user has none or nothing matches
     */
    @Query("""
            select p from Project p
            where p.userId = :userId
              and (:category is null or lower(p.category) = lower(:category))
              and (:skill is null or lower(cast(p.skillsRequired as string)) like lower(concat('%', :skill, '%')))
              and (:budgetMin is null or p.budget >= :budgetMin)
              and (:budgetMax is null or p.budget <= :budgetMax)
              and (:experienceLevel is null or lower(p.experienceLevel) = lower(:experienceLevel))
              and (:location is null or lower(p.location) = lower(:location))
              and (:keyword is null or lower(p.title) like lower(concat('%', :keyword, '%'))
                   or lower(p.description) like lower(concat('%', :keyword, '%')))
            order by p.createdAt desc, p.id desc
            """)
    List<Project> findByUserIdAndFilters(@Param("userId") UUID userId,
                                         @Param("category") String category,
                                         @Param("skill") String skill,
                                         @Param("budgetMin") BigDecimal budgetMin,
                                         @Param("budgetMax") BigDecimal budgetMax,
                                         @Param("experienceLevel") String experienceLevel,
                                         @Param("location") String location,
                                         @Param("keyword") String keyword);
}
