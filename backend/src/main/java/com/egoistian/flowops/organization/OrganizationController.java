package com.egoistian.flowops.organization;

import com.egoistian.flowops.identity.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {
    private final OrganizationRepository organizations;

    public OrganizationController(OrganizationRepository organizations) {
        this.organizations = organizations;
    }

    @GetMapping("/{organizationId}")
    OrganizationRepository.OrganizationSummary get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID organizationId) {
        return organizations.findScoped(user.organizationId(), organizationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Organization not found"));
    }
}
