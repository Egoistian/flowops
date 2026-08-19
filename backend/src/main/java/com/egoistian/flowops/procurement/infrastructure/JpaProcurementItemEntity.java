package com.egoistian.flowops.procurement.infrastructure;

import com.egoistian.flowops.procurement.domain.ProcurementItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "procurement_items")
public class JpaProcurementItemEntity {
    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private JpaProcurementRequestEntity request;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected JpaProcurementItemEntity() {
    }

    static JpaProcurementItemEntity from(
            UUID organizationId,
            JpaProcurementRequestEntity request,
            ProcurementItem item) {
        JpaProcurementItemEntity entity = new JpaProcurementItemEntity();
        entity.id = UUID.randomUUID();
        entity.organizationId = organizationId;
        entity.request = request;
        entity.name = item.name();
        entity.quantity = item.quantity();
        entity.unitPrice = BigDecimal.valueOf(item.unitPrice().amount());
        entity.subtotal = BigDecimal.valueOf(item.subtotal().amount());
        entity.createdAt = Instant.now();
        return entity;
    }

    ProcurementItem toDomain() {
        return new ProcurementItem(name, quantity,
                com.egoistian.flowops.procurement.domain.Money.krw(unitPrice.longValueExact()));
    }
}
