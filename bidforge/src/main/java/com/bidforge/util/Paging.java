package com.bidforge.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;


// sort fields go into the generated SQL's ORDER BY

public final class Paging {


    private static final int MAX_PAGE_SIZE = 50;

    private Paging() {
    }

    // sort is a parameter that takes the form "field" or "field,asc" or "field,desc" like "endTime,asc"
    // allowed parameter are the sort fields the endpoint permits
    public static Pageable of(int page, int size, String sort, Set<String> allowed) {
        // Most entities have createdAt
        // endpoints whose entity does not have it like auction results use the overloaded function directly
        return of(page, size, sort, allowed, "createdAt");
    }


    // variant with a custom default sort field
    public static Pageable of(int page, int size, String sort, Set<String> allowed, String defaultField) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);

        Sort order = Sort.by(Sort.Direction.DESC, defaultField);
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            if (allowed.contains(field)) {
                Sort.Direction direction =
                        (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc"))
                                ? Sort.Direction.ASC
                                : Sort.Direction.DESC;
                order = Sort.by(direction, field);
            }
        }
        return PageRequest.of(safePage, safeSize, order);
    }
}
