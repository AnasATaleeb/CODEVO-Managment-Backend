package com.codevo.businessos.common;

import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

@Getter @Setter
public class ListFilter {
    private String q = "";
    private String status;
    private UUID clientId;
    private UUID projectId;
    private UUID assignedUserId;
    private LocalDate from;
    private LocalDate to;
    private int page = 0;
    private int size = 20;
    private String sort = "createdAt";
    private String direction = "desc";

    public Pageable pageable(Set<String> allowedSorts) {
        if (page < 0 || size < 1 || size > 100 || q.length() > 120) throw DomainException.invalid("Pagination or search is out of range");
        if (!allowedSorts.contains(sort)) throw DomainException.invalid("Unsupported sort field");
        if (!Set.of("asc","desc").contains(direction)) throw DomainException.invalid("Invalid sort direction");
        if (from != null && to != null && from.isAfter(to)) throw DomainException.invalid("Start date must precede end date");
        return PageRequest.of(page,size,Sort.by(Sort.Direction.fromString(direction),sort).and(Sort.by("id")));
    }
    public <T> Specification<T> specification(String searchField, String statusField, Set<String> referenceFields, String dateField) {
        return (root,query,cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (!q.isBlank()) p.add(cb.like(root.get(searchField),escape(q)+"%",'='));
            if (status != null && !status.isBlank() && statusField != null) p.add(cb.equal(root.get(statusField), status));
            if (clientId != null && referenceFields.contains("clientId")) p.add(cb.equal(root.get("clientId"),clientId));
            if (projectId != null && referenceFields.contains("projectId")) p.add(cb.equal(root.get("projectId"),projectId));
            if (assignedUserId != null && referenceFields.contains("assignedUserId")) p.add(cb.equal(root.get("assignedUserId"),assignedUserId));
            if (dateField != null) {
                if (root.get(dateField).getJavaType().equals(java.time.Instant.class)) {
                    if(from!=null) p.add(cb.greaterThanOrEqualTo(root.get(dateField),from.atStartOfDay(java.time.ZoneOffset.UTC).toInstant()));
                    if(to!=null) p.add(cb.lessThan(root.get(dateField),to.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()));
                } else {
                    if (from != null) p.add(cb.greaterThanOrEqualTo(root.get(dateField),from));
                    if (to != null) p.add(cb.lessThanOrEqualTo(root.get(dateField),to));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
    public static String escape(String value) { return value.replace("=","==").replace("%","=%").replace("_","=_"); }
}
