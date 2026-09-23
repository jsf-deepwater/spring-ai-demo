package com.deepwater.toolpermission.tool;

import com.deepwater.toolpermission.annotation.AuthRoles;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.support.ToolUtils;

import java.lang.reflect.Method;
import java.util.Arrays;

@Slf4j
public class PermissionToolCallWrapper implements ToolCallback {

    /**
     * 拦截文案。会作为工具结果回填给模型，所以：
     * 1. 说清是终态，堵死"换个工具再试"
     * 2. 明确禁止变换参数绕路
     * 3. 给用户可行动的方向
     * 4. 不泄漏权限边界（说"需要更高权限"，别说"ADMIN 可以"）
     */
    private static final String PERMISSION_DENIED_TIP = """
            PERMISSION_DENIED: 当前用户无权执行该操作。
            这不是临时故障，重试不会成功；也不要改用其他工具或变换参数再来一次。
            请直接告知用户：该操作需要更高权限，如有需要请联系管理员开通。
            """;

    private final ToolCallback delegate;
    private String[] roles;

    public PermissionToolCallWrapper(ToolCallback delegate, Class clazz) {
        this.delegate = delegate;

        String name = getToolDefinition().name();

        // 框架不告诉你这个 callback 背后是哪个方法，只能自己反查
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            String toolName = ToolUtils.getToolName(method);
            if (!name.equalsIgnoreCase(toolName)) {
                continue;
            }
            AuthRoles authRoles = method.getAnnotation(AuthRoles.class);
            if (authRoles != null) {
                roles = authRoles.roles();
            }
            break;// 找到方法就停，别往下扫了
        }

        // 构造期 fail-fast：反查失败 or 没加注解，都在这里炸掉
        // 绝不能静默降级成放行 —— 那是全文最阴的坑
        if (roles == null || roles.length == 0) {
            throw new IllegalStateException(
                    "工具 " + name + " 未配置 @AuthRoles，拒绝注册。" +
                            "请检查：1) 方法是否加了注解 2) 工具类是否有父类" +
                            "（getDeclaredMethods 不含继承方法）3) 是否有重载导致工具名对不上");
        }
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        String role = (toolContext == null) ? null
                : (String) toolContext.getContext().get("role");

        if (!hasPermission(role)) {
            // 不抛异常：无权限是预期内的业务结果，返回文案让模型转告用户。
            // 抛异常的话，Spring AI 默认也会把 message 转成文本发回模型，
            // 绕一圈回到原点，还额外污染监控告警。
            log.warn("[权限拦截] 工具={} role={}", getToolDefinition().name(), role);
            return PERMISSION_DENIED_TIP;
        }
        return delegate.call(toolInput, toolContext);
    }

    private boolean hasPermission(String role) {
        // 变化 ：fail-close，默认拒绝
        if (roles == null || roles.length == 0) {
            return false;
        }
        return role != null && Arrays.asList(roles).contains(role);
    }

}
