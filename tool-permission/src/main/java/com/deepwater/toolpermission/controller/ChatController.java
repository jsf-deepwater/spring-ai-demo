package com.deepwater.toolpermission.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
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
                       @RequestParam(defaultValue = "USER") String role) {
        return agent.prompt().toolContext(Map.of("role", role)).user(msg).call().content();
    }
}
