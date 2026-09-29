package com.deepwater.longtermmemory.advisor;

import com.deepwater.longtermmemory.common.UserContext;
import com.deepwater.longtermmemory.service.PreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PreferenceAdvisor implements BaseAdvisor {
    private final PreferenceService preferenceService;
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String userId = UserContext.get();
        String preferencePrompt = preferenceService.renderAsPromptSection(userId);
        if (preferencePrompt==null){
            return chatClientRequest;
        }
        SystemMessage systemMessage = chatClientRequest.prompt().getSystemMessage();
        Prompt newPrompt = chatClientRequest.prompt().augmentSystemMessage(systemMessage.getText() + preferencePrompt);
        return chatClientRequest.mutate().prompt(newPrompt).build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
//        return Ordered.HIGHEST_PRECEDENCE + 100;
        return 0;
    }
}
