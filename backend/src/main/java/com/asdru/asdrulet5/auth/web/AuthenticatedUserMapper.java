package com.asdru.asdrulet5.auth.web;

import com.asdru.asdrulet5.auth.AuthenticatedUser;
import lombok.experimental.UtilityClass;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@UtilityClass
public class AuthenticatedUserMapper {

    public AuthenticatedUser from(OidcUser principal) {
        return new AuthenticatedUser(principal.getSubject(), principal.getFullName(), principal.getPicture());
    }
}
