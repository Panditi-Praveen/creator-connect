package com.creatorconnect.project.entity;

/**
 * Lifecycle state of a posted project.
 *
 * <p>Stored as a string ({@code EnumType.STRING}) in the {@code status}
 * column so the stored value stays readable and schema-safe when new states
 * are introduced. A project is created {@link #OPEN} and moves through the
 * lifecycle as applications are received and work proceeds.
 */
public enum ProjectStatus {

    /**
     * The project is published and accepting applications.
     */
    OPEN,

    /**
     * A creator has been selected and work is underway.
     */
    IN_PROGRESS,

    /**
     * The project has finished.
     */
    COMPLETED,

    /**
     * The project was withdrawn before completion.
     */
    CANCELLED
}
