package com.creatorconnect.hiring.entity;

/**
 * Categorises in-app notifications by the event that triggered them.
 *
 * <p>Each type carries a fixed title and message template on the frontend so
 * the backend only needs to persist the type and the resource reference; the
 * presentation layer decides the icon, colour and wording.
 */
public enum NotificationType {

    /** A freelancer submitted an application to the creator's project. */
    APPLICATION_RECEIVED,

    /** The creator accepted the freelancer's application. */
    APPLICATION_ACCEPTED,

    /** The creator rejected the freelancer's application. */
    APPLICATION_REJECTED,

    /** The freelancer withdrew a pending application. */
    APPLICATION_WITHDRAWN,

    /** The creator submitted a review for the freelancer. */
    REVIEW_RECEIVED
}
