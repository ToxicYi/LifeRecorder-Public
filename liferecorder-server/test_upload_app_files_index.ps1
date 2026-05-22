# 测试 App 文件索引上传功能

## 测试步骤

### 1. 启动 Spring Boot 应用
```bash
mvn spring-boot:run
```

### 2. 测试首次上传 App 文件索引

使用 curl 发送请求：

```bash
curl -X POST http://localhost:8080/phone-sync/upload-app-files-index \
  -H "Content-Type: application/json" \
  -d '{
    "schemaVersion": 1,
    "deviceId": "android_main",
    "updatedTime": 1770000000000,
    "files": [
      {
        "fileId": "app_file_001",
        "name": "课程设计.pdf",
        "virtualPath": "LifeRecorder文件/课程设计.pdf",
        "source": "file_picker",
        "mimeType": "application/pdf",
        "size": 238888,
        "lastModified": 1770000000000,
        "contentHash": "hash_v1",
        "availableLocally": false,
        "cachedPath": null,
        "linkedPhoneFileId": "phone_file_001"
      },
      {
        "fileId": "app_file_002",
        "name": "笔记.txt",
        "virtualPath": "LifeRecorder文件/笔记.txt",
        "source": "in_app_editor",
        "mimeType": "text/plain",
        "size": 1024,
        "lastModified": 1770000000000,
        "contentHash": "hash_v2",
        "availableLocally": true,
        "cachedPath": "phone_sync/current/cache/files/app_file_002/笔记.txt",
        "linkedPhoneFileId": null
      }
    ]
  }'
```

**预期响应：**
```json
{
  "success": true,
  "message": "App 文件索引已保存",
  "totalFiles": 2,
  "updatedFiles": 0,
  "newFiles": 2
}
```

### 3. 验证生成的文件

#### 检查 app_files_index.json：

```bash
cat D:/LifeRecorder/phone_sync/current/app_files_index.json
```

应该包含上传的文件列表。

#### 检查 today/index.json：

```bash
cat D:/LifeRecorder/today/index.json
```

应该包含 `app_files_index` 项：
```json
{
  "id": "app_files_index",
  "type": "phone_file_index",
  "name": "app_files_index.json",
  "relativePath": "../phone_sync/current/app_files_index.json",
  "source": "android_app",
  ...
}
```

#### 检查 today/changes.json：

```bash
cat D:/LifeRecorder/today/changes.json
```

应该包含类型为 `app_file_index_updated` 的事件记录。

### 4. 测试重复上传（同一个 fileId）

再次发送相同的请求，验证：
- app_files_index.json 中不会有重复的 fileId
- totalFiles 仍然是 2
- updatedFiles 为 0（因为没有变化）

```bash
curl -X POST http://localhost:8080/phone-sync/upload-app-files-index \
  -H "Content-Type: application/json" \
  -d '{
    "schemaVersion": 1,
    "deviceId": "android_main",
    "updatedTime": 1770000001000,
    "files": [
      {
        "fileId": "app_file_001",
        "name": "课程设计.pdf",
        "virtualPath": "LifeRecorder文件/课程设计.pdf",
        "source": "file_picker",
        "mimeType": "application/pdf",
        "size": 238888,
        "lastModified": 1770000000000,
        "contentHash": "hash_v1",
        "availableLocally": false,
        "cachedPath": null,
        "linkedPhoneFileId": "phone_file_001"
      }
    ]
  }'
```

**预期响应：**
```json
{
  "success": true,
  "message": "App 文件索引已保存",
  "totalFiles": 2,
  "updatedFiles": 0,
  "newFiles": 0
}
```

### 5. 测试文件更新（contentHash 变化）

修改其中一个文件的 contentHash，再次上传：

```bash
curl -X POST http://localhost:8080/phone-sync/upload-app-files-index \
  -H "Content-Type: application/json" \
  -d '{
    "schemaVersion": 1,
    "deviceId": "android_main",
    "updatedTime": 1770000002000,
    "files": [
      {
        "fileId": "app_file_001",
        "name": "课程设计.pdf",
        "virtualPath": "LifeRecorder文件/课程设计.pdf",
        "source": "file_picker",
        "mimeType": "application/pdf",
        "size": 239000,
        "lastModified": 1770000002000,
        "contentHash": "hash_v2_updated",
        "availableLocally": false,
        "cachedPath": null,
        "linkedPhoneFileId": "phone_file_001"
      }
    ]
  }'
```

**预期响应：**
```json
{
  "success": true,
  "message": "App 文件索引已保存",
  "totalFiles": 2,
  "updatedFiles": 1,
  "newFiles": 0
}
```

验证：
- app_files_index.json 中 app_file_001 的信息已更新
- changes.json 中新增了 `app_file_index_updated` 事件
- index.json 中 app_files_index 项的 updatedTime 已更新

### 6. 测试新增文件

添加一个新的 fileId：

```bash
curl -X POST http://localhost:8080/phone-sync/upload-app-files-index \
  -H "Content-Type: application/json" \
  -d '{
    "schemaVersion": 1,
    "deviceId": "android_main",
    "updatedTime": 1770000003000,
    "files": [
      {
        "fileId": "app_file_003",
        "name": "新文档.docx",
        "virtualPath": "LifeRecorder文件/新文档.docx",
        "source": "file_picker",
        "mimeType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "size": 50000,
        "lastModified": 1770000003000,
        "contentHash": "hash_v3",
        "availableLocally": false,
        "cachedPath": null,
        "linkedPhoneFileId": null
      }
    ]
  }'
```

**预期响应：**
```json
{
  "success": true,
  "message": "App 文件索引已保存",
  "totalFiles": 3,
  "updatedFiles": 0,
  "newFiles": 1
}
```

验证：
- app_files_index.json 中有 3 个文件
- changes.json 中记录了 app_file_003 的新增事件

### 7. 验证 virtualPath 不会被当作真实路径

注意：
- `virtualPath` 字段只是 App 逻辑路径，用于展示
- 后端不会根据 virtualPath 创建真实目录或文件
- 所有真实文件都保存在 `cache/files/{fileId}/` 下

## 验收标准

✅ POST /phone-sync/upload-app-files-index 后生成 app_files_index.json  
✅ 重复上传同一个 fileId，不出现重复记录  
✅ contentHash 变化时更新旧 item，并记录 changes  
✅ today/index.json 出现 app_files_index  
✅ virtualPath 不会被当作真实文件路径读写  
✅ 所有路径限制在 D:/LifeRecorder 内  
