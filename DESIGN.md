# KKRegistry

一个轻量级的分布式服务注册中心，基于 Spring Boot 实现，支持集群部署、主从选举和数据同步。

## 项目概述

KKRegistry 是一个自研的服务注册中心，提供服务注册、发现、健康检查等核心功能，适用于微服务架构中的服务治理场景。

## 技术栈

- **Java 17**
- **Spring Boot 3.2.4**
- **Spring Cloud Commons 4.1.1**
- **OkHttp 4.12.0** - HTTP 客户端
- **Fastjson 1.2.83** - JSON 序列化
- **Lombok** - 简化代码

## 核心功能

### 1. 服务注册与发现
- 服务实例注册 (register)
- 服务实例注销 (unregister)
- 查询所有服务实例 (findAll)
- 服务心跳续约 (renew/renews)
- 服务版本管理 (version/versions)

### 2. 集群管理
- 多节点集群部署
- 自动健康检查
- 主从选举机制
- 数据快照同步

### 3. 健康检查
- 定时检查服务实例存活状态
- 自动剔除超时实例（默认 20 秒超时）
- 集群节点健康监控（每 5 秒检查一次）

## 项目架构

```
kkregistry/
├── src/main/java/io/github/kimmking/kkregistry/
│   ├── KkregistryApplication.java          # 应用启动类
│   ├── KKRegistryController.java           # REST API 控制器
│   ├── KKRegistryConfig.java               # Bean 配置类
│   ├── KKRegistryConfigProperties.java     # 配置属性
│   ├── KKExceptionHandler.java             # 全局异常处理
│   ├── ExceptionResponse.java              # 异常响应封装
│   │
│   ├── model/
│   │   └── InstanceMeta.java               # 服务实例元数据模型
│   │
│   ├── service/
│   │   ├── RegistryService.java            # 注册服务接口
│   │   └── KKRegistryService.java          # 注册服务实现
│   │
│   ├── cluster/
│   │   ├── Cluster.java                    # 集群管理
│   │   ├── Server.java                     # 服务器节点模型
│   │   ├── ServerHealth.java               # 集群健康检查
│   │   ├── Election.java                   # 主节点选举
│   │   └── Snapshot.java                   # 数据快照
│   │
│   ├── health/
│   │   ├── HealthChecker.java              # 健康检查接口
│   │   └── KKHealthChecker.java            # 健康检查实现
│   │
│   └── http/
│       ├── HttpInvoker.java                # HTTP 调用接口
│       └── OkHttpInvoker.java              # OkHttp 实现
│
├── src/main/resources/
│   └── application.yml                     # 应用配置
│
├── registry.http                           # HTTP 测试文件
└── pom.xml                                 # Maven 配置
```

## 核心组件说明

### 1. InstanceMeta (服务实例元数据)
表示一个服务实例的完整信息：
- `scheme`: 协议（http/https）
- `host`: 主机地址
- `port`: 端口号
- `context`: 上下文路径
- `status`: 在线状态
- `parameters`: 自定义参数（如环境、标签等）

### 2. RegistryService (注册服务)
核心业务逻辑：
- 使用 `LinkedMultiValueMap` 存储服务实例映射
- 使用 `ConcurrentHashMap` 管理版本和时间戳
- 使用 `AtomicLong` 维护全局版本号
- 提供快照（Snapshot）和恢复（Restore）功能

### 3. Cluster (集群管理)
集群协调机制：
- 自动发现本机 IP 地址
- 初始化集群节点列表
- 维护当前节点状态
- 提供 Leader 节点查询

### 4. ServerHealth (集群健康检查)
定时任务（每 5 秒执行）：
1. **更新服务器状态**: 并行检查所有节点的 `/info` 接口
2. **选举 Leader**: 当无 Leader 或多 Leader 时触发选举
3. **同步快照**: Follower 节点从 Leader 同步数据

### 5. Election (选举算法)
简单的确定性选举算法：
- 基于节点 hashCode 选举
- 保证所有节点选出同一个 Leader
- 只有状态正常的节点参与选举

### 6. KKHealthChecker (实例健康检查)
定时任务（每 10 秒执行）：
- 检查服务实例心跳时间戳
- 超过 20 秒未续约的实例自动注销

## API 接口

### 服务注册与发现
- `POST /reg?service={serviceName}` - 注册服务实例
- `POST /unreg?service={serviceName}` - 注销服务实例
- `GET /findAll?service={serviceName}` - 查询所有实例
- `POST /renew?service={serviceName}` - 单服务心跳续约
- `POST /renews?services={service1,service2}` - 多服务心跳续约
- `POST /version?service={serviceName}` - 获取服务版本
- `POST /versions?services={service1,service2}` - 获取多服务版本

### 集群管理
- `GET /info` - 获取当前节点信息
- `GET /cluster` - 获取集群所有节点
- `GET /leader` - 获取 Leader 节点
- `GET /snapshot` - 获取数据快照
- `GET /sl` - 手动设置当前节点为 Leader（测试用）

## 配置说明

```yaml
server:
  port: 8484

spring:
  application:
    name: kkregistry

kkregistry:
  serverList:
    - http://127.0.0.1:8484
    - http://127.0.0.1:8485
    - http://127.0.0.1:8486
```

## 快速开始

### 1. 构建项目
```bash
mvn clean package
```

### 2. 启动集群节点
```bash
# 节点 1
java -jar target/kkregistry-0.0.1-SNAPSHOT.jar --server.port=8484

# 节点 2
java -jar target/kkregistry-0.0.1-SNAPSHOT.jar --server.port=8485

# 节点 3
java -jar target/kkregistry-0.0.1-SNAPSHOT.jar --server.port=8486
```

### 3. 注册服务实例
```bash
curl -X POST "http://localhost:8484/reg?service=io.github.kimmking.kkrpc.UserService" \
  -H "Content-Type: application/json" \
  -d '{
    "scheme": "http",
    "host": "127.0.0.1",
    "port": 8081,
    "context": "kkrpc",
    "status": true,
    "parameters": {
      "env": "dev",
      "tag": "RED"
    }
  }'
```

### 4. 查询服务实例
```bash
curl "http://localhost:8484/findAll?service=io.github.kimmking.kkrpc.UserService"
```

## 设计特点

### 1. 主从架构
- 只有 Leader 节点接受写操作（register/unregister/renew）
- 所有节点都可以处理读操作（findAll/version）
- 非 Leader 节点写操作会返回错误，提示 Leader 地址

### 2. 数据一致性
- Leader 节点维护权威数据
- Follower 节点定期从 Leader 同步快照
- 基于版本号判断数据是否需要同步

### 3. 高可用性
- 自动故障检测和 Leader 重新选举
- 节点故障不影响其他节点运行
- 数据快照机制保证数据不丢失

### 4. 简单高效
- 内存存储，性能优异
- 无外部依赖，部署简单
- RESTful API，易于集成

## 注意事项

1. **数据持久化**: 当前版本使用内存存储，重启后数据丢失
2. **选举算法**: 使用简单的 hashCode 选举，适合小规模集群
3. **网络分区**: 未处理脑裂问题，不适合跨机房部署
4. **安全性**: 未实现认证授权机制

## 后续优化方向

- 数据持久化（RocksDB/LevelDB）
- 更强的一致性算法（Raft）
- 服务分组和命名空间
- 权限控制和安全认证
- 监控指标和管理界面
- 客户端 SDK

## 作者

kimmking (kimmking@apache.org)

## 许可证

本项目为学习和研究目的开发。