package com.example.authservice.application.context;

import java.util.List;

/**
 * 表达当前发起用例的操作者上下文。
 * Represents the current operator of the ongoing application use case.
 */
public record CurrentOperator(
        Long id,
        String username,
        String sessionId,
        String token,
        List<String> roles,
        List<String> permissions
) {
}
