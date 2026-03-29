package com.example.authservice.application.context;

import com.example.authservice.domain.identity.model.result.CurrentIdentity;

import java.util.List;

/**
 * 表达当前发起用例的操作者上下文，隔离传输层身份对象。
 * Represents the current operator for an application use case and decouples transport-layer identity objects.
 */
public record CurrentOperator(
        Long id,
        String username,
        String sessionId,
        List<String> roles,
        List<String> permissions
) {

    /**
     * 把接口层注入的当前身份转换成应用层操作者上下文。
     * Converts the interface-layer identity into an application-layer operator context.
     */
    public static CurrentOperator from(CurrentIdentity currentIdentity) {
        return new CurrentOperator(
                currentIdentity.getId(),
                currentIdentity.getUsername(),
                currentIdentity.getSessionId(),
                currentIdentity.getRoles(),
                currentIdentity.getPermissions()
        );
    }
}
