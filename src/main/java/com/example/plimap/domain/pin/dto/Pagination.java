package com.example.plimap.domain.pin.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record Pagination <T>(
   List<T> data,
   String nextCursor,
   Boolean hasNext,
   Integer pageSize
) {}
