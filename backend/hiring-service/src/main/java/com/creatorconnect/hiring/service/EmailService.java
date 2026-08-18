package com.creatorconnect.hiring.service;

/**
 * Transactional email sender for application lifecycle events.
 *
 * <p>All methods are fire-and-forget: a failure to send an email must never
 * roll back the triggering business operation.  Callers should wrap calls
 * in a try/catch or rely on the default implementation swallowing exceptions.
 */
public interface EmailService {

    /**
     * Sends a "new application received" email to the project owner.
     *
     * @param recipientEmail the project owner's email address
     * @param recipientName  the project owner's display name
     * @param projectTitle   the project title
     * @param freelancerName the freelancer's display name (may be email prefix)
     */
    void sendApplicationReceived(String recipientEmail, String recipientName,
                                 String projectTitle, String freelancerName);

    /**
     * Sends an "application accepted" email to the freelancer.
     *
     * @param recipientEmail the freelancer's email address
     * @param recipientName  the freelancer's display name
     * @param projectTitle   the project title
     * @param creatorName    the creator's display name (may be email prefix)
     */
    void sendApplicationAccepted(String recipientEmail, String recipientName,
                                 String projectTitle, String creatorName);

    /**
     * Sends an "application rejected" email to the freelancer.
     *
     * @param recipientEmail the freelancer's email address
     * @param recipientName  the freelancer's display name
     * @param projectTitle   the project title
     * @param creatorName    the creator's display name (may be email prefix)
     */
    void sendApplicationRejected(String recipientEmail, String recipientName,
                                 String projectTitle, String creatorName);
}
