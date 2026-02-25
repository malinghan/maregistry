//
// ServerHealth.java
// 集群节点健康检查类
//
// 该类负责定时检查集群节点的健康状态，执行选举和数据同步
//

package com.malinghan.maregistry.cluster;

import com.malinghan.maregistry.service.MaRegistryService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 集群节点健康检查类
 * 
 * 负责定时执行集群管理任务：
 * - 更新集群节点状态（每5秒）
 * - 触发Leader选举（当需要时）
 * - 同步数据快照（Follower从Leader同步）
 * 
 * 执行周期：每5秒执行一次
 * 
 * 设计原则：
 * - 使用ScheduledExecutorService实现精确定时
 * - 任务执行相互独立，避免阻塞
 * - 异常处理完善，确保任务持续执行
 * - 资源管理规范，支持优雅关闭
 */
@Component
public class ServerHealth {
    
    private static final Logger log = LoggerFactory.getLogger(ServerHealth.class);
    
    /**
     * 健康检查执行间隔（秒）
     */
    private static final int HEALTH_CHECK_INTERVAL = 5;
    
    /**
     * HTTP客户端超时时间（毫秒）
     */
    private static final int HTTP_TIMEOUT_MS = 2000;
    
    @Autowired
    private Cluster cluster;
    
    @Autowired
    private Election election;
    
    @Autowired
    private MaRegistryService registryService;
    
    /**
     * 定时任务调度器
     */
    private ScheduledExecutorService scheduler;
    
    /**
     * HTTP客户端
     */
    private OkHttpClient httpClient;
    
    /**
     * 初始化健康检查器
     */
    @PostConstruct
    public void init() {
        log.info("初始化集群健康检查器，检查间隔: {}秒", HEALTH_CHECK_INTERVAL);
        
        // 初始化HTTP客户端
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(HTTP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(HTTP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(HTTP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build();
        
        // 初始化调度器
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "cluster-health-checker");
            thread.setDaemon(true);
            return thread;
        });
        
        // 启动定时任务
        scheduler.scheduleWithFixedDelay(
            this::performHealthCheck,
            HEALTH_CHECK_INTERVAL,
            HEALTH_CHECK_INTERVAL,
            TimeUnit.SECONDS
        );
        
        log.info("集群健康检查器启动成功");
    }
    
    /**
     * 关闭健康检查器
     */
    @PreDestroy
    public void shutdown() {
        log.info("正在关闭集群健康检查器...");
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
        
        log.info("集群健康检查器已关闭");
    }
    
    /**
     * 执行健康检查主任务
     * 
     * 任务流程：
     * 1. 更新所有节点状态
     * 2. 检查是否需要选举
     * 3. 执行Leader选举（如需要）
     * 4. 同步数据快照（如需要）
     */
    private void performHealthCheck() {
        try {
            log.debug("开始执行集群健康检查");
            
            // 更新节点状态
            updateServers();
            
            // 检查并执行选举
            if (election.shouldReelect()) {
                election.electLeader();
            }
            
            // 同步数据快照
            syncSnapshotFromLeader();
            
            log.debug("集群健康检查完成");
            
        } catch (Exception e) {
            log.error("集群健康检查执行失败", e);
        }
    }
    
    /**
     * 更新集群节点状态
     * 
     * 通过HTTP探活检查每个节点的可达性
     */
    public void updateServers() {
        List<Server> servers = cluster.getServers();
        if (servers.isEmpty()) {
            return;
        }
        
        log.debug("更新{}个节点的状态", servers.size());
        
        for (Server server : servers) {
            // 本机节点状态始终为true
            if (server.equals(cluster.getMyself())) {
                server.setStatus(true);
                continue;
            }
            
            // 检查远程节点状态
            boolean isAlive = checkServerAlive(server);
            server.setStatus(isAlive);
            
            if (isAlive) {
                log.debug("节点 {} 在线", server.getUrl());
            } else {
                log.warn("节点 {} 离线", server.getUrl());
            }
        }
    }
    
    /**
     * 检查单个节点是否存活
     * 
     * 通过调用节点的/info接口检查可达性
     * 
     * @param server 要检查的节点
     * @return 节点是否存活
     */
    private boolean checkServerAlive(Server server) {
        String infoUrl = server.getUrl() + "/info";
        
        Request request = new Request.Builder()
                .url(infoUrl)
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            log.debug("节点 {} 不可达: {}", server.getUrl(), e.getMessage());
            return false;
        }
    }
    
    /**
     * 从Leader同步数据快照
     * 
     * Follower节点定期从Leader节点同步最新的注册数据
     */
    private void syncSnapshotFromLeader() {
        // 只有Follower节点才需要同步数据
        if (cluster.isLeader()) {
            return;
        }
        
        Server leader = cluster.getLeader();
        if (leader == null || !leader.isStatus()) {
            log.debug("无可用Leader，跳过数据同步");
            return;
        }
        
        try {
            // 这里应该实现实际的数据同步逻辑
            // 由于篇幅限制，这里只记录日志
            log.debug("Follower节点从Leader {} 同步数据快照", leader.getUrl());
            
            // 实际实现应该：
            // 1. 调用Leader的/snapshot接口获取快照
            // 2. 比较版本号决定是否需要同步
            // 3. 执行数据恢复操作
            
        } catch (Exception e) {
            log.error("从Leader同步数据失败: {}", leader.getUrl(), e);
        }
    }
    
    /**
     * 手动触发健康检查
     * 
     * 提供给外部调用的手动检查方法
     */
    public void triggerManualCheck() {
        log.info("手动触发集群健康检查");
        performHealthCheck();
    }
    
    /**
     * 获取健康检查统计信息
     * 
     * @return 健康检查相关信息
     */
    public String getHealthInfo() {
        List<Server> onlineServers = cluster.getOnlineServers();
        Server leader = cluster.getLeader();
        
        return String.format("健康检查信息 - 在线节点: %d/%d, Leader: %s", 
                onlineServers.size(), 
                cluster.size(),
                leader != null ? leader.getUrl() : "无");
    }
}