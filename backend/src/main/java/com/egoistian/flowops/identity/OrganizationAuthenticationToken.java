package com.egoistian.flowops.identity;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.util.Collection;

public final class OrganizationAuthenticationToken extends AbstractAuthenticationToken {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Object principal;
    private Object credentials;
    private final String organizationKey;

    private OrganizationAuthenticationToken(
            String organizationKey,
            Object principal,
            Object credentials,
            Collection<? extends GrantedAuthority> authorities,
            boolean authenticated) {
        super(authorities);
        this.organizationKey = organizationKey;
        this.principal = principal;
        this.credentials = credentials;
        super.setAuthenticated(authenticated);
    }

    public static OrganizationAuthenticationToken unauthenticated(
            String organizationKey, String email, String password) {
        return new OrganizationAuthenticationToken(
                organizationKey, email, password, java.util.List.of(), false);
    }

    public static OrganizationAuthenticationToken authenticated(
            String organizationKey,
            AuthenticatedUser principal,
            Collection<? extends GrantedAuthority> authorities) {
        return new OrganizationAuthenticationToken(
                organizationKey, principal, null, authorities, true);
    }

    public String organizationKey() {
        return organizationKey;
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        credentials = null;
    }
}
