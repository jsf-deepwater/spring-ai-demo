package com.deepwater.toolpermission.tool;

import com.deepwater.toolpermission.annotation.AuthRoles;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;

@Slf4j
public class PermissionTool {
    @Tool(description = "查询设备信息")
    @AuthRoles(roles = {"ADMIN", "USER"})      // 管理员和普通用户都能查
    public String getDevice(String deviceId) {
        log.info("[工具执行] getDevice: {}", deviceId);
        return "设备 " + deviceId + "：温度 23.5℃，状态正常";
    }

    @Tool(description = "删除一个设备及其下所有点位")
    @AuthRoles(roles = {"ADMIN"})              // 只有管理员能删
    public String deleteDevice(String deviceId) {
        log.info("[工具执行] deleteDevice: {} —— 真的执行了删除", deviceId);
        return "设备 " + deviceId + " 已删除，其下点位及历史数据一并清除";
    }
}
