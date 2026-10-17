package com.codevo.businessos.billing.invoice;
import java.util.UUID;
import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="invoice_items") @Getter @Setter
public class InvoiceItem {
    @Id private UUID id=UUID.randomUUID();
    private UUID invoiceId;
    private UUID projectServiceId;
    @Column(length=1000) private String description;
    @Column(precision=12,scale=3) private BigDecimal quantity;
    @Column(precision=19,scale=3) private BigDecimal unitPrice;
    @Column(precision=19,scale=3) private BigDecimal lineTotal;
    private boolean commercialBilling=true;
}
