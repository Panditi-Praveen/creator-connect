package com.creatorconnect.hiring.controller;

import com.creatorconnect.hiring.config.SecurityBeansConfig;
import com.creatorconnect.hiring.dto.response.FreelancerReviewsResponse;
import com.creatorconnect.hiring.dto.response.ReviewResponse;
import com.creatorconnect.hiring.exception.DuplicateReviewException;
import com.creatorconnect.hiring.exception.ReviewAccessDeniedException;
import com.creatorconnect.hiring.exception.ReviewValidationException;
import com.creatorconnect.hiring.security.JwtAuthenticationEntryPoint;
import com.creatorconnect.hiring.security.JwtAuthenticationFilter;
import com.creatorconnect.hiring.security.JwtService;
import com.creatorconnect.hiring.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link ReviewController}.
 *
 * <p>{@code @WebMvcTest} loads only the controller slice; the security chain,
 * the JWT filter and the 401 entry point are recreated in a small test
 * configuration so the tests exercise the real bearer-token path (a mocked
 * {@link JwtService} makes any {@code Bearer <anything>} header authenticate
 * as {@link #CREATOR_ID} with the {@code CREATOR} role by default — individual
 * tests re-stub the role where a freelancer is needed). The global exception
 * handler (a {@code @ControllerAdvice}) is picked up automatically by the
 * slice.
 */
@WebMvcTest(
        value = ReviewController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@Import({SecurityBeansConfig.class, ReviewControllerTest.SecurityTestConfig.class})
class ReviewControllerTest {

    private static final UUID CREATOR_ID = UUID.fromString("9d2b4f79-c75f-68ff-d402-6ea024a1bd13");
    private static final UUID FREELANCER_ID = UUID.fromString("7b092f57-a53d-46dd-b2e0-4c8f0289fb91");
    private static final UUID PROJECT_ID = UUID.fromString("6a3c2e18-d46a-4f8b-9e0c-1b2d3e4f5a6b");
    private static final UUID REVIEW_ID = UUID.fromString("5b4d1f07-e35b-4e9a-b1d5-0a9f8b7c6d5e");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        when(jwtService.isValid(anyString())).thenReturn(true);
        when(jwtService.extractUserId(anyString())).thenReturn(CREATOR_ID);
        when(jwtService.extractUsername(anyString())).thenReturn("creator@gmail.com");
        when(jwtService.extractRole(anyString())).thenReturn("CREATOR");
    }

    @Test
    void submitReview_withValidTokenAndPayload_returns201() throws Exception {
        when(reviewService.createReview(eq(CREATOR_ID), eq("CREATOR"), any()))
                .thenReturn(reviewResponse(5));

        mockMvc.perform(post("/reviews")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validReviewPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Review submitted successfully"))
                .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.data.creatorId").value(CREATOR_ID.toString()))
                .andExpect(jsonPath("$.data.freelancerId").value(FREELANCER_ID.toString()))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.path").value("/reviews"));
    }

    @Test
    void submitReview_withMissingToken_returns401() throws Exception {
        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validReviewPayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void submitReview_withInvalidPayload_returns400() throws Exception {
        mockMvc.perform(post("/reviews")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void submitReview_withOutOfRangeRating_returns400() throws Exception {
        String payload = """
                {
                  "projectId": "%s",
                  "freelancerId": "%s",
                  "rating": 6
                }
                """.formatted(PROJECT_ID, FREELANCER_ID);

        mockMvc.perform(post("/reviews")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void submitReview_byFreelancer_returns403() throws Exception {
        when(jwtService.extractRole(anyString())).thenReturn("FREELANCER");
        when(reviewService.createReview(eq(CREATOR_ID), eq("FREELANCER"), any()))
                .thenThrow(new ReviewAccessDeniedException("Only creators can submit reviews"));

        mockMvc.perform(post("/reviews")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validReviewPayload()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void submitReview_duplicate_returns409() throws Exception {
        when(reviewService.createReview(eq(CREATOR_ID), eq("CREATOR"), any()))
                .thenThrow(new DuplicateReviewException("This freelancer has already been reviewed on this project"));

        mockMvc.perform(post("/reviews")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validReviewPayload()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void submitReview_whenFreelancerNotHired_returns400() throws Exception {
        when(reviewService.createReview(eq(CREATOR_ID), eq("CREATOR"), any()))
                .thenThrow(new ReviewValidationException(
                        "Only freelancers with an accepted application can be reviewed on this project"));

        mockMvc.perform(post("/reviews")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validReviewPayload()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getFreelancerReviews_returns200WithAverage() throws Exception {
        when(reviewService.getFreelancerReviews(FREELANCER_ID))
                .thenReturn(FreelancerReviewsResponse.builder()
                        .freelancerId(FREELANCER_ID)
                        .averageRating(4.5)
                        .totalReviews(1)
                        .reviews(List.of(reviewResponse(5)))
                        .build());

        mockMvc.perform(get("/reviews/freelancer/{freelancerId}", FREELANCER_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reviews retrieved successfully"))
                .andExpect(jsonPath("$.data.freelancerId").value(FREELANCER_ID.toString()))
                .andExpect(jsonPath("$.data.averageRating").value(4.5))
                .andExpect(jsonPath("$.data.totalReviews").value(1))
                .andExpect(jsonPath("$.data.reviews[0].rating").value(5));
    }

    @Test
    void getFreelancerReviews_withMalformedUuid_returns400() throws Exception {
        mockMvc.perform(get("/reviews/freelancer/not-a-uuid")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private String validReviewPayload() {
        return """
                {
                  "projectId": "%s",
                  "freelancerId": "%s",
                  "rating": 5,
                  "reviewText": "Outstanding work, delivered ahead of schedule."
                }
                """.formatted(PROJECT_ID, FREELANCER_ID);
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

    /**
     * Supplies only the two security beans the production
     * {@link SecurityBeansConfig} depends on: the real JWT filter (backed by
     * the mocked {@link JwtService}) and the real 401 entry point. The filter
     * chain itself is the production one — imported via
     * {@code @Import(SecurityBeansConfig.class)} — so the tests exercise the
     * exact same authorization rules as production.
     */
    @TestConfiguration
    static class SecurityTestConfig {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
            return new JwtAuthenticationFilter(jwtService);
        }

        @Bean
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
            return new JwtAuthenticationEntryPoint(objectMapper);
        }
    }
}
