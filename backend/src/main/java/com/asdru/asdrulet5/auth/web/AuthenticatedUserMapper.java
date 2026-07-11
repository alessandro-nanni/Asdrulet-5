package com.asdru.asdrulet5.auth.web;

import com.asdru.asdrulet5.auth.AuthenticatedUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public final class AuthenticatedUserMapper {

    private AuthenticatedUserMapper() {
    }

    public static AuthenticatedUser from(OidcUser principal) {
        return new AuthenticatedUser(principal.getSubject(), principal.getFullName(), principal.getPicture());
    }
}
