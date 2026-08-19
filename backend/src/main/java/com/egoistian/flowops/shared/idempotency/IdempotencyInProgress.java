package com.egoistian.flowops.shared.idempotency;

public class IdempotencyInProgress extends RuntimeException {
    public IdempotencyInProgress() {
        super("IDEMPOTENCY_REQUEST_IN_PROGRESS");
    }
}
