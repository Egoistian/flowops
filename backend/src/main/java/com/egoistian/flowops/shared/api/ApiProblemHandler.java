package com.egoistian.flowops.shared.api;

import com.egoistian.flowops.procurement.application.ProcurementNotFound;
import com.egoistian.flowops.procurement.application.RequestVersionConflict;
import com.egoistian.flowops.procurement.domain.DomainRuleViolation;
import com.egoistian.flowops.shared.idempotency.IdempotencyInProgress;
import com.egoistian.flowops.shared.idempotency.IdempotencyKeyReused;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiProblemHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "One or more fields are invalid.",
                request);
        List<FieldErrorView> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldErrorView(
                        error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldErrorView::field))
                .toList();
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(ProcurementNotFound.class)
    ProblemDetail notFound(
            ProcurementNotFound exception,
            HttpServletRequest request) {
        return problem(
                HttpStatus.NOT_FOUND,
                "PROCUREMENT_NOT_FOUND",
                "The requested procurement record was not found.",
                request);
    }

    @ExceptionHandler(IdempotencyKeyReused.class)
    ProblemDetail idempotencyKeyReused(
            IdempotencyKeyReused exception,
            HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                exception.code(),
                "The idempotency key was already used for another request.",
                request);
    }

    @ExceptionHandler(IdempotencyInProgress.class)
    ProblemDetail idempotencyInProgress(
            IdempotencyInProgress exception,
            HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                "IDEMPOTENCY_REQUEST_IN_PROGRESS",
                "The original request is still being processed.",
                request);
    }

    @ExceptionHandler(RequestVersionConflict.class)
    ProblemDetail versionConflict(
            RequestVersionConflict exception,
            HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                exception.code(),
                "Another user changed this request. Reload the latest version.",
                request);
    }

    @ExceptionHandler(DomainRuleViolation.class)
    ProblemDetail domainRule(
            DomainRuleViolation exception,
            HttpServletRequest request) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                exception.code(),
                "The requested state change is not allowed.",
                request);
    }

    private ProblemDetail problem(
            HttpStatus status,
            String code,
            String detail,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(
                "https://flowops.example/problems/"
                        + code.toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("traceId", request.getAttribute(
                TraceIdFilter.REQUEST_ATTRIBUTE));
        return problem;
    }

    public record FieldErrorView(String field, String message) {
    }
}
