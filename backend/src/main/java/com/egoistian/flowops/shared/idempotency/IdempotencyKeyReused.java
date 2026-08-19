package com.egoistian.flowops.shared.idempotency;

public class IdempotencyKeyReused extends RuntimeException {
    private final String code;

    public IdempotencyKeyReused() {
        super("IDEMPOTENCY_KEY_REUSED");
        this.code = "IDEMPOTENCY_KEY_REUSED";
    }

    public String code() {
        return code;
    }
}
