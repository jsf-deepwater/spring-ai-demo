package com.deepwater.longtermmemory.tool;

import com.deepwater.longtermmemory.common.UserContext;
import com.deepwater.longtermmemory.entity.Preference;
import com.deepwater.longtermmemory.repository.PreferenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PreferenceTools {

    private final PreferenceRepository repository;

    public PreferenceTools(PreferenceRepository repository) {
        this.repository = repository;
    }

    @Tool(description = """
            新增一条用户长期记忆。仅在用户明确要求"记住/记一下/以后记得"时调用。
            不要保存临时状态、闲聊内容或一次性任务。
            category 取值：USER(身份与偏好)、FEEDBACK(对AI行为的纠正)、
            PROJECT(项目背景)、REFERENCE(外部资料)
            """)
    public String addPreference(
            @ToolParam(description = "记忆分类：USER/FEEDBACK/PROJECT/REFERENCE") String category,
            @ToolParam(description = "记忆内容，一句话，不超过500字符") String content) {
        try {
            repository.add(UserContext.get(), category, content);
            return "已记住：" + content;
        } catch (RuntimeException e) {
            log.error("新增失败", e);
            return "新增失败：" + e.getMessage();
        }
    }

    @Tool(description = """
            按关键词模糊搜索用户长期记忆，返回 id 和内容。
            修改或删除之前必须先调用它定位具体是哪一条。
            """)
    public String searchPreference(
            @ToolParam(description = "关键词") String keyword) {
        List<Preference> list = repository.searchByKeyword(UserContext.get(), keyword);
        if (list.isEmpty()) {
            return "没找到包含「" + keyword + "」的记忆";
        }
        return list.stream()
                .map(p -> p.id() + " | " + p.category() + " | " + p.content())
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "修改已有记忆。必须先用 searchPreference 定位 id，再用新内容整体替换。仅在用户明确要求更正时调用。")
    public String updatePreference(
            @ToolParam(description = "要修改的记忆ID") Long id,
            @ToolParam(description = "修改后的完整内容") String content) {
        return repository.updateContent(id, UserContext.get(), content) > 0
                ? "已更新为：" + content
                : "未找到该记忆，id=" + id;
    }

    @Tool(description = "删除一条记忆。必须先用 searchPreference 定位 id，一次只删一条。仅在用户明确要求忘掉时调用。")
    public String deletePreference(
            @ToolParam(description = "要删除的记忆ID") Long id) {
        return repository.delete(id, UserContext.get()) > 0
                ? "已删除该记忆"
                : "未找到该记忆，id=" + id;
    }
}
