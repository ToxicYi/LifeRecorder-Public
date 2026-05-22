# 测试 Agent 生成文件读取接口

$baseUrl = "http://localhost:8080"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "测试 Agent 生成文件读取接口" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 测试 1: 获取 Agent 生成文件列表
Write-Host "【测试 1】GET /life/agent-generated-files" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/life/agent-generated-files" -Method Get
    Write-Host "状态: 成功" -ForegroundColor Green
    Write-Host "返回数据:" -ForegroundColor Gray
    $response | ConvertTo-Json -Depth 10 | Write-Host
} catch {
    Write-Host "状态: 失败" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}
Write-Host ""

# 测试 2: 读取指定文件内容（需要先确认有文件）
Write-Host "【测试 2】GET /life/agent-generated-file?relativePath=ai_generated/markdown/test.md" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/life/agent-generated-file?relativePath=ai_generated/markdown/test.md" -Method Get
    Write-Host "状态: 成功" -ForegroundColor Green
    Write-Host "返回数据:" -ForegroundColor Gray
    $response | ConvertTo-Json -Depth 10 | Write-Host
} catch {
    Write-Host "状态: 文件不存在或错误" -ForegroundColor Yellow
    Write-Host $_.Exception.Message -ForegroundColor Yellow
}
Write-Host ""

# 测试 3: 路径穿越攻击测试（应该被拒绝）
Write-Host "【测试 3】路径穿越攻击测试 - ../config/application.yaml" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/life/agent-generated-file?relativePath=../config/application.yaml" -Method Get
    Write-Host "状态: 失败（应该被拒绝但成功了）" -ForegroundColor Red
} catch {
    Write-Host "状态: 成功拦截" -ForegroundColor Green
    Write-Host "错误信息: $($_.Exception.Message)" -ForegroundColor Gray
}
Write-Host ""

# 测试 4: 访问非 ai_generated 目录（应该被拒绝）
Write-Host "【测试 4】访问非 ai_generated 目录 - context/daily_context.json" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/life/agent-generated-file?relativePath=context/daily_context.json" -Method Get
    Write-Host "状态: 失败（应该被拒绝但成功了）" -ForegroundColor Red
} catch {
    Write-Host "状态: 成功拦截" -ForegroundColor Green
    Write-Host "错误信息: $($_.Exception.Message)" -ForegroundColor Gray
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "测试完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
