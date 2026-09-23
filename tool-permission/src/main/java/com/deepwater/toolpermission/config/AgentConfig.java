package com.deepwater.toolpermission.config;

import com.deepwater.toolpermission.tool.PermissionTool;
import com.deepwater.toolpermission.tool.PermissionToolCallWrapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class AgentConfig {

    @Bean
    public ChatClient agent(OllamaChatModel ollamaChatModel){

        ToolCallback[] toolCallbacks = ToolCallbacks.from(new PermissionTool());
        ToolCallback[] toolCallBacksWrapper = Arrays.stream(toolCallbacks)
                .map(e -> new PermissionToolCallWrapper(e, PermissionTool.class)).
                toArray(ToolCallback[]::new);

        return ChatClient.builder(ollamaChatModel)
                .defaultTools(toolCallBacksWrapper)
                .build();
    }
}
