package com.deepwater.longtermmemory.config;

import com.deepwater.longtermmemory.advisor.PreferenceAdvisor;
import com.deepwater.longtermmemory.tool.PreferenceTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    @Bean
    public ChatClient agent(OllamaChatModel ollamaChatModel,
                            PreferenceAdvisor preferenceAdvisor,
                            ChatMemory chatMemory,
                            PreferenceTools preferenceTools){
        return ChatClient.builder(ollamaChatModel)
//                .defaultSystem("你是一名英语老师")
                .defaultTools(preferenceTools)
                .defaultAdvisors(preferenceAdvisor,
                        new SimpleLoggerAdvisor(),
                        ToolCallingAdvisor.builder().build(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
