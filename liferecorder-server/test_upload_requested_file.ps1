# 测试上传被请求的文件功能

## 前置条件

1. 确保已经创建了 fetch request（参考 test_pending_requests.ps1）
2. 启动 Spring Boot 应用

```bash
mvn spring-boot:run
```

## 测试步骤

### 1. 先创建一个 fetch request

```bash
curl -X POST http://localhost:8080/phone-sync/create-fetch-request \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "android_main",
    "fileId": "phone_file_001",
    "sourceIndex": "file_index.json",
    "reason": "Agent 需要读取这个文件来完成用户请求"
  }'
```

记录返回的 `request.id`，例如：`request_1770000000000`

### 2. 准备测试文件

在 D:/ 下创建一个测试文件，例如：`D:/test_document.pdf`

或者使用 PowerShell 创建一个测试文件：

```powershell
# 创建一个小的测试文本文件
"这是测试文件内容" | Out-File -FilePath "D:/test_file.txt" -Encoding UTF8
```

### 3. 上传文件

#### 使用 curl（推荐）：

```bash
curl -X POST http://localhost:8080/phone-sync/upload-requested-file \
  -F "requestId=request_1770000000000" \
  -F "deviceId=android_main" \
  -F "fileId=phone_file_001" \
  -F "file=@D:/test_file.txt"
```

#### 使用 PowerShell：

```powershell
# PowerShell 示例
$url = "http://localhost:8080/phone-sync/upload-requested-file"

# 创建 multipart 表单数据
$boundary = [System.Guid]::NewGuid().ToString()
$contentType = "multipart/form-data; boundary=$boundary"

$body = @"
--$boundary
Content-Disposition: form-data; name="requestId"

request_1770000000000
--$boundary
Content-Disposition: form-data; name="deviceId"

android_main
--$boundary
Content-Disposition: form-data; name="fileId"

phone_file_001
--$boundary
Content-Disposition: form-data; name="file"; filename="test_file.txt"
Content-Type: text/plain

$(Get-Content "D:/test_file.txt" -Raw)
--$boundary--
"@

Invoke-RestMethod -Uri $url -Method POST -Body $body -ContentType $contentType
```

**预期响应：**
```json
{
  "success": true,
  "requestId": "request_1770000000000",
  "fileId": "phone_file_001",
  "cachedPath": "phone_sync/current/cache/files/phone_file_001/test_file.txt",
  "message": "文件已上传并缓存"
}
```

### 4. 验证文件是否保存

检查文件是否存在：

```bash
ls D:/LifeRecorder/phone_sync/current/cache/files/phone_file_001/
```

应该能看到 `test_file.txt` 文件。

### 5. 验证 pending_requests.json 更新

```bash
cat D:/LifeRecorder/phone_sync/current/pending_requests.json
```

对应的 request 应该变成：
```json
{
  "id": "request_1770000000000",
  "type": "fetch_phone_file",
  "status": "completed",
  "deviceId": "android_main",
  "fileId": "phone_file_001",
  "resultCachedPath": "phone_sync/current/cache/files/phone_file_001/test_file.txt",
  "updatedTime": 1770000000000,
  "error": null
}
```

注意：`status` 从 `pending` 变成了 `completed`。

### 6. 验证 file_index.json 更新

```bash
cat D:/LifeRecorder/phone_sync/current/file_index.json
```

找到 `phone_file_001` 对应的项，应该看到：
```json
{
  "fileId": "phone_file_001",
  "name": "...",
  "availableLocally": true,
  "cachedPath": "phone_sync/current/cache/files/phone_file_001/test_file.txt",
  ...
}
```

注意：
- `availableLocally` 变成了 `true`
- `cachedPath` 有值了

### 7. 验证 today/changes.json

```bash
cat D:/LifeRecorder/today/changes.json
```

应该包含类型为 `phone_file_cached` 的事件：
```json
{
  "type": "phone_file_cached",
  "targetId": "phone_file_001",
  "targetPath": "phone_sync/current/cache/files/phone_file_001/test_file.txt",
  "source": "android_app",
  "description": "手机端按需上传了指定文件: phone_file_001",
  "createdTime": 1770000000000
}
```

### 8. 测试重复上传（latest_only）

再次上传同一个 fileId 的文件：

```bash
curl -X POST http://localhost:8080/phone-sync/upload-requested-file \
  -F "requestId=request_1770000000000" \
  -F "deviceId=android_main" \
  -F "fileId=phone_file_001" \
  -F "file=@D:/test_file.txt"
```

验证：
- 不会生成多个版本文件（v1/v2/v3）
- 同目录下同名文件被覆盖

### 9. 测试错误情况

#### 测试无效的 requestId：

```bash
curl -X POST http://localhost:8080/phone-sync/upload-requested-file \
  -F "requestId=invalid_request_id" \
  -F "deviceId=android_main" \
  -F "fileId=phone_file_001" \
  -F "file=@D:/test_file.txt"
```

**预期响应：**
```json
{
  "success": false,
  "message": "未找到匹配的待处理请求"
}
```

#### 测试已完成的状态：

如果 request 已经是 `completed` 状态，再次上传应该失败。

### 10. 测试大文件限制（可选）

创建一个超过 50MB 的文件进行测试，应该返回错误：

```json
{
  "success": false,
  "message": "文件大小超过限制（最大50MB）"
}
```

## 验收标准

✅ 上传后 cache/files/{fileId}/ 下出现真实文件  
✅ pending_requests.json 对应 request 变成 completed  
✅ file_index.json 对应 fileId 的 availableLocally=true  
✅ file_index.json 对应 fileId 的 cachedPath 有值  
✅ today/changes.json 出现 phone_file_cached  
✅ 重复上传不会生成多个版本文件  
✅ 无效 requestId 返回清晰错误  
✅ 文件大小限制生效  
