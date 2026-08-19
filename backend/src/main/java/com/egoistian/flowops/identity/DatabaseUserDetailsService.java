package com.egoistian.flowops.identity;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService {
    private final UserAccountRepository accounts;

    public DatabaseUserDetailsService(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    public UserAccountRepository.UserAccount load(String organizationKey, String email) {
        return accounts.findActive(organizationKey, email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    }
}
