package com.asdru.asdrulet5.auth;

/**
 * Identity-provider-agnostic view of the signed-in user, so downstream
 * services never depend on Spring Security's OAuth2/OIDC types directly.
 */
public record AuthenticatedUser(String id, String displayName, String avatarUrl) {
}
