package com.egoistian.flowops.procurement.api;

import com.egoistian.flowops.identity.AuthenticatedUser;
import com.egoistian.flowops.procurement.application.CreateProcurementDraft;
import com.egoistian.flowops.procurement.application.ApprovalResult;
import com.egoistian.flowops.procurement.application.ApproveProcurementRequest;
import com.egoistian.flowops.procurement.application.ProcurementCommands.ApproveCommand;
import com.egoistian.flowops.procurement.application.ProcurementCommands.CreateDraftCommand;
import com.egoistian.flowops.procurement.application.ProcurementCommands.CreateDraftItem;
import com.egoistian.flowops.procurement.application.ProcurementCommands.DraftResult;
import com.egoistian.flowops.procurement.application.ProcurementCommands.SubmitCommand;
import com.egoistian.flowops.procurement.application.ProcurementCommands.SubmitResult;
import com.egoistian.flowops.procurement.application.SubmitProcurementRequest;
import com.egoistian.flowops.procurement.api.ProcurementRequests.ApproveBody;
import com.egoistian.flowops.procurement.api.ProcurementRequests.CreateProcurementRequest;
import com.egoistian.flowops.procurement.api.ProcurementRequests.SubmitBody;
import com.egoistian.flowops.procurement.query.ProcurementDetailView;
import com.egoistian.flowops.procurement.query.ProcurementQueryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@RestController
@RequestMapping("/api/procurement/requests")
public class ProcurementController {
    private final CreateProcurementDraft createDraft;
    private final ProcurementQueryService queryService;
    private final SubmitProcurementRequest submitRequest;
    private final ApproveProcurementRequest approveRequest;

    public ProcurementController(
            CreateProcurementDraft createDraft,
            ProcurementQueryService queryService,
            SubmitProcurementRequest submitRequest,
            ApproveProcurementRequest approveRequest) {
        this.createDraft = createDraft;
        this.queryService = queryService;
        this.submitRequest = submitRequest;
        this.approveRequest = approveRequest;
    }

    @PostMapping
    ResponseEntity<DraftResult> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateProcurementRequest request) {
        DraftResult result = createDraft.handle(new CreateDraftCommand(
                user.organizationId(),
                user.userId(),
                request.title(),
                request.purpose(),
                request.budgetCode(),
                request.items().stream()
                        .map(item -> new CreateDraftItem(
                                item.name(), item.quantity(), item.unitPriceKrw()))
                        .toList()));
        URI location = URI.create("/api/procurement/requests/" + result.requestId());
        return ResponseEntity.created(location).body(result);
    }

    @GetMapping("/{requestId}")
    ProcurementDetailView get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID requestId) {
        return queryService.get(user.organizationId(), requestId);
    }

    @PostMapping("/{requestId}/submit")
    SubmitResult submit(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID requestId,
            @Valid @RequestBody SubmitBody request) {
        return submitRequest.handle(new SubmitCommand(
                user.organizationId(),
                requestId,
                user.userId(),
                request.idempotencyKey(),
                canonicalSubmitHash(user.organizationId(), requestId)));
    }

    @PostMapping("/{requestId}/approve")
    ApprovalResult approve(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID requestId,
            @Valid @RequestBody ApproveBody request) {
        return approveRequest.handle(new ApproveCommand(
                user.organizationId(),
                requestId,
                user.userId(),
                user.roles(),
                request.expectedVersion()));
    }

    private String canonicalSubmitHash(UUID organizationId, UUID requestId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((
                    "PROCUREMENT_SUBMIT:" + organizationId + ":" + requestId)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
