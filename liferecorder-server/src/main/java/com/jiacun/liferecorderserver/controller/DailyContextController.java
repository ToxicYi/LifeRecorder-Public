package com.jiacun.liferecorderserver.controller;

import com.jiacun.liferecorderserver.dto.AgentGeneratedFileContentResponse;
import com.jiacun.liferecorderserver.dto.AgentGeneratedFilesResponse;
import com.jiacun.liferecorderserver.dto.DailyContextRequest;
import com.jiacun.liferecorderserver.dto.DailyContextResponse;
import com.jiacun.liferecorderserver.service.AgentGeneratedFileService;
import com.jiacun.liferecorderserver.service.DailyContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 日常上下文数据控制器
 * 
 * 主要职责：
 * 1. 接收 Android App 上传的日常上下文数据（位置、天气、设备状态等）
 * 2. 提供 Agent 生成文件的查询接口
 * 
 * 工作流程：
 * 1. App 定期上传 daily_context → /daily-context
 * 2. 后端保存到 D:/LifeRecorder/today/context/daily_context.json
 * 3. 更新 today/index.json 和 today/changes.json
 * 4. App 可以查询 Agent 生成的文件 → /agent-generated-files
 * 5. App 可以读取具体文件内容 → /agent-generated-file
 */
@RestController
@RequestMapping("/life")
public class DailyContextController {

    /**
     * 日常上下文服务，处理位置、天气、设备等数据的保存
     */
    @Autowired
    private DailyContextService dailyContextService;

    /**
     * Agent 生成文件服务，提供文件列表和内容的查询
     */
    @Autowired
    private AgentGeneratedFileService agentGeneratedFileService;

    /**
     * 上传日常上下文数据
     * 
     * Android App 定期调用此接口，上传当天的现实世界数据：
     * - 位置信息（城市、国家）
     * - 天气数据（自动从 Open-Meteo API 获取）
     * - 设备状态（电量、充电状态、网络类型）
     * - 健康数据、应用使用情况等
     * 
     * 后端会自动：
     * 1. 保存 daily_context.json 到 D:/LifeRecorder/today/context/
     * 2. 更新 today/index.json
     * 3. 追加 today/changes.json
     * 
     * @param request 日常上下文请求，包含日期、位置、设备等信息
     * @return 处理结果，包含成功状态和消息
     */
    @PostMapping(value = "/daily-context", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<DailyContextResponse> uploadDailyContext(@RequestBody DailyContextRequest request) {
        try {
            // 验证必要字段
            if (request.getDate() == null || request.getDate().trim().isEmpty()) {
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DailyContextResponse(false, "日期不能为空"));
            }

            // 处理请求
            String result = dailyContextService.processDailyContext(request);

            // 判断是否成功
            boolean success = !result.contains("失败") && !result.contains("错误");

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DailyContextResponse(success, result));

        } catch (Exception e) {
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DailyContextResponse(false, "处理异常：" + e.getMessage()));
        }
    }

    /**
     * 获取 Agent 生成的文件列表
     * 
     * 返回 today/index.json 中所有 type=ai_generated 的文件。
     * 这些文件是 OpenClaw Agent 通过 write_agent_result.py 脚本生成的。
     * 
     * @return 文件列表响应，包含 schemaVersion 和 files 数组
     */
    @GetMapping(value = "/agent-generated-files", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<AgentGeneratedFilesResponse> getAgentGeneratedFiles() {
        try {
            AgentGeneratedFilesResponse response = agentGeneratedFileService.getAgentGeneratedFiles();
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
        } catch (Exception e) {
            System.err.println("获取 Agent 生成文件列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AgentGeneratedFilesResponse());
        }
    }

    /**
     * 读取 Agent 生成的指定文件内容
     * 
     * 根据 relativePath 读取具体的文件内容。
     * 安全检查：只能访问 D:/LifeRecorder/today/ai_generated/ 下的文件。
     * 
     * @param relativePath 文件相对路径，例如 "ai_generated/markdown/xxx.md"
     * @return 文件内容响应，或错误信息（404/403/400）
     */
    @GetMapping(value = "/agent-generated-file", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<?> getAgentGeneratedFile(@RequestParam String relativePath) {
        try {
            AgentGeneratedFileContentResponse response = agentGeneratedFileService.getAgentGeneratedFileContent(relativePath);
            
            if (response == null) {
                // 文件不存在，返回 404
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"文件不存在: " + relativePath + "\"}");
            }
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
                
        } catch (SecurityException e) {
            // 安全异常，返回 403
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (IllegalArgumentException e) {
            // 参数错误，返回 400
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            System.err.println("读取 Agent 生成文件失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\": \"读取文件失败: " + e.getMessage() + "\"}");
        }
    }
}
