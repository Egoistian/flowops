package com.egoistian.flowops.procurement.application;

public class RequestVersionConflict extends RuntimeException {
    private final String code;

    public RequestVersionConflict(String code) {
        super(code);
        this.code = code;
    }

    public RequestVersionConflict(String code, Throwable cause) {
        super(code, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
