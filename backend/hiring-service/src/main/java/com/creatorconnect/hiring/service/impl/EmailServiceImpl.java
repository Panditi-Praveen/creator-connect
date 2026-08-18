package com.creatorconnect.hiring.service.impl;

import com.creatorconnect.hiring.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Concrete {@link EmailService} implementation using Spring Boot's
 * {@link JavaMailSender}.
 *
 * <p>Emails are sent asynchronously ({@code @Async}) so they never block the
 * calling thread.  If sending fails, the exception is logged and swallowed —
 * the triggering business operation is never affected.
 *
 * <p>Configuration is read from {@code app.email.*} properties in
 * {@code application.yml}.  When SMTP is not configured (dev/test), the
 * service logs a warning and silently skips sending.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;
    private final boolean enabled;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${app.email.from-address:noreply@creatorconnect.app}") String fromAddress,
            @Value("${app.email.from-name:CreatorConnect}") String fromName,
            @Value("${app.email.enabled:false}") boolean enabled) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.enabled = enabled;
    }

    @Override
    @Async
    public void sendApplicationReceived(String recipientEmail, String recipientName,
                                        String projectTitle, String freelancerName) {
        String subject = "New application received — " + projectTitle;
        String body = buildApplicationReceivedHtml(recipientName, projectTitle, freelancerName);
        send(recipientEmail, subject, body);
    }

    @Override
    @Async
    public void sendApplicationAccepted(String recipientEmail, String recipientName,
                                        String projectTitle, String creatorName) {
        String subject = "Your application has been accepted — " + projectTitle;
        String body = buildApplicationAcceptedHtml(recipientName, projectTitle, creatorName);
        send(recipientEmail, subject, body);
    }

    @Override
    @Async
    public void sendApplicationRejected(String recipientEmail, String recipientName,
                                        String projectTitle, String creatorName) {
        String subject = "Application update — " + projectTitle;
        String body = buildApplicationRejectedHtml(recipientName, projectTitle, creatorName);
        send(recipientEmail, subject, body);
    }

    // ---- Internal helpers ----

    private void send(String to, String subject, String htmlBody) {
        if (!enabled) {
            log.info("Email disabled — skipping send to {}: {}", to, subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception ex) {
            // Email failure must never roll back the triggering operation.
            log.warn("Failed to send email to {}: {} — {}", to, subject, ex.getMessage());
        }
    }

    // ---- HTML template builders ----

    private String buildApplicationReceivedHtml(String recipientName, String projectTitle,
                                                 String freelancerName) {
        return wrapHtml(
                "New Application Received",
                "<p>Hello <strong>" + escape(recipientName) + "</strong>,</p>"
              + "<p>A new application has been submitted for your project:</p>"
              + "<div style=\"background:#f8fafc;border-left:4px solid #4f46e5;padding:12px 16px;margin:16px 0;border-radius:6px;\">"
              + "<strong>" + escape(projectTitle) + "</strong></div>"
              + "<p><strong>" + escape(freelancerName) + "</strong> has applied to your project. "
              + "Review the application in your Hiring dashboard.</p>"
              + "<p style=\"margin-top:24px;\">"
              + "<a href=\"#\" style=\"background:#4f46e5;color:#fff;padding:10px 20px;border-radius:6px;text-decoration:none;font-weight:600;\">View Applications</a>"
              + "</p>"
              + "<p style=\"color:#64748b;font-size:13px;margin-top:24px;\">This is an automated notification from CreatorConnect.</p>"
        );
    }

    private String buildApplicationAcceptedHtml(String recipientName, String projectTitle,
                                                 String creatorName) {
        return wrapHtml(
                "Application Accepted",
                "<p>Hello <strong>" + escape(recipientName) + "</strong>,</p>"
              + "<p>Great news! Your application for the project:</p>"
              + "<div style=\"background:#ecfdf5;border-left:4px solid #059669;padding:12px 16px;margin:16px 0;border-radius:6px;\">"
              + "<strong>" + escape(projectTitle) + "</strong></div>"
              + "<p>has been <strong style=\"color:#059669;\">accepted</strong> by <strong>" + escape(creatorName) + "</strong>.</p>"
              + "<p>Log in to CreatorConnect to view the project details and get started.</p>"
              + "<p style=\"margin-top:24px;\">"
              + "<a href=\"#\" style=\"background:#4f46e5;color:#fff;padding:10px 20px;border-radius:6px;text-decoration:none;font-weight:600;\">View Project</a>"
              + "</p>"
              + "<p style=\"color:#64748b;font-size:13px;margin-top:24px;\">This is an automated notification from CreatorConnect.</p>"
        );
    }

    private String buildApplicationRejectedHtml(String recipientName, String projectTitle,
                                                 String creatorName) {
        return wrapHtml(
                "Application Update",
                "<p>Hello <strong>" + escape(recipientName) + "</strong>,</p>"
              + "<p>We wanted to let you know that your application for the project:</p>"
              + "<div style=\"background:#fef2f2;border-left:4px solid #dc2626;padding:12px 16px;margin:16px 0;border-radius:6px;\">"
              + "<strong>" + escape(projectTitle) + "</strong></div>"
              + "<p>was not selected by <strong>" + escape(creatorName) + "</strong>.</p>"
              + "<p>Don't worry — there are plenty of other opportunities. Keep exploring projects on CreatorConnect.</p>"
              + "<p style=\"margin-top:24px;\">"
              + "<a href=\"#\" style=\"background:#4f46e5;color:#fff;padding:10px 20px;border-radius:6px;text-decoration:none;font-weight:600;\">Browse Projects</a>"
              + "</p>"
              + "<p style=\"color:#64748b;font-size:13px;margin-top:24px;\">This is an automated notification from CreatorConnect.</p>"
        );
    }

    private String wrapHtml(String title, String bodyContent) {
        return "<!DOCTYPE html>"
             + "<html lang=\"en\"><head><meta charset=\"UTF-8\">"
             + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
             + "<title>" + escape(title) + "</title></head>"
             + "<body style=\"margin:0;padding:0;background:#f7f8fc;font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;\">"
             + "<div style=\"max-width:560px;margin:0 auto;padding:32px 20px;\">"
             + "<div style=\"text-align:center;margin-bottom:24px;\">"
             + "<span style=\"display:inline-block;background:linear-gradient(135deg,#4f46e5,#7c3aed);color:#fff;font-weight:700;font-size:18px;padding:8px 14px;border-radius:8px;\">CC</span>"
             + " <span style=\"font-size:20px;font-weight:700;color:#0f172a;\">CreatorConnect</span>"
             + "</div>"
             + "<div style=\"background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;padding:28px 24px;\">"
             + "<h1 style=\"margin:0 0 16px;font-size:20px;color:#0f172a;\">" + escape(title) + "</h1>"
             + bodyContent
             + "</div>"
             + "<p style=\"text-align:center;color:#94a3b8;font-size:12px;margin-top:24px;\">"
             + "© " + java.time.Year.now().getValue() + " CreatorConnect. All rights reserved.</p>"
             + "</div></body></html>";
    }

    /** Minimal HTML entity escaping to prevent XSS in email content. */
    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
