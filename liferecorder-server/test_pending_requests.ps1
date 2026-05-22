# 测试 Pending Requests 功能

## 测试步骤

### 1. 启动 Spring Boot 应用
```bash
mvn spring-boot:run
```

### 2. 测试 GET /phone-sync/pending-requests（首次访问）

```powershell
# PowerShell
Invoke-RestMethod -Uri "http://localhost:8080/phone-sync/pending-requests" -Method GET
```

或使用 curl：

```bash
curl http://localhost:8080/phone-sync/pending-requests
```

**预期响应：**
```json
{
  "schemaVersion": 1,
  "updatedTime": 1770000000000,
  "requests": []
}
```

### 3. 测试 POST /phone-sync/create-fetch-request

```powershell
# PowerShell
$body = @{
    deviceId = "android_main"
    fileId = "phone_file_001"
    sourceIndex = "file_index.json"
    reason = "Agent 需要读取这个文件来完成用户请求"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/phone-sync/create-fetch-request" -Method POST -Body $body -ContentType "application/json"
```

或使用 curl：

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

**预期响应：**
```json
{
  "success": true,
  "message": "请求已创建",
  "request": {
    "id": "request_1770000000000",
    "type": "fetch_phone_file",
    "status": "pending",
    "deviceId": "android_main",
    "fileId": "phone_file_001",
    "sourceIndex": "file_index.json",
    "reason": "Agent 需要读取这个文件来完成用户请求",
    "requestedBy": "spring_boot",
    "createdTime": 1770000000000,
    "updatedTime": 1770000000000,
    "resultCachedPath": null,
    "error": null
  },
  "isExisting": false
}
```

### 4. 再次 GET /phone-sync/pending-requests

```bash
curl http://localhost:8080/phone-sync/pending-requests
```

**预期响应：**
```json
{
  "schemaVersion": 1,
  "updatedTime": 1770000000000,
  "requests": [
    {
      "id": "request_1770000000000",
      "type": "fetch_phone_file",
      "status": "pending",
      "deviceId": "android_main",
      "fileId": "phone_file_001",
      "sourceIndex": "file_index.json",
      "reason": "Agent 需要读取这个文件来完成用户请求",
      "requestedBy": "spring_boot",
      "createdTime": 1770000000000,
      "updatedTime": 1770000000000,
      "resultCachedPath": null,
      "error": null
    }
  ]
}
```

### 5. 测试重复创建同一个请求（去重机制）

再次发送相同的 POST 请求：

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

**预期响应：**
```json
{
  "success": true,
  "message": "请求已存在",
  "request": {
    "id": "request_1770000000000",
    "type": "fetch_phone_file",
    "status": "pending",
    "deviceId": "android_main",
    "fileId": "phone_file_001",
    ...
  },
  "isExisting": true
}
```

注意：`isExisting` 为 `true`，表示返回的是旧请求。

### 6. 验证文件生成

#### 检查 pending_requests.json：
```bash
cat D:/LifeRecorder/phone_sync/current/pending_requests.json
```

应该包含创建的 request 记录。

#### 检查 today/changes.json：
```bash
cat D:/LifeRecorder/today/changes.json
```

应该包含类型为 `phone_file_fetch_requested` 的事件记录。

### 7. 测试创建另一个不同的请求

```bash
curl -X POST http://localhost:8080/phone-sync/create-fetch-request \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "android_main",
    "fileId": "phone_file_002",
    "sourceIndex": "file_index.json",
    "reason": "需要另一个文件"
  }'
```

然后再次 GET /phone-sync/pending-requests，应该看到两个 pending requests。

## 验收标准

✅ GET /phone-sync/pending-requests 不再 404  
✅ 第一次 GET 返回空 requests  
✅ POST /phone-sync/create-fetch-request 能创建 request  
✅ 再次 GET 能看到 pending request  
✅ 重复 POST 同一个 fileId 不会重复创建多条 pending request  
✅ pending_requests.json 正确保存  
✅ today/changes.json 记录了 phone_file_fetch_requested 事件  
