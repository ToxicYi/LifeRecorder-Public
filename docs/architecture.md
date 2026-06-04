# LifeRecorder 架构说明

## 1. 项目目标

LifeRecorder 是一个本地优先的人生记录系统。当前阶段先从笔记、文件入口和 AI 聊天开始，帮助用户把日常想法、资料、对话和生活片段稳定记录下来。

未来 LifeRecorder 会逐步发展为一个更完整的 Life OS：它不仅保存生活记录，还可以总结阶段性经历、分析长期趋势、辅助整理文件、理解个人上下文，并在用户授权后调用本地 Agent 能力处理复杂任务。整体方向是让手机端负责轻量记录和展示，让电脑端负责重任务、本地文件和复杂 Agent 编排。

## 2. 当前模块

### Android App

Android App 是当前 LifeRecorder 的主要入口，负责用户日常使用体验。它承担轻量、即时、贴近记录场景的功能。

当前 Android App 负责：

- 写笔记
- 查看笔记
- 管理文件/资源入口
- 普通 AI 聊天
- 未来的 App 端轻量总结和长文本修改

Android App 应优先保持简单、稳定、响应快。它不应该承担过重的本地文件扫描、复杂工具调用或长时间 Agent 任务，而是作为用户记录、确认和查看结果的前端。

### liferecorder-server

`liferecorder-server` 是当前项目中的 Spring Boot server，主要用于本地开发、局域网通信和早期服务端能力验证。

当前 Spring Boot server 负责：

- 提供本地/局域网接口
- 转发 AI API 请求
- 作为早期测试服务端
- 未来可能被 Windows Agent 或云端中转方案部分替代

在当前阶段，`liferecorder-server` 可以帮助 Android App 连接电脑端环境、测试 AI 请求转发和验证数据流。但随着 Supabase、Windows Agent、OpenClaw/qclaw 集成逐步成熟，它的一部分职责可能会被拆分或替代。

### AI API

AI API 是当前 LifeRecorder 的基础智能能力来源，适合处理普通文本任务和轻量交互。

AI API 负责：

- 普通聊天
- 文本总结
- 文本改写
- 笔记分类
- 标题生成

普通聊天和简单文本处理应尽量直接走 AI API，避免引入不必要的 Supabase、Windows Agent 或 OpenClaw/qclaw 链路。这样可以减少延迟、降低复杂度，也更容易调试。

## 3. 未来模块

### Supabase

Supabase 未来可以作为云端同步和任务中转层。它的核心价值不是替代本地能力，而是解决手机和电脑不在同一网络时的通信问题。

Supabase 负责：

- 手机和电脑之间的数据同步
- 保存 pending / processing / done 任务状态
- 让手机不必直接访问电脑
- 支持跨网络任务传递

典型场景是：Android App 创建一个复杂任务，把任务写入 Supabase；Windows Agent 监听任务队列，取走任务并处理；处理完成后把结果写回 Supabase；Android App 再读取结果并展示给用户。

### Windows Agent

Windows Agent 是未来电脑端重任务的核心执行者。它运行在用户电脑上，拥有访问本地文件和调用本地工具的能力，但这些能力必须以用户授权为前提。

Windows Agent 负责：

- 读取用户授权的本地文件夹
- 处理长文本和大文件
- 调用本地 AI / Ollama
- 调用 OpenClaw 或 qclaw
- 执行复杂 Agent 任务
- 把结果写回 Supabase 或返回给手机

Windows Agent 适合处理 Android App 不适合做的任务，例如长期文件搜索、大文件分析、多步骤工具调用、电脑端命令执行和本地模型推理。

### App-side Mini Agent

App-side Mini Agent 是 Android App 内部的轻量智能处理层。它不追求完整 Agent 能力，而是专注于 App 内部可控数据和用户主动选择的数据。

App 端 Mini Agent 负责：

- 读取 App 内部笔记
- 读取用户主动选择的本地文件
- 分块处理长文本
- 调用 AI API 生成修改草稿
- 用户确认后保存为新版本

App-side Mini Agent 适合单篇笔记总结、局部改写、小文件分析和用户可即时确认的内容修改。它不应该绕过用户确认直接覆盖重要内容。

### OpenClaw / qclaw

OpenClaw 和 qclaw 主要作为电脑端 Agent 能力，不直接放进 Android App。它们更适合运行在 Windows Agent 所在的电脑环境中。

OpenClaw 和 qclaw 负责：

- 工具调用
- 本地文件处理
- 命令执行
- 复杂任务编排

Android App 不应该完整承载 OpenClaw/qclaw。手机端应保持轻量，把复杂任务交给 Windows Agent，再由 Windows Agent 调用 OpenClaw/qclaw 或 Ollama。

## 4. 主要链路

### 普通 AI 聊天链路

```text
Android App -> AI API -> Android App
```

说明：用于普通聊天、简单总结、文本润色，不需要经过 OpenClaw 或 Supabase。

这条链路应该是 LifeRecorder 中最短、最稳定的 AI 链路。只要任务不需要访问电脑文件、不需要长时间执行、不需要工具调用，就优先使用这条链路。

### App 端轻量文件/笔记总结链路

```text
Android App -> 读取本地笔记/文件 -> AI API -> Android App 保存结果
```

说明：适合单篇笔记、小型 txt/md/json 文件总结。

这条链路由 Android App 直接读取 App 内部笔记或用户主动选择的文件，然后调用 AI API 生成总结、改写或标题。对于文件修改，应先生成草稿，用户确认后再保存为新版本。

### 复杂 Agent 任务链路

```text
Android App -> Supabase -> Windows Agent -> OpenClaw/qclaw/Ollama -> Supabase -> Android App
```

说明：适合本地文件搜索、长任务、大文件分析、跨设备任务、电脑端工具调用。

这条链路用于 LifeRecorder 的高级能力。Android App 只负责提交任务和展示结果；Supabase 负责任务中转和状态同步；Windows Agent 负责执行；OpenClaw/qclaw/Ollama 负责复杂推理、本地工具调用和本地模型能力。

### 局域网直连链路

```text
Android App -> Windows LifeRecorder 本地服务 -> Windows Agent -> 本地文件/AI -> Android App
```

说明：当手机和电脑在同一个局域网时，可以走更快的直连方式，类似 vivo 办公套件的手机电脑互联模式。

这条链路可以减少云端中转延迟，适合家庭、宿舍、办公室等同网环境。但它需要处理局域网发现、连接认证、防止未授权访问等问题。

## 5. 架构原则

- 普通聊天走最短链路
- 复杂任务交给 Agent
- 手机端负责记录和展示
- Windows 端负责重任务和本地能力
- Supabase 负责跨网络中转
- 重要文件修改必须先生成草稿，用户确认后再保存
- 不把 OpenClaw/qclaw 完整搬进手机端
- 尽量保持模块边界清晰

这些原则的目标是避免系统过早复杂化。LifeRecorder 应该先保证核心记录体验可靠，再逐步把 AI、同步、Agent、本地文件处理等能力接入进来。

## 6. 当前开发优先级

1. 先稳定 Android App 的笔记和基础 AI 聊天。
2. 补充 docs/architecture.md 和 docs/flows.md。
3. 做 App 端单篇笔记 AI 总结/改写。
4. 再做 Supabase 同步和任务队列。
5. 最后再做 Windows Agent 和 OpenClaw/qclaw 深度集成。

这个文档是架构说明，不是最终设计。后续每次重大架构调整，都应该同步更新。
