# 测试手机文件索引上传接口

## 测试步骤

### 1. 启动 Spring Boot 应用
```bash
mvn spring-boot:run
```

### 2. 测试首次上传文件索引

使用 PowerShell 或 curl 发送请求：

```powershell
# PowerShell 示例
$body = @{
    schemaVersion = 1
    deviceId = "android_main"
    updatedTime = 1770000000000
    files = @(
        @{
            fileId = "phone_file_001"
            name = "课程设计.pdf"
            relativePhonePath = "Documents/课程设计.pdf"
            mimeType = "application/pdf"
            size = 238888
            lastModified = 1770000000000
            contentHash = "abc123"
            availableLocally = $false
            cachedPath = $null
        },
        @{
            fileId = "phone_file_002"
            name = "笔记.txt"
            relativePhonePath = "Notes/笔记.txt"
            mimeType = "text/plain"
            size = 1024
            lastModified = 1770000000000
            contentHash = "def456"
            availableLocally = $true
            cachedPath = "/storage/emulated/0/Notes/笔记.txt"
        }
    )
} | ConvertTo-Json -Depth 10

Invoke-RestMethod -Uri "http://localhost:8080/phone-sync/upload-file-index" -Method POST -Body $body -ContentType "application/json"
```

或使用 curl：

```bash
curl -X POST http://localhost:8080/phone-sync/upload-file-index \
  -H "Content-Type: application/json" \
  -d '{
    "schemaVersion": 1,
    "deviceId": "android_main",
    "updatedTime": 1770000000000,
    "files": [
      {
        "fileId": "phone_file_001",
        "name": "课程设计.pdf",
        "relativePhonePath": "Documents/课程设计.pdf",
        "mimeType": "application/pdf",
        "size": 238888,
        "lastModified": 1770000000000,
        "contentHash": "abc123",
        "availableLocally": false,
        "cachedPath": null
      },
      {
        "fileId": "phone_file_002",
        "name": "笔记.txt",
        "relativePhonePath": "Notes/笔记.txt",
        "mimeType": "text/plain",
        "size": 1024,
        "lastModified": 1770000000000,
        "contentHash": "def456",
        "availableLocally": true,
        "cachedPath": "/storage/emulated/0/Notes/笔记.txt"
      }
    ]
  }'
```

### 3. 验证结果

#### 检查生成的文件：

1. **file_index.json** 应该位于 `D:/LifeRecorder/phone_sync/current/file_index.json`
   ```bash
   cat D:/LifeRecorder/phone_sync/current/file_index.json
   ```

2. **today/index.json** 应该包含 phone_file_index 项
   ```bash
   cat D:/LifeRecorder/today/index.json
   ```

3. **today/changes.json** 应该记录更新事件
   ```bash
   cat D:/LifeRecorder/today/changes.json
   ```

### 4. 测试重复上传（同一个 fileId）

再次发送相同的请求，验证：
- file_index.json 中不会有重复的 fileId
- 只有 updatedTime 会变化

### 5. 测试文件更新

修改其中一个文件的 contentHash 或 lastModified，再次上传：

```bash
curl -X POST http://localhost:8080/phone-sync/upload-file-index \
  -H "Content-Type: application/json" \
  -d '{
    "schemaVersion": 1,
    "deviceId": "android_main",
    "updatedTime": 1770000001000,
    "files": [
      {
        "fileId": "phone_file_001",
        "name": "课程设计.pdf",
        "relativePhonePath": "Documents/课程设计.pdf",
        "mimeType": "application/pdf",
        "size": 239000,
        "lastModified": 1770000001000,
        "contentHash": "xyz789",
        "availableLocally": false,
        "cachedPath": null
      }
    ]
  }'
```

验证：
- file_index.json 中 phone_file_001 的信息已更新
- changes.json 中新增了 phone_file_index_updated 事件
- index.json 中 phone_file_index 项的 updatedTime 已更新

## 预期响应格式

成功响应示例：
```json
{
  "success": true,
  "message": "文件索引已保存",
  "totalFiles": 2,
  "updatedFiles": 0,
  "newFiles": 2
}
```

失败响应示例：
```json
{
  "success": false,
  "message": "文件列表不能为空"
}
```
