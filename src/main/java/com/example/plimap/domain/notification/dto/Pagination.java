package com.example.plimap.domain.notification.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record Pagination<T>(
        List<T> data,
        String nextCursor,
        Boolean hasNext,
        Integer pageSize
) {}
