package com.egoistian.flowops.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/session")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/csrf")
    CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken(), token.getHeaderName());
    }

    @GetMapping
    SessionResponse current(@AuthenticationPrincipal AuthenticatedUser user) {
        return SessionResponse.from(user);
    }

    @PostMapping("/login")
    ResponseEntity<SessionResponse> login(
            @Valid @RequestBody LoginRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                OrganizationAuthenticationToken.unauthenticated(
                        requestBody.organizationKey(), requestBody.email(), requestBody.password()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return ResponseEntity.ok(SessionResponse.from((AuthenticatedUser) authentication.getPrincipal()));
    }

    public record LoginRequest(
            @NotBlank String organizationKey,
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record SessionResponse(
            UUID userId,
            UUID organizationId,
            String displayName,
            List<String> roles) {
        static SessionResponse from(AuthenticatedUser user) {
            return new SessionResponse(
                    user.userId(), user.organizationId(), user.displayName(), user.roles().stream().sorted().toList());
        }
    }

    public record CsrfResponse(String token, String headerName) {
    }
}
