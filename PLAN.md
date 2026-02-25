# MaRegistry 版本迭代计划

> 本文档将项目已实现的功能拆解为可学习的迭代版本，并给出后续演进方向。
> 适合按版本逐步实现，理解每个阶段的设计思路。

---

## v1 — 基础注册与发现

**学习目标：** 理解注册中心最核心的数据结构和 CRUD 操作。

**实现内容：**
- `InstanceMeta` 服务实例模型（scheme/host/port/context/parameters）
- `RegistryService` 接口定义（register/unregister/getAllInstances）
- `MaRegistryService` 内存实现，使用 `LinkedMultiValueMap<String, InstanceMeta>` 存储
- `MaRegistryController` 暴露 `/reg`、`/unreg`、`/findAll` 三个接口

**关键设计：**
- `InstanceMeta` 用 `scheme+host+port+context` 作为 equals 判断依据（`@EqualsAndHashCode`）
- 注册时检查重复，避免同一实例多次注册
- `register/unregister` 加 `synchronized`，保证并发安全

**涉及文件：**
- `model/InstanceMeta.java`
- `service/RegistryService.java`
- `service/RegistryService.java`（register/unregister/getAllInstances 部分）
- `MaRegistryController.java`（/reg /unreg /findAll）

**测试：** 构造测试场景，输出直观结果验证功能

**文档总结:**   将该version的代码结构、核心功能实现、测试流程输出到v1.0.md

---

## v2 — 心跳续约与版本管理

**学习目标：** 理解注册中心如何感知服务存活，以及客户端如何感知服务变化。

**实现内容：**
- `TIMESTAMPS` 记录每个实例最后心跳时间（key: `service@url`）
- `VERSIONS` 记录每个服务的版本号，每次注册/注销时递增
- `VERSION` 全局原子版本号（`AtomicLong`）
- 新增接口：`/renew`、`/renews`、`/version`、`/versions`

**关键设计：**
- 版本号用于客户端轮询感知变化：客户端缓存版本号，定期对比，版本变了才拉取新实例列表
- `renew` 支持一次续约多个服务（`/renews?services=svc1,svc2`），减少网络请求
- 时间戳 key 格式 `service@url` 方便后续健康检查解析

**涉及文件：**
- `service/RegistryService.java`（renew/version/versions 部分）
- `RegistryController.java`（/renew /renews /version /versions）

**测试：** 构造测试场景，输出直观结果验证功能，不要单元测试版本的测试

**文档总结:**   将该version的代码结构、核心功能实现、测试流程输出到v2.0.md

---

## v3 — 健康检查与自动摘除

**学习目标：** 理解注册中心如何自动剔除宕机实例。

**实现内容：**
- `HealthChecker` 接口（start/stop）
- `HealthChecker` 定时任务实现（每 10 秒执行一次）
- 检查 `TIMESTAMPS`，超过 20 秒未续约的实例自动 unregister
- `RegistryConfig` 中通过 `initMethod/destroyMethod` 管理生命周期
- `ExceptionHandler` + `ExceptionResponse` 全局异常处理

**关键设计：**
- 健康检查依赖心跳时间戳，不主动探测实例，由实例主动上报存活
- 使用 `ScheduledExecutorService` 而非 Spring `@Scheduled`，更灵活
- Bean 生命周期管理：`@Bean(initMethod = "start", destroyMethod = "stop")`

**涉及文件：**
- `health/HealthChecker.java`
- `health/MaHealthChecker.java`
- `MaRegistryConfig.java`
- `MaExceptionHandler.java`
- `ExceptionResponse.java`

---

## v4 — 集群与主从选举

**学习目标：** 理解多节点集群的组织方式和简单选主算法。

**实现内容：**
- `Server` 集群节点模型（url/status/leader/version）
- `Cluster` 集群管理，初始化节点列表，自动识别本机节点
- `Election` 选举算法：基于 hashCode 确定性选主，保证所有节点选出同一个 Leader
- `ServerHealth` 定时任务（每 5 秒）：更新节点状态 → 触发选举 → 同步快照
- `MaRegistryConfigProperties` 读取配置中的 `serverList`
- Controller 新增：`/info`、`/cluster`、`/leader`、`/sl` 接口
- 写操作（register/unregister/renew）增加 `checkLeader()` 校验，非 Leader 拒绝写入

**关键设计：**
- 选举算法：遍历所有在线节点，选 hashCode 最小的作为 Leader，确定性保证各节点结果一致
- `MYSELF` 节点始终标记 `status=true`，其他节点状态通过 HTTP 探活更新
- `localhost/127.0.0.1` 自动替换为本机真实 IP，避免集群通信问题

**涉及文件：**
- `cluster/Server.java`
- `cluster/Cluster.java`
- `cluster/Election.java`
- `cluster/ServerHealth.java`（updateServers/doElect 部分）
- `MaRegistryConfigProperties.java`
- `MaRegistryConfig.java`（cluster bean）
- `MaRegistryController.java`（/info /cluster /leader /sl + checkLeader）

---

## v5 — 快照同步

**学习目标：** 理解 Follower 如何从 Leader 同步数据，保证集群数据一致。

**实现内容：**
- `Snapshot` 数据快照模型（REGISTRY + VERSIONS + TIMESTAMPS + version）
- `RegistryService.snapshot()` 生成当前数据快照
- `RegistryService.restore(snapshot)` 从快照恢复数据
- `ServerHealth.syncSnapshotFromLeader()` Follower 定期对比版本号，落后时从 Leader 拉取快照
- `HttpInvoker` 接口 + `OkHttpInvoker` 实现（集群间 HTTP 通信）
- Controller 新增 `/snapshot` 接口

**关键设计：**
- 同步条件：`!self.isLeader() && self.version < leader.version`，避免不必要的同步
- 快照是全量数据，简单但有效；数据量大时可优化为增量同步
- `HttpInvoker.Default` 静态单例，复用连接池（16 个连接，500ms 超时）
- `snapshot()` 和 `restore()` 都加 `synchronized`，防止并发读写不一致

**涉及文件：**
- `cluster/Snapshot.java`
- `cluster/ServerHealth.java`（syncSnapshotFromLeader 部分）
- `http/HttpInvoker.java`
- `http/OkHttpInvoker.java`
- `service/RegistryService.java`（snapshot/restore 部分）
- `RegistryController.java`（/snapshot）

---

## 后续迭代方向

### v6 — 数据持久化
- 定期将 Snapshot 序列化为 JSON 文件落盘
- 启动时自动加载快照恢复数据，解决重启数据丢失问题
- 抽象 `RegistryStore` 接口，支持文件/数据库等多种存储后端

### v7 — 一致性算法升级（Raft）
- 当前 hashCode 选举存在脑裂风险（网络分区时可能出现多 Leader）
- 实现 Raft 协议：随机超时选举 + 日志复制 + 心跳维持
- 写操作先写 Raft 日志，多数节点确认后再应用到状态机

### v8 — 命名空间与服务分组
- 支持多租户隔离（Namespace）
- 支持灰度/蓝绿等场景的服务分组（Group）
- 数据模型：`Namespace → Group → Service → Instance[]`

### v9 — 客户端 SDK
- 封装注册/注销/续约/订阅逻辑
- 提供 Spring Boot Starter，自动配置零代码接入
- 本地缓存 + 版本轮询，减少对注册中心的压力