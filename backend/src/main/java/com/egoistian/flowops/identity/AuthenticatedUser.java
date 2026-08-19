package com.egoistian.flowops.identity;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        UUID organizationId,
        String displayName,
        Set<String> roles) implements Serializable {
    public AuthenticatedUser {
        roles = Set.copyOf(roles);
    }
}
