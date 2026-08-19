package com.egoistian.flowops.procurement.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class ProcurementRequests {
    private ProcurementRequests() {
    }

    public record CreateProcurementRequest(
            @NotBlank String title,
            @NotBlank String purpose,
            @NotBlank String budgetCode,
            @NotNull @Size(min = 1, max = 50) List<@Valid Item> items) {
        public record Item(
                @NotBlank String name,
                @Min(1) int quantity,
                @Min(0) long unitPriceKrw) {
        }
    }

    public record SubmitBody(
            @NotBlank @Size(max = 200) String idempotencyKey) {
    }

    public record ApproveBody(long expectedVersion) {
    }
}
