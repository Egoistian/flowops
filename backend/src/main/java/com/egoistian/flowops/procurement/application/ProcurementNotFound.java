package com.egoistian.flowops.procurement.application;

public class ProcurementNotFound extends RuntimeException {
    public ProcurementNotFound() {
        super("PROCUREMENT_NOT_FOUND");
    }
}
