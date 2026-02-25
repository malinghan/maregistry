//
// Snapshot.java
// 数据快照模型类
//
// 该类用于封装注册中心的完整数据状态，支持集群间的数据同步
//

package com.malinghan.maregistry.cluster;

import com.malinghan.maregistry.model.InstanceMeta;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据快照模型类
 * 
 * 封装注册中心的完整数据状态，用于集群间的数据同步。
 * 包含注册表数据、版本信息和时间戳等核心数据。
 * 
 * 核心属性：
 * - REGISTRY: 服务注册表数据
 * - VERSIONS: 服务版本映射
 * - TIMESTAMPS: 时间戳信息
 * - version: 快照版本号
 * 
 * 设计原则：
 * - 数据完整性：包含所有必要的注册中心状态信息
 * - 版本控制：通过version字段支持增量同步判断
 * - 线程安全：使用ConcurrentHashMap保证并发安全
 * - 序列化友好：支持JSON序列化传输
 */
public class Snapshot {
    
    /**
     * 服务注册表数据
     * key: 服务名称
     * value: 该服务的所有实例列表
     */
    private Map<String, List<InstanceMeta>> REGISTRY;
    
    /**
     * 服务版本映射
     * key: 服务名称
     * value: 该服务的版本号
     */
    private Map<String, Long> VERSIONS;
    
    /**
     * 时间戳信息
     * key: 时间戳键（通常为服务名+实例标识）
     * value: 最后更新时间戳
     */
    private Map<String, Long> TIMESTAMPS;
    
    /**
     * 快照版本号
     * 用于标识快照的新旧程度，支持增量同步判断
     */
    private long version;
    
    /**
     * 快照创建时间
     */
    private long createTime;
    
    /**
     * 默认构造函数
     */
    public Snapshot() {
        this.REGISTRY = new ConcurrentHashMap<>();
        this.VERSIONS = new ConcurrentHashMap<>();
        this.TIMESTAMPS = new ConcurrentHashMap<>();
        this.version = 0;
        this.createTime = System.currentTimeMillis();
    }
    
    /**
     * 完整构造函数
     * 
     * @param registry 注册表数据
     * @param versions 版本映射
     * @param timestamps 时间戳信息
     * @param version 快照版本号
     */
    public Snapshot(Map<String, List<InstanceMeta>> registry,
                   Map<String, Long> versions,
                   Map<String, Long> timestamps,
                   long version) {
        this.REGISTRY = registry != null ? new ConcurrentHashMap<>(registry) : new ConcurrentHashMap<>();
        this.VERSIONS = versions != null ? new ConcurrentHashMap<>(versions) : new ConcurrentHashMap<>();
        this.TIMESTAMPS = timestamps != null ? new ConcurrentHashMap<>(timestamps) : new ConcurrentHashMap<>();
        this.version = version;
        this.createTime = System.currentTimeMillis();
    }
    
    // Getter和Setter方法
    public Map<String, List<InstanceMeta>> getREGISTRY() {
        return REGISTRY;
    }
    
    public void setREGISTRY(Map<String, List<InstanceMeta>> REGISTRY) {
        this.REGISTRY = REGISTRY != null ? new ConcurrentHashMap<>(REGISTRY) : new ConcurrentHashMap<>();
    }
    
    public Map<String, Long> getVERSIONS() {
        return VERSIONS;
    }
    
    public void setVERSIONS(Map<String, Long> VERSIONS) {
        this.VERSIONS = VERSIONS != null ? new ConcurrentHashMap<>(VERSIONS) : new ConcurrentHashMap<>();
    }
    
    public Map<String, Long> getTIMESTAMPS() {
        return TIMESTAMPS;
    }
    
    public void setTIMESTAMPS(Map<String, Long> TIMESTAMPS) {
        this.TIMESTAMPS = TIMESTAMPS != null ? new ConcurrentHashMap<>(TIMESTAMPS) : new ConcurrentHashMap<>();
    }
    
    public long getVersion() {
        return version;
    }
    
    public void setVersion(long version) {
        this.version = version;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
    
    /**
     * 检查快照是否为空
     * 
     * @return 快照是否不包含任何数据
     */
    public boolean isEmpty() {
        return REGISTRY.isEmpty() && VERSIONS.isEmpty() && TIMESTAMPS.isEmpty();
    }
    
    /**
     * 获取快照大小（服务数量）
     * 
     * @return 注册的服务数量
     */
    public int size() {
        return REGISTRY.size();
    }
    
    /**
     * 检查是否需要同步
     * 
     * @param localVersion 本地版本号
     * @return 是否需要从Leader同步数据
     */
    public boolean shouldSync(long localVersion) {
        return this.version > localVersion;
    }
    
    /**
     * 合并另一个快照的数据
     * 
     * @param other 另一个快照
     */
    public void merge(Snapshot other) {
        if (other == null) return;
        
        // 合并注册表数据
        if (other.REGISTRY != null) {
            this.REGISTRY.putAll(other.REGISTRY);
        }
        
        // 合并版本信息
        if (other.VERSIONS != null) {
            this.VERSIONS.putAll(other.VERSIONS);
        }
        
        // 合并时间戳
        if (other.TIMESTAMPS != null) {
            this.TIMESTAMPS.putAll(other.TIMESTAMPS);
        }
        
        // 更新版本号为较大的那个
        if (other.version > this.version) {
            this.version = other.version;
        }
    }
    
    /**
     * 清空快照数据
     */
    public void clear() {
        this.REGISTRY.clear();
        this.VERSIONS.clear();
        this.TIMESTAMPS.clear();
        this.version = 0;
        this.createTime = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return "Snapshot{" +
                "version=" + version +
                ", services=" + REGISTRY.size() +
                ", createTime=" + createTime +
                '}';
    }
}