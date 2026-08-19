package com.egoistian.flowops.identity;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseOrganizationAuthenticationProvider implements AuthenticationProvider {
    private final DatabaseUserDetailsService users;
    private final PasswordEncoder passwordEncoder;

    public DatabaseOrganizationAuthenticationProvider(
            DatabaseUserDetailsService users,
            PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        OrganizationAuthenticationToken token = (OrganizationAuthenticationToken) authentication;
        UserAccountRepository.UserAccount account = users.load(
                token.organizationKey(), token.getPrincipal().toString());
        if (!passwordEncoder.matches(token.getCredentials().toString(), account.passwordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        List<SimpleGrantedAuthority> authorities = account.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        AuthenticatedUser principal = new AuthenticatedUser(
                account.userId(), account.organizationId(), account.displayName(), account.roles());
        return OrganizationAuthenticationToken.authenticated(
                token.organizationKey(), principal, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OrganizationAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
