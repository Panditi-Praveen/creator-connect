package com.creatorconnect.hiring.entity;

/**
 * Lifecycle state of a project application.
 *
 * <p>Stored as a string ({@code EnumType.STRING}) in the {@code status}
 * column so the stored value stays readable and schema-safe when new states
 * are introduced. An application is created {@link #PENDING} and moves
 * through the lifecycle as the creator decides on it or the freelancer
 * withdraws.
 */
public enum ApplicationStatus {

    /**
     * Submitted and awaiting the creator's decision.
     */
    PENDING,

    /**
     * The creator accepted the application.
     */
    ACCEPTED,

    /**
     * The creator rejected the application.
     */
    REJECTED,

    /**
     * The freelancer withdrew the application before a decision.
     */
    WITHDRAWN
}
