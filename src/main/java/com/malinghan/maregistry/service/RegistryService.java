//
// RegistryService.java
// 服务注册接口定义
//
// 该接口定义了服务注册中心的核心业务逻辑，包括服务注册、注销和查询功能
// 是服务注册中心业务层的抽象接口
//

package com.malinghan.maregistry.service;

import com.malinghan.maregistry.model.InstanceMeta;

import java.util.List;

/**
 * 服务注册接口
 * 
 * 该接口定义了服务注册中心的核心功能契约，所有注册服务实现类都必须实现此接口。
 * 提供了服务实例的注册、注销和查询等基本操作。
 * 
 * 设计原则：
 * - 面向接口编程，便于后续扩展不同的注册实现
 * - 保持接口简洁，只包含核心业务方法
 * - 参数和返回值使用领域模型对象，提高代码可读性
 */
public interface RegistryService {

    /**
     * 注册服务实例
     * 
     * 将指定的服务实例注册到注册中心，如果该实例已经存在则不会重复注册。
     * 
     * @param service 服务名称，用于标识一类服务
     *                例如："com.example.UserService"、"order-service"
     * @param instance 要注册的服务实例元数据
     * @return 注册后的服务实例对象（可能是原有实例或新创建的实例）
     * 
     * 业务规则：
     * - 同一服务下的相同实例（根据scheme+host+port+context判断）只会被注册一次
     * - 注册成功后实例可以在注册中心被发现
     */
    InstanceMeta register(String service, InstanceMeta instance);

    /**
     * 注销服务实例
     * 
     * 从注册中心移除指定的服务实例。
     * 
     * @param service 服务名称
     * @param instance 要注销的服务实例元数据
     * @return 被注销的服务实例对象
     * 
     * 注意事项：
     * - 如果实例不存在，通常不会抛出异常，而是静默处理
     * - 注销后该实例将无法被发现
     */
    InstanceMeta unregister(String service, InstanceMeta instance);

    /**
     * 获取指定服务的所有实例
     * 
     * 查询某个服务名称下注册的所有服务实例列表。
     * 
     * @param service 服务名称
     * @return 该服务的所有实例列表，如果服务不存在则返回null或空列表
     * 
     * 使用场景：
     * - 服务消费者进行负载均衡时获取可用实例列表
     * - 健康检查时批量检查实例状态
     * - 监控系统统计服务实例数量
     */
    List<InstanceMeta> getAllInstances(String service);
}