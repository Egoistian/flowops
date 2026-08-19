package com.egoistian.flowops.procurement.domain;

public class DomainRuleViolation extends RuntimeException {
    private final String code;

    public DomainRuleViolation(String code) {
        super(code);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
