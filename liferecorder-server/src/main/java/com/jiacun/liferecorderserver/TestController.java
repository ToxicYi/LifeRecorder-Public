package com.jiacun.liferecorderserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 新增：测试服务器是否能访问
@RestController
public class TestController {

    // 新增：访问 /ping 时返回一段文字
    @GetMapping("/ping")
    public String ping() {
        return "LifeRecorder server ok";
    }
}