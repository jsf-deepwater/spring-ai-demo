package com.deepwater.longtermmemory.service;

import com.deepwater.longtermmemory.entity.Preference;
import com.deepwater.longtermmemory.repository.PreferenceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PreferenceService {

    private final PreferenceRepository repository;

    public PreferenceService(PreferenceRepository repository) {
        this.repository = repository;
    }

    /** 拼成一小段文本，交给 Advisor 追加到 system prompt */
    public String renderAsPromptSection(String userId) {
        List<Preference> prefs = repository.findByUserId(userId);
        if (prefs.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("\n\n【用户长期记忆】以下是该用户明确要求记住的内容：\n");
        prefs.forEach(p -> sb.append("- [").append(p.category()).append("] ").append(p.content()).append("\n"));
        sb.append("以上记忆跨会话长期有效，除非用户明确要求修改或删除。");
        return sb.toString();
    }

    public long add(String userId, String category, String preference){
        return repository.add(userId, category, preference);
    }

    public List<Preference> findByUserId(String userId) {
        return repository.findByUserId(userId);
    }
}
