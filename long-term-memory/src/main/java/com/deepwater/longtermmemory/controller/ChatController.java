package com.deepwater.longtermmemory.controller;

import com.deepwater.longtermmemory.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ChatController {
    private final ChatClient agent;

    @GetMapping("chat")
    public String chat(@RequestParam String msg,
                       @RequestParam String userId,
                       @RequestParam(defaultValue = "1") String conversationId) {
        try{
            UserContext.set(userId);
            String content = agent.prompt()
                    .advisors(e -> e.params(Map.of(ChatMemory.CONVERSATION_ID, conversationId)))
                    .user(msg)
                    .call()
                    .content();
            return content;
        }finally {
            UserContext.clear();
        }
    }
}
