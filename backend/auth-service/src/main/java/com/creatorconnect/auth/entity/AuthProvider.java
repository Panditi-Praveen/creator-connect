package com.creatorconnect.auth.entity;

/**
 * The identity provider used to create a user account.
 *
 * <ul>
 *   <li>{@code LOCAL} — registered with email + password on CreatorConnect</li>
 *   <li>{@code GOOGLE} / {@code GITHUB} — registered via OAuth (future work)</li>
 * </ul>
 *
 * Accounts created through the standard register endpoint always use
 * {@link #LOCAL}.
 */
public enum AuthProvider {

    LOCAL,
    GOOGLE,
    GITHUB
}
