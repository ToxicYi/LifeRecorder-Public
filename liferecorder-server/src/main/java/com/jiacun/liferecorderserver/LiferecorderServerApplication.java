package com.jiacun.liferecorderserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LifeRecorder 服务器应用程序主入口类
 * 
 * LifeRecorder 是一个个人生活记录系统，由三部分组成：
 * 1. Android App - 负责数据采集和文件上传
 * 2. Spring Boot 后端 - 负责数据管理、工作区维护、AI 集成
 * 3. OpenClaw Agent - 负责智能分析和文件生成
 * 
 * 本应用提供 RESTful API 接口，支持：
 * - 日常上下文数据上传
 * - 手机文件同步（phone_sync）
 * - AI 聊天和 Agent 调用
 * - 工作区文件管理
 */
@SpringBootApplication
public class LiferecorderServerApplication {

    /**
     * 应用程序启动入口
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(LiferecorderServerApplication.class, args);
    }

}
