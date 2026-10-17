package com.codevo.businessos.billing.invoice;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem,UUID> { List<InvoiceItem> findByInvoiceId(UUID id); }
