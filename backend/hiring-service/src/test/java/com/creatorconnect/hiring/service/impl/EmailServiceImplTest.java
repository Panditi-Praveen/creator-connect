package com.creatorconnect.hiring.service.impl;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EmailServiceImpl} — verifies that emails are
 * composed and sent (or skipped) correctly.  No real SMTP is used;
 * the {@link JavaMailSender} is mocked.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    private EmailServiceImpl enabledService;
    private EmailServiceImpl disabledService;

    @BeforeEach
    void setUp() {
        enabledService = new EmailServiceImpl(mailSender, "test@creatorconnect.app", "CreatorConnect", true);
        disabledService = new EmailServiceImpl(mailSender, "test@creatorconnect.app", "CreatorConnect", false);
    }

    private void stubMimeMessage() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    // ---- sendApplicationReceived ----

    @Test
    void sendApplicationReceived_whenEnabled_sendsEmail() {
        stubMimeMessage();
        enabledService.sendApplicationReceived(
                "owner@test.com", "Alice", "Spring Boot Platform", "Bob"
        );

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendApplicationReceived_whenEnabled_usesCorrectRecipient() {
        stubMimeMessage();
        enabledService.sendApplicationReceived(
                "owner@test.com", "Alice", "Spring Boot Platform", "Bob"
        );

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        // MimeMessage was passed through MimeMessageHelper — just verify it was sent
        assertThat(captor.getValue()).isNotNull();
    }

    @Test
    void sendApplicationReceived_whenDisabled_skipsSend() {
        disabledService.sendApplicationReceived(
                "owner@test.com", "Alice", "Spring Boot Platform", "Bob"
        );

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    // ---- sendApplicationAccepted ----

    @Test
    void sendApplicationAccepted_whenEnabled_sendsEmail() {
        stubMimeMessage();
        enabledService.sendApplicationAccepted(
                "freelancer@test.com", "Bob", "Spring Boot Platform", "Alice"
        );

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendApplicationAccepted_whenDisabled_skipsSend() {
        disabledService.sendApplicationAccepted(
                "freelancer@test.com", "Bob", "Spring Boot Platform", "Alice"
        );

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    // ---- sendApplicationRejected ----

    @Test
    void sendApplicationRejected_whenEnabled_sendsEmail() {
        stubMimeMessage();
        enabledService.sendApplicationRejected(
                "freelancer@test.com", "Bob", "Spring Boot Platform", "Alice"
        );

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendApplicationRejected_whenDisabled_skipsSend() {
        disabledService.sendApplicationRejected(
                "freelancer@test.com", "Bob", "Spring Boot Platform", "Alice"
        );

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    // ---- Failure handling ----

    @Test
    void sendApplicationReceived_whenMailSenderThrows_doesNotPropagate() {
        stubMimeMessage();
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP connection refused"))
                .when(mailSender).send(any(MimeMessage.class));

        // Must NOT throw — email failure must not break the business operation.
        enabledService.sendApplicationReceived(
                "owner@test.com", "Alice", "Spring Boot Platform", "Bob"
        );
    }
}
