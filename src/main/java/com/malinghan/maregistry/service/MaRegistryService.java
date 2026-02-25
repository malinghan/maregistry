//
// MaRegistryService.java
// 服务注册中心内存实现类
//
// 该类提供了RegistryService接口的内存存储实现，使用LinkedMultiValueMap作为底层存储结构
// 适用于单机部署或开发测试环境
//

package com.malinghan.maregistry.service;

import com.malinghan.maregistry.model.InstanceMeta;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

/**
 * 服务注册中心内存实现类
 * 
 * 这是RegistryService接口的一个具体实现，使用内存数据结构存储服务注册信息。
 * 采用LinkedMultiValueMap作为核心存储容器，支持一个服务名对应多个实例的映射关系。
 * 
 * 特点：
 * - 基于内存存储，性能优异但不持久化
 * - 使用synchronized关键字保证并发安全
 * - 适合单机部署或开发测试环境
 * - 结构简单，易于理解和维护
 * 
 * 存储结构：
 * Map<serviceName, List<InstanceMeta>>
 * 例如：{"UserService" -> [instance1, instance2], "OrderService" -> [instance3]}
 */
@Service
public class MaRegistryService implements RegistryService {

    /**
     * 核心注册表存储结构
     * 
     * 使用LinkedMultiValueMap实现：
     * - Key: 服务名称（String）
     * - Value: 该服务的所有实例列表（List<InstanceMeta>）
     * - 保持插入顺序，便于遍历
     * - 线程安全的操作需要额外同步控制
     */
    private final MultiValueMap<String, InstanceMeta> REGISTRY = new LinkedMultiValueMap<>();

    /**
     * 注册服务实例
     * 
     * 将服务实例添加到注册表中，如果实例已存在则直接返回。
     * 
     * @param service 服务名称
     * @param instance 要注册的服务实例
     * @return 注册后的实例对象
     * 
     * 实现细节：
     * - 使用synchronized关键字保证并发安全
     * - 通过contains()方法检查实例是否已存在（基于InstanceMeta的equals方法）
     * - 避免重复注册相同的实例
     * 
     * 时间复杂度：O(n)，其中n为该服务已注册的实例数
     */
    @Override
    public synchronized InstanceMeta register(String service, InstanceMeta instance) {
        // 获取该服务已注册的所有实例
        List<InstanceMeta> instances = REGISTRY.get(service);
        
        // 检查实例是否已存在，避免重复注册
        if (instances != null && instances.contains(instance)) {
            return instance;
        }
        
        // 添加新的实例到注册表
        REGISTRY.add(service, instance);
        return instance;
    }

    /**
     * 注销服务实例
     * 
     * 从注册表中移除指定的服务实例。
     * 
     * @param service 服务名称
     * @param instance 要注销的服务实例
     * @return 被注销的实例对象
     * 
     * 实现细节：
     * - 使用synchronized关键字保证并发安全
     * - 如果服务不存在或实例不存在，静默处理不抛异常
     * - 基于InstanceMeta的equals方法定位要删除的实例
     * 
     * 时间复杂度：O(n)，其中n为该服务已注册的实例数
     */
    @Override
    public synchronized InstanceMeta unregister(String service, InstanceMeta instance) {
        // 获取该服务的所有实例
        List<InstanceMeta> instances = REGISTRY.get(service);
        
        // 如果实例列表存在，则从中移除指定实例
        if (instances != null) {
            instances.remove(instance);
        }
        
        return instance;
    }

    /**
     * 获取指定服务的所有实例
     * 
     * 查询某个服务名称下注册的所有服务实例列表。
     * 
     * @param service 服务名称
     * @return 该服务的所有实例列表，如果服务不存在则返回null
     * 
     * 实现细节：
     * - 此方法不需要同步，因为只是读取操作
     * - LinkedMultiValueMap的get方法是线程安全的读操作
     * - 返回的是内部存储的引用，调用方不应修改返回的列表
     * 
     * 时间复杂度：O(1)
     */
    @Override
    public List<InstanceMeta> getAllInstances(String service) {
        return REGISTRY.get(service);
    }
}