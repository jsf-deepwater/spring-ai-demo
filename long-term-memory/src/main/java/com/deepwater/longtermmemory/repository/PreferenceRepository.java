package com.deepwater.longtermmemory.repository;

import com.deepwater.longtermmemory.entity.Preference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Repository
public class PreferenceRepository {

    /** 直接抄 Kimi 的规格：最多 50 条，每条最多 500 字符 */
    public static final int MAX_ITEMS = 50;
    public static final int MAX_LENGTH = 500;

    private final JdbcTemplate jdbc;
    private final SimpleJdbcInsert insertPreference;

    public PreferenceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.insertPreference = new SimpleJdbcInsert(jdbc)
                .withTableName("user_preference")
                .usingGeneratedKeyColumns("id");
    }

    /** 读：注入 system prompt 时调用 */
    public List<Preference> findByUserId(String userId) {
        return jdbc.query("""
                        SELECT id, user_id, category, content, updated_at
                        FROM user_preference
                        WHERE user_id = ?
                        ORDER BY category, id
                        """,
                (rs, row) -> new Preference(
                        rs.getLong("id"),
                        rs.getString("user_id"),
                        rs.getString("category"),
                        rs.getString("content"),
                        rs.getTimestamp("updated_at").toInstant()
                ),
                userId);
    }

    public int countByUserId(String userId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_preference WHERE user_id = ?", Integer.class, userId);
        return n == null ? 0 : n;
    }

    /** 新增：超限直接抛，让工具层返回友好提示给模型 */
    public long add(String userId, String category, String content) {
        validateContent(content);
        if (countByUserId(userId) >= MAX_ITEMS) {
            throw new IllegalStateException("记忆条数已达上限 " + MAX_ITEMS + " 条，请先删除部分记忆再新增");
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Map<String, Object> params = Map.of(
                "user_id", userId,
                "category", category,
                "content", content,
                "created_at", now,
                "updated_at", now);
        return insertPreference.executeAndReturnKey(params).longValue();
    }

    /** 修改：只改内容，不改分类 */
    public int updateContent(Long id, String userId, String content) {
        validateContent(content);
        return jdbc.update(
                "UPDATE user_preference SET content = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ?",
                content, id, userId);
    }

    /** 删除：必须带 user_id，防止 A 用户删掉 B 用户的记忆 */
    public int delete(Long id, String userId) {
        return jdbc.update("DELETE FROM user_preference WHERE id = ? AND user_id = ?", id, userId);
    }

    /** 清空某个用户某一类记忆 */
    public int deleteByCategory(String userId, String category) {
        return jdbc.update("DELETE FROM user_preference WHERE user_id = ? AND category = ?", userId, category);
    }

    /** 清空某个用户的全部记忆 */
    public int deleteByUserId(String userId) {
        return jdbc.update("DELETE FROM user_preference WHERE user_id = ?", userId);
    }

    /** 模糊查找：模型说"把那条 Python 的删了"时，先靠这个定位 id */
    public List<Preference> searchByKeyword(String userId, String keyword) {
        return jdbc.query("""
                        SELECT id, user_id, category, content, updated_at
                        FROM user_preference
                        WHERE user_id = ? AND content LIKE ?
                        ORDER BY id
                        """,
                (rs, row) -> new Preference(
                        rs.getLong("id"),
                        rs.getString("user_id"),
                        rs.getString("category"),
                        rs.getString("content"),
                        rs.getTimestamp("updated_at").toInstant()
                ),
                userId, "%" + keyword + "%");
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("记忆内容不能为空");
        }
        if (content.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("单条记忆不能超过 " + MAX_LENGTH + " 字符，当前 " + content.length());
        }
    }
}
