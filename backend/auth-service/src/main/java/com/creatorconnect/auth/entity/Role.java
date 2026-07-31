package com.creatorconnect.auth.entity;

/**
 * Platform roles used for role-based access control.
 *
 * <ul>
 *   <li>{@code ADMIN} — platform administration</li>
 *   <li>{@code CREATOR} — content creators who post projects</li>
 *   <li>{@code FREELANCER} — creative professionals who apply to projects</li>
 * </ul>
 *
 * Stored as a {@code VARCHAR} in the database (see
 * {@link User#getRole()}), never as an ordinal.
 */
public enum Role {

    ADMIN,
    CREATOR,
    FREELANCER
}
