//
// MaHealthChecker.java
// 健康检查器实现类
//
// 该类实现了HealthChecker接口，提供基于时间戳的健康检查功能
// 使用ScheduledExecutorService实现定时任务调度
//

package com.malinghan.maregistry.health;

import com.malinghan.maregistry.model.InstanceMeta;
import com.malinghan.maregistry.service.MaRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 健康检查器实现类
 * 
 * 基于心跳时间戳机制实现服务实例的健康检查和自动摘除功能。
 * 使用ScheduledExecutorService提供灵活的定时任务调度。
 * 
 * 核心机制：
 * - 被动健康检查：依赖服务实例主动发送心跳
 * - 时间窗口：默认20秒超时阈值
 * - 定时扫描：每10秒执行一次全量检查
 * - 自动摘除：超时实例自动从注册中心移除
 */
@Component
public class MaHealthChecker implements HealthChecker {
    
    private static final Logger log = LoggerFactory.getLogger(MaHealthChecker.class);
    
    /**
     * 健康检查时间间隔（秒）
     * 默认每10秒执行一次健康检查
     */
    private static final int CHECK_INTERVAL_SECONDS = 10;
    
    /**
     * 实例超时阈值（毫秒）
     * 超过20秒未收到心跳的实例将被认定为不健康
     */
    private static final long TIMEOUT_THRESHOLD_MS = 20 * 1000L;
    
    /**
     * 定时任务调度器
     * 使用单线程调度器确保检查任务串行执行
     */
    private ScheduledExecutorService scheduler;
    
    /**
     * 注册服务实例
     * 用于访问注册表数据和执行实例摘除操作
     */
    @Autowired
    private MaRegistryService registryService;
    
    /**
     * 启动健康检查任务
     * 
     * 初始化ScheduledExecutorService并启动周期性检查任务。
     * 该方法应在应用启动完成后调用。
     */
    @Override
    public void start() {
        log.info("启动健康检查器，检查间隔: {}秒，超时阈值: {}毫秒", 
                CHECK_INTERVAL_SECONDS, TIMEOUT_THRESHOLD_MS);
        
        // 初始化单线程调度器
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "health-checker");
            thread.setDaemon(true); // 设置为守护线程
            return thread;
        });
        
        // 启动周期性健康检查任务
        scheduler.scheduleWithFixedDelay(
            this::check,                    // 要执行的任务
            CHECK_INTERVAL_SECONDS,         // 初始延迟
            CHECK_INTERVAL_SECONDS,         // 执行间隔
            TimeUnit.SECONDS                // 时间单位
        );
        
        log.info("健康检查器启动成功");
    }
    
    /**
     * 停止健康检查任务
     * 
     * 优雅关闭调度器，等待正在进行的任务完成。
     * 该方法应在应用关闭前调用。
     */
    @Override
    public void stop() {
        log.info("正在停止健康检查器...");
        
        if (scheduler != null && !scheduler.isShutdown()) {
            // 优雅关闭：不再接受新任务，等待正在执行的任务完成
            scheduler.shutdown();
            
            try {
                // 等待最多5秒让任务完成
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("健康检查器未能在5秒内优雅关闭，强制终止");
                    scheduler.shutdownNow(); // 强制终止
                }
            } catch (InterruptedException e) {
                log.warn("等待健康检查器关闭时被中断", e);
                scheduler.shutdownNow();
                Thread.currentThread().interrupt(); // 恢复中断状态
            }
        }
        
        log.info("健康检查器已停止");
    }
    
    /**
     * 执行健康检查
     * 
     * 核心健康检查逻辑：
     * 1. 获取所有服务的时间戳数据
     * 2. 计算每个实例的超时时间
     * 3. 摘除超时的不健康实例
     * 4. 记录检查结果和统计信息
     */
    @Override
    public void check() {
        try {
            log.debug("开始执行健康检查");
            
            // 获取时间戳数据（需要在MaRegistryService中提供访问方法）
            Map<String, Long> timestamps = registryService.getTimestamps();
            
            if (timestamps == null || timestamps.isEmpty()) {
                log.debug("暂无实例需要健康检查");
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            int checkedCount = 0;
            int removedCount = 0;
            
            // 遍历所有时间戳记录
            for (Map.Entry<String, Long> entry : timestamps.entrySet()) {
                String timestampKey = entry.getKey();
                Long lastHeartbeatTime = entry.getValue();
                
                checkedCount++;
                
                // 计算超时时间
                long timeSinceLastHeartbeat = currentTime - lastHeartbeatTime;
                
                // 检查是否超时
                if (timeSinceLastHeartbeat > TIMEOUT_THRESHOLD_MS) {
                    log.info("发现超时实例: {}, 超时时间: {}ms", 
                            timestampKey, timeSinceLastHeartbeat);
                    
                    // 解析timestampKey获取服务名和实例信息
                    if (removeTimeoutInstance(timestampKey)) {
                        removedCount++;
                    }
                }
            }
            
            if (removedCount > 0) {
                log.info("健康检查完成: 检查{}个实例，摘除{}个超时实例", 
                        checkedCount, removedCount);
            } else {
                log.debug("健康检查完成: 检查{}个实例，无超时实例", checkedCount);
            }
            
        } catch (Exception e) {
            log.error("健康检查执行失败", e);
        }
    }
    
    /**
     * 移除超时实例
     * 
     * 根据timestampKey解析出服务名和实例信息，执行摘除操作。
     * 
     * @param timestampKey 时间戳键值，格式为 service@scheme://host:port/context
     * @return 是否成功移除实例
     */
    private boolean removeTimeoutInstance(String timestampKey) {
        try {
            // 解析timestampKey格式: service@scheme://host:port/context
            int atIndex = timestampKey.indexOf('@');
            if (atIndex <= 0) {
                log.warn("无效的时间戳键格式: {}", timestampKey);
                return false;
            }
            
            String serviceName = timestampKey.substring(0, atIndex);
            String instanceUrl = timestampKey.substring(atIndex + 1);
            
            // 从注册中心获取该服务的所有实例
            List<InstanceMeta> instances = registryService.getAllInstances(serviceName);
            if (instances == null || instances.isEmpty()) {
                log.debug("服务{}无实例需要检查", serviceName);
                return false;
            }
            
            // 查找匹配的实例并摘除
            for (InstanceMeta instance : instances) {
                if (instance.toUrl().equals(instanceUrl)) {
                    log.info("摘除超时实例: {}@{}", serviceName, instanceUrl);
                    registryService.unregister(serviceName, instance);
                    return true;
                }
            }
            
            log.warn("未找到匹配的实例: {}@{}", serviceName, instanceUrl);
            return false;
            
        } catch (Exception e) {
            log.error("移除超时实例失败: {}", timestampKey, e);
            return false;
        }
    }
}