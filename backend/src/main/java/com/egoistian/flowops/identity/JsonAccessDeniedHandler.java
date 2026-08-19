package com.egoistian.flowops.identity;

import com.egoistian.flowops.shared.api.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        boolean csrfFailure = exception instanceof CsrfException;
        String code = csrfFailure ? "CSRF_FAILED" : "ACCESS_DENIED";
        String detail = csrfFailure
                ? "A valid CSRF token is required."
                : "The current user cannot perform this action.";
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "https://flowops.example/problems/" + code.toLowerCase().replace('_', '-'));
        problem.put("title", "Forbidden");
        problem.put("status", HttpServletResponse.SC_FORBIDDEN);
        problem.put("detail", detail);
        problem.put("instance", request.getRequestURI());
        problem.put("code", code);
        problem.put("traceId", request.getAttribute(TraceIdFilter.REQUEST_ATTRIBUTE));
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
