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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ApplicationServiceImpl} — apply/view/decide/withdraw
 * flows plus the role and ownership rules, using mocked collaborators.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    private static final UUID FREELANCER_ID = UUID.fromString("7b092f57-a53d-46dd-b2e0-4c8f0289fb91");
    private static final UUID OTHER_USER_ID = UUID.fromString("8c1a3e68-b64e-57ee-c3f1-5d9f1390ac02");
    private static final UUID PROJECT_ID = UUID.fromString("9d2b4f79-c75f-68ff-d402-6ea024a1bd13");
    private static final UUID APPLICATION_ID = UUID.fromString("6a3c2e18-d46a-4f8b-9e0c-1b2d3e4f5a6b");

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationMapper applicationMapper;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Test
    void apply_byFreelancer_persistsAndReturnsResponse() {
        ApplicationRequest request = applicationRequest();
        Application entity = application(ApplicationStatus.PENDING);
        when(applicationRepository.existsByProjectIdAndFreelancerId(PROJECT_ID, FREELANCER_ID)).thenReturn(false);
        when(applicationMapper.toEntity(FREELANCER_ID, request)).thenReturn(entity);
        when(applicationRepository.save(entity)).thenReturn(entity);
        when(applicationMapper.toResponse(entity)).thenReturn(applicationResponse(ApplicationStatus.PENDING));

        ApplicationResponse response = applicationService.apply(FREELANCER_ID, "FREELANCER", request);

        assertThat(response.getFreelancerId()).isEqualTo(FREELANCER_ID);
        assertThat(response.getProjectId()).isEqualTo(PROJECT_ID);
        verify(applicationRepository).save(entity);
    }

    @Test
    void apply_byCreator_throwsAccessDenied() {
        assertThatThrownBy(() -> applicationService.apply(FREELANCER_ID, "CREATOR", applicationRequest()))
                .isInstanceOf(ApplicationAccessDeniedException.class);
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void apply_whenAlreadyApplied_throwsDuplicateApplication() {
        when(applicationRepository.existsByProjectIdAndFreelancerId(PROJECT_ID, FREELANCER_ID)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.apply(FREELANCER_ID, "FREELANCER", applicationRequest()))
                .isInstanceOf(DuplicateApplicationException.class);
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void getMyApplications_returnsMappedPage() {
        Application entity = application(ApplicationStatus.PENDING);
        PageRequest pageable = PageRequest.of(0, 20);
        when(applicationRepository.findByFreelancerId(FREELANCER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(applicationMapper.toResponse(entity)).thenReturn(applicationResponse(ApplicationStatus.PENDING));

        Page<ApplicationResponse> page = applicationService.getMyApplications(FREELANCER_ID, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getFreelancerId()).isEqualTo(FREELANCER_ID);
    }

    @Test
    void getApplicationsForProject_byCreator_returnsMappedPage() {
        Application entity = application(ApplicationStatus.PENDING);
        PageRequest pageable = PageRequest.of(0, 20);
        when(applicationRepository.findByProjectId(PROJECT_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(applicationMapper.toResponse(entity)).thenReturn(applicationResponse(ApplicationStatus.PENDING));

        Page<ApplicationResponse> page = applicationService.getApplicationsForProject("CREATOR", PROJECT_ID, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getProjectId()).isEqualTo(PROJECT_ID);
    }

    @Test
    void getApplicationsForProject_byFreelancer_throwsAccessDenied() {
        assertThatThrownBy(() ->
                applicationService.getApplicationsForProject("FREELANCER", PROJECT_ID, PageRequest.of(0, 20)))
                .isInstanceOf(ApplicationAccessDeniedException.class);
        verify(applicationRepository, never()).findByProjectId(any(), any());
    }

    @Test
    void updateStatus_byCreator_acceptsPendingApplication() {
        Application entity = application(ApplicationStatus.PENDING);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(entity));
        when(applicationRepository.save(entity)).thenReturn(entity);
        when(applicationMapper.toResponse(entity)).thenReturn(applicationResponse(ApplicationStatus.ACCEPTED));

        ApplicationResponse response = applicationService.updateStatus(
                "CREATOR", APPLICATION_ID, statusRequest(ApplicationStatus.ACCEPTED));

        assertThat(entity.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
    }

    @Test
    void updateStatus_byCreator_rejectsPendingApplication() {
        Application entity = application(ApplicationStatus.PENDING);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(entity));
        when(applicationRepository.save(entity)).thenReturn(entity);
        when(applicationMapper.toResponse(entity)).thenReturn(applicationResponse(ApplicationStatus.REJECTED));

        ApplicationResponse response = applicationService.updateStatus(
                "CREATOR", APPLICATION_ID, statusRequest(ApplicationStatus.REJECTED));

        assertThat(entity.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void updateStatus_byFreelancer_throwsAccessDenied() {
        assertThatThrownBy(() ->
                applicationService.updateStatus("FREELANCER", APPLICATION_ID, statusRequest(ApplicationStatus.ACCEPTED)))
                .isInstanceOf(ApplicationAccessDeniedException.class);
        verify(applicationRepository, never()).findById(any());
    }

    @Test
    void updateStatus_withIllegalDecision_throwsValidation() {
        assertThatThrownBy(() ->
                applicationService.updateStatus("CREATOR", APPLICATION_ID, statusRequest(ApplicationStatus.PENDING)))
                .isInstanceOf(ApplicationValidationException.class);
        assertThatThrownBy(() ->
                applicationService.updateStatus("CREATOR", APPLICATION_ID, statusRequest(ApplicationStatus.WITHDRAWN)))
                .isInstanceOf(ApplicationValidationException.class);
        verify(applicationRepository, never()).findById(any());
    }

    @Test
    void updateStatus_whenMissing_throwsNotFound() {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                applicationService.updateStatus("CREATOR", APPLICATION_ID, statusRequest(ApplicationStatus.ACCEPTED)))
                .isInstanceOf(ApplicationNotFoundException.class);
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void updateStatus_whenAlreadyDecided_throwsConflict() {
        Application entity = application(ApplicationStatus.REJECTED);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() ->
                applicationService.updateStatus("CREATOR", APPLICATION_ID, statusRequest(ApplicationStatus.ACCEPTED)))
                .isInstanceOf(ApplicationStatusConflictException.class);
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void withdraw_byOwner_setsWithdrawn() {
        Application entity = application(ApplicationStatus.PENDING);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(entity));
        when(applicationRepository.save(entity)).thenReturn(entity);

        applicationService.withdraw(FREELANCER_ID, APPLICATION_ID);

        assertThat(entity.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        verify(applicationRepository).save(entity);
    }

    @Test
    void withdraw_byNonOwner_throwsAccessDenied() {
        Application entity = application(ApplicationStatus.PENDING);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> applicationService.withdraw(OTHER_USER_ID, APPLICATION_ID))
                .isInstanceOf(ApplicationAccessDeniedException.class);
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void withdraw_whenMissing_throwsNotFound() {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.withdraw(FREELANCER_ID, APPLICATION_ID))
                .isInstanceOf(ApplicationNotFoundException.class);
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void withdraw_whenAlreadyDecided_throwsConflict() {
        Application entity = application(ApplicationStatus.ACCEPTED);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> applicationService.withdraw(FREELANCER_ID, APPLICATION_ID))
                .isInstanceOf(ApplicationStatusConflictException.class);
        verify(applicationRepository, never()).save(any(Application.class));
    }

    private ApplicationRequest applicationRequest() {
        return ApplicationRequest.builder()
                .projectId(PROJECT_ID)
                .proposal("I can deliver this in 2 weeks.")
                .expectedBudget(new BigDecimal("500.00"))
                .estimatedDuration("2 weeks")
                .build();
    }

    private UpdateApplicationStatusRequest statusRequest(ApplicationStatus status) {
        return UpdateApplicationStatusRequest.builder().status(status).build();
    }

    private Application application(ApplicationStatus status) {
        return Application.builder()
                .id(APPLICATION_ID)
                .projectId(PROJECT_ID)
                .freelancerId(FREELANCER_ID)
                .proposal("I can deliver this in 2 weeks.")
                .expectedBudget(new BigDecimal("500.00"))
                .estimatedDuration("2 weeks")
                .status(status)
                .build();
    }

    private ApplicationResponse applicationResponse(ApplicationStatus status) {
        return ApplicationResponse.builder()
                .id(APPLICATION_ID)
                .projectId(PROJECT_ID)
                .freelancerId(FREELANCER_ID)
                .proposal("I can deliver this in 2 weeks.")
                .expectedBudget(new BigDecimal("500.00"))
                .estimatedDuration("2 weeks")
                .status(status)
                .build();
    }
}
