package com.creatorconnect.hiring.service.impl;

import com.creatorconnect.hiring.dto.request.ReviewRequest;
import com.creatorconnect.hiring.dto.response.FreelancerReviewsResponse;
import com.creatorconnect.hiring.dto.response.ReviewResponse;
import com.creatorconnect.hiring.entity.ApplicationStatus;
import com.creatorconnect.hiring.entity.Review;
import com.creatorconnect.hiring.exception.DuplicateReviewException;
import com.creatorconnect.hiring.exception.ProjectNotFoundException;
import com.creatorconnect.hiring.exception.ReviewAccessDeniedException;
import com.creatorconnect.hiring.exception.ReviewValidationException;
import com.creatorconnect.hiring.feign.ProjectClientService;
import com.creatorconnect.hiring.feign.ProjectResponse;
import com.creatorconnect.hiring.feign.ProjectStatus;
import com.creatorconnect.hiring.mapper.ReviewMapper;
import com.creatorconnect.hiring.repository.ApplicationRepository;
import com.creatorconnect.hiring.repository.ReviewRepository;
import com.creatorconnect.hiring.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ReviewServiceImpl} — the creator-and-owner-only /
 * hired-first / one-review-per-pair rules plus the average-rating aggregation,
 * using mocked collaborators.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    private static final UUID CREATOR_ID = UUID.fromString("9d2b4f79-c75f-68ff-d402-6ea024a1bd13");
    private static final UUID OTHER_USER_ID = UUID.fromString("8c1a3e68-b64e-57ee-c3f1-5d9f1390ac02");
    private static final UUID FREELANCER_ID = UUID.fromString("7b092f57-a53d-46dd-b2e0-4c8f0289fb91");
    private static final UUID PROJECT_ID = UUID.fromString("6a3c2e18-d46a-4f8b-9e0c-1b2d3e4f5a6b");
    private static final UUID REVIEW_ID = UUID.fromString("5b4d1f07-e35b-4e9a-b1d5-0a9f8b7c6d5e");

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private ProjectClientService projectClientService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    void createReview_byOwner_persistsAndReturnsResponse() {
        ReviewRequest request = reviewRequest();
        Review entity = review(5);
        when(projectClientService.getProject(PROJECT_ID)).thenReturn(ownedProject());
        when(reviewRepository.existsByProjectIdAndFreelancerId(PROJECT_ID, FREELANCER_ID)).thenReturn(false);
        when(applicationRepository.existsByProjectIdAndFreelancerIdAndStatus(
                PROJECT_ID, FREELANCER_ID, ApplicationStatus.ACCEPTED)).thenReturn(true);
        when(reviewMapper.toEntity(CREATOR_ID, request)).thenReturn(entity);
        when(reviewRepository.save(entity)).thenReturn(entity);
        when(reviewMapper.toResponse(entity)).thenReturn(reviewResponse(5));

        ReviewResponse response = reviewService.createReview(CREATOR_ID, "CREATOR", request);

        assertThat(response.getCreatorId()).isEqualTo(CREATOR_ID);
        assertThat(response.getFreelancerId()).isEqualTo(FREELANCER_ID);
        assertThat(response.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(response.getRating()).isEqualTo(5);
        verify(reviewRepository).save(entity);
    }

    @Test
    void createReview_byFreelancer_throwsAccessDenied() {
        assertThatThrownBy(() -> reviewService.createReview(FREELANCER_ID, "FREELANCER", reviewRequest()))
                .isInstanceOf(ReviewAccessDeniedException.class);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void createReview_whenAlreadyReviewed_throwsDuplicateReview() {
        when(reviewRepository.existsByProjectIdAndFreelancerId(PROJECT_ID, FREELANCER_ID)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(CREATOR_ID, "CREATOR", reviewRequest()))
                .isInstanceOf(DuplicateReviewException.class);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void createReview_byNonOwner_throwsAccessDenied() {
        when(reviewRepository.existsByProjectIdAndFreelancerId(PROJECT_ID, FREELANCER_ID)).thenReturn(false);
        when(projectClientService.getProject(PROJECT_ID)).thenReturn(foreignProject());

        assertThatThrownBy(() -> reviewService.createReview(CREATOR_ID, "CREATOR", reviewRequest()))
                .isInstanceOf(ReviewAccessDeniedException.class)
                .hasMessageContaining("owner");
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void createReview_whenProjectMissing_throwsProjectNotFound() {
        when(reviewRepository.existsByProjectIdAndFreelancerId(PROJECT_ID, FREELANCER_ID)).thenReturn(false);
        when(projectClientService.getProject(PROJECT_ID))
                .thenThrow(new ProjectNotFoundException("Project not found: " + PROJECT_ID));

        assertThatThrownBy(() -> reviewService.createReview(CREATOR_ID, "CREATOR", reviewRequest()))
                .isInstanceOf(ProjectNotFoundException.class);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void createReview_whenProjectNotCompleted_throwsValidation() {
        when(reviewRepository.existsByProjectIdAndFreelancerId(PROJECT_ID, FREELANCER_ID)).thenReturn(false);
        when(projectClientService.getProject(PROJECT_ID)).thenReturn(openProject());

        assertThatThrownBy(() -> reviewService.createReview(CREATOR_ID, "CREATOR", reviewRequest()))
                .isInstanceOf(ReviewValidationException.class)
                .hasMessageContaining("completed");
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void createReview_whenFreelancerNotHired_throwsValidation() {
        when(projectClientService.getProject(PROJECT_ID)).thenReturn(ownedProject());
        when(reviewRepository.existsByProjectIdAndFreelancerId(PROJECT_ID, FREELANCER_ID)).thenReturn(false);
        when(applicationRepository.existsByProjectIdAndFreelancerIdAndStatus(
                PROJECT_ID, FREELANCER_ID, ApplicationStatus.ACCEPTED)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.createReview(CREATOR_ID, "CREATOR", reviewRequest()))
                .isInstanceOf(ReviewValidationException.class);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void getFreelancerReviews_returnsReviewsNewestFirstWithRoundedAverage() {
        Review fiveStar = review(5);
        Review fourStar = review(4);
        when(reviewRepository.findByFreelancerIdOrderByCreatedAtDescIdDesc(FREELANCER_ID))
                .thenReturn(List.of(fiveStar, fourStar));
        when(reviewMapper.toResponse(fiveStar)).thenReturn(reviewResponse(5));
        when(reviewMapper.toResponse(fourStar)).thenReturn(reviewResponse(4));

        FreelancerReviewsResponse summary = reviewService.getFreelancerReviews(FREELANCER_ID);

        assertThat(summary.getFreelancerId()).isEqualTo(FREELANCER_ID);
        assertThat(summary.getTotalReviews()).isEqualTo(2);
        assertThat(summary.getAverageRating()).isEqualTo(4.5);
        assertThat(summary.getReviews()).hasSize(2);
        assertThat(summary.getReviews().get(0).getRating()).isEqualTo(5);
    }

    @Test
    void getFreelancerReviews_whenNoReviews_returnsZeroAverageAndEmptyList() {
        when(reviewRepository.findByFreelancerIdOrderByCreatedAtDescIdDesc(FREELANCER_ID))
                .thenReturn(List.of());

        FreelancerReviewsResponse summary = reviewService.getFreelancerReviews(FREELANCER_ID);

        assertThat(summary.getFreelancerId()).isEqualTo(FREELANCER_ID);
        assertThat(summary.getTotalReviews()).isZero();
        assertThat(summary.getAverageRating()).isEqualTo(0.0);
        assertThat(summary.getReviews()).isEmpty();
    }

    @Test
    void getFreelancerReviews_roundsAverageToOneDecimalPlace() {
        Review fourStar = review(4);
        Review fiveStar = review(5);
        Review anotherFourStar = review(4);
        when(reviewRepository.findByFreelancerIdOrderByCreatedAtDescIdDesc(FREELANCER_ID))
                .thenReturn(List.of(fourStar, fiveStar, anotherFourStar));

        FreelancerReviewsResponse summary = reviewService.getFreelancerReviews(FREELANCER_ID);

        // (4 + 5 + 4) / 3 = 4.333... -> rounded to one decimal place = 4.3
        assertThat(summary.getAverageRating()).isEqualTo(4.3);
    }

    private ReviewRequest reviewRequest() {
        return ReviewRequest.builder()
                .projectId(PROJECT_ID)
                .freelancerId(FREELANCER_ID)
                .rating(5)
                .reviewText("Outstanding work, delivered ahead of schedule.")
                .build();
    }

    private Review review(Integer rating) {
        return Review.builder()
                .id(REVIEW_ID)
                .projectId(PROJECT_ID)
                .creatorId(CREATOR_ID)
                .freelancerId(FREELANCER_ID)
                .rating(rating)
                .reviewText("Outstanding work, delivered ahead of schedule.")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ReviewResponse reviewResponse(Integer rating) {
        return ReviewResponse.builder()
                .id(REVIEW_ID)
                .projectId(PROJECT_ID)
                .creatorId(CREATOR_ID)
                .freelancerId(FREELANCER_ID)
                .rating(rating)
                .reviewText("Outstanding work, delivered ahead of schedule.")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ProjectResponse ownedProject() {
        return new ProjectResponse(PROJECT_ID, "Test Project", CREATOR_ID, ProjectStatus.COMPLETED);
    }

    private ProjectResponse openProject() {
        return new ProjectResponse(PROJECT_ID, "Test Project", CREATOR_ID, ProjectStatus.OPEN);
    }

    private ProjectResponse foreignProject() {
        return new ProjectResponse(PROJECT_ID, "Test Project", OTHER_USER_ID, ProjectStatus.COMPLETED);
    }
}
