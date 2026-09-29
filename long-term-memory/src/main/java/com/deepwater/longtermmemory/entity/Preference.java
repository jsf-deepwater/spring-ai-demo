package com.deepwater.longtermmemory.entity;

import java.time.Instant;

public record Preference(
        Long id,
        String userId,
        String category,
        String content,
        Instant updatedAt
) {}
