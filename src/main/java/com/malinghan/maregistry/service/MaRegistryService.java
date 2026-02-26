//
// MaRegistryService.java
// 服务注册中心内存实现类
//
// 该类提供了RegistryService接口的内存存储实现，使用LinkedMultiValueMap作为底层存储结构
// 适用于单机部署或开发测试环境
//

package com.malinghan.maregistry.service;

import com.malinghan.maregistry.cluster.MaRegistryConfigProperties;
import com.malinghan.maregistry.cluster.Snapshot;
import com.malinghan.maregistry.model.InstanceMeta;
import com.malinghan.maregistry.store.RegistryStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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

    private static final Logger log = LoggerFactory.getLogger(MaRegistryService.class);

    @Autowired(required = false)
    private RegistryStore store;

    @Autowired
    private MaRegistryConfigProperties properties;

    private ScheduledExecutorService scheduler;

    private final MultiValueMap<String, InstanceMeta> REGISTRY = new LinkedMultiValueMap<>();
    
    /**
     * 记录每个实例最后心跳时间戳
     * Key格式: service@scheme://host:port/context
     * Value: 时间戳（毫秒）
     */
    private final Map<String, Long> TIMESTAMPS = new ConcurrentHashMap<>();
    
    /**
     * 记录每个服务的版本号
     * Key: 服务名称
     * Value: 版本号
     */
    private final Map<String, Long> VERSIONS = new ConcurrentHashMap<>();
    
    /**
     * 全局原子版本号，用于跟踪整个注册中心的变化
     */
    private final AtomicLong VERSION = new AtomicLong(0);
    
    /**
     * 快照版本号，用于集群数据同步
     */
    private final AtomicLong SNAPSHOT_VERSION = new AtomicLong(0);

    @PostConstruct
    public void init() {
        if (store != null) {
            Snapshot snapshot = store.load();
            if (snapshot != null) {
                restore(snapshot);
                log.info("Restored registry from snapshot, version={}", snapshot.getVersion());
            }
            int interval = properties.getSnapshotInterval();
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() -> store.save(snapshot()), interval, interval, TimeUnit.SECONDS);
        }
    }

    @PreDestroy
    public void destroy() {
        if (store != null) {
            store.save(snapshot());
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

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

    /**
     * 心跳续约单个服务
     * 
     * 更新指定服务实例的心跳时间戳，并递增相关版本号。
     * 
     * @param service 服务名称
     * @param instance 服务实例元数据
     * @return 续约后的实例信息
     */
    @Override
    public synchronized InstanceMeta renew(String service, InstanceMeta instance) {
        // 生成时间戳key
        String timestampKey = service + "@" + instance.toUrl();
        
        // 更新时间戳
        TIMESTAMPS.put(timestampKey, System.currentTimeMillis());
        
        // 递增服务版本号
        VERSIONS.merge(service, 1L, Long::sum);
        
        // 递增全局版本号
        VERSION.incrementAndGet();
        
        return instance;
    }

    /**
     * 批量心跳续约多个服务
     * 
     * 一次性更新多个服务实例的心跳时间戳。
     * 
     * @param services 服务名称数组
     * @param instance 服务实例元数据
     * @return 续约后的实例信息
     */
    @Override
    public synchronized InstanceMeta renews(String[] services, InstanceMeta instance) {
        // 为每个服务执行续约操作
        for (String service : services) {
            String timestampKey = service + "@" + instance.toUrl();
            TIMESTAMPS.put(timestampKey, System.currentTimeMillis());
            VERSIONS.merge(service, 1L, Long::sum);
        }
        
        // 递增全局版本号（只递增一次）
        VERSION.incrementAndGet();
        
        return instance;
    }

    /**
     * 获取单个服务的版本号
     * 
     * @param service 服务名称
     * @return 服务版本号，如果服务不存在则返回0
     */
    @Override
    public Long version(String service) {
        return VERSIONS.getOrDefault(service, 0L);
    }

    /**
     * 批量获取多个服务的版本号
     * 
     * @param services 服务名称数组
     * @return 服务名称到版本号的映射关系
     */
    @Override
    public Map<String, Long> versions(String[] services) {
        Map<String, Long> result = new ConcurrentHashMap<>();
        for (String service : services) {
            result.put(service, VERSIONS.getOrDefault(service, 0L));
        }
        return result;
    }
    
    /**
     * 获取所有时间戳数据
     * 
     * 供健康检查器使用，用于检查实例的存活状态。
     * 
     * @return 时间戳映射关系，key为service@url格式，value为时间戳
     */
    public Map<String, Long> getTimestamps() {
        return TIMESTAMPS;
    }
    
    /**
     * 生成当前数据快照
     * 
     * 创建包含当前所有注册数据的快照对象，用于集群间数据同步。
     * 
     * @return 当前数据快照
     * 
     * 实现细节：
     * - 使用synchronized保证并发安全
     * - 创建数据副本避免外部修改影响内部状态
     * - 递增快照版本号
     */
    @Override
    public synchronized Snapshot snapshot() {
        // 递增快照版本号
        long snapshotVersion = SNAPSHOT_VERSION.incrementAndGet();
        
        // 创建快照对象
        Snapshot snapshot = new Snapshot(
            new ConcurrentHashMap<>(REGISTRY), // 注册表数据副本
            VERSIONS,      // 版本映射
            TIMESTAMPS,    // 时间戳信息
            snapshotVersion // 快照版本号
        );
        
        return snapshot;
    }
    
    /**
     * 从快照恢复数据
     * 
     * 使用提供的快照数据恢复注册中心状态。
     * 
     * @param snapshot 要恢复的数据快照
     * 
     * 实现细节：
     * - 使用synchronized保证并发安全
     * - 清空现有数据后再恢复
     * - 更新快照版本号
     */
    @Override
    public synchronized void restore(Snapshot snapshot) {
        if (snapshot == null) return;
        
        // 清空现有数据
        REGISTRY.clear();
        VERSIONS.clear();
        TIMESTAMPS.clear();
        
        // 恢复注册表数据
        if (snapshot.getREGISTRY() != null) {
            snapshot.getREGISTRY().forEach((key, value) -> {
                if (value != null) {
                    REGISTRY.addAll(key, value);
                }
            });
        }
        
        // 恢复版本信息
        if (snapshot.getVERSIONS() != null) {
            VERSIONS.putAll(snapshot.getVERSIONS());
        }
        
        // 恢复时间戳
        if (snapshot.getTIMESTAMPS() != null) {
            TIMESTAMPS.putAll(snapshot.getTIMESTAMPS());
        }
        
        // 更新快照版本号
        SNAPSHOT_VERSION.set(snapshot.getVersion());
        
        // 更新全局版本号
        VERSION.set(Math.max(VERSION.get(), snapshot.getVersion()));
    }
}