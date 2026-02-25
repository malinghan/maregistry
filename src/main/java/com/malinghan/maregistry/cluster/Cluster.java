//
// Cluster.java
// 集群管理类
//
// 该类负责管理注册中心集群的节点信息，包括节点发现、状态维护和本机识别
//

package com.malinghan.maregistry.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 集群管理类
 * 
 * 负责管理注册中心集群的所有节点，包括：
 * - 集群节点的初始化和维护
 * - 本机节点的自动识别
 * - 节点状态的跟踪和更新
 * - Leader节点的管理
 * 
 * 设计原则：
 * - 线程安全：使用CopyOnWriteArrayList保证并发安全
 * - 自动发现：启动时自动识别本机节点
 * - 状态同步：维护所有节点的实时状态信息
 * - 配置驱动：通过配置文件读取集群节点列表
 */
@Component
public class Cluster {
    
    private static final Logger log = LoggerFactory.getLogger(Cluster.class);
    
    /**
     * 集群所有节点列表
     * 使用CopyOnWriteArrayList保证线程安全的读写操作
     */
    private final List<Server> servers = new CopyOnWriteArrayList<>();
    
    /**
     * 当前节点（本机节点）
     */
    private Server myself;
    
    /**
     * 当前Leader节点
     */
    private volatile Server leader;
    
    /**
     * 本机真实IP地址
     */
    private String localIpAddress;
    
    /**
     * 集群配置属性
     */
    @Autowired
    private MaRegistryConfigProperties configProperties;
    
    /**
     * 初始化集群
     * 
     * 在Spring Bean初始化完成后自动执行：
     * 1. 获取本机IP地址
     * 2. 从配置中读取集群节点列表
     * 3. 识别本机节点并设置为MYSELF
     * 4. 初始化所有节点状态
     */
    @PostConstruct
    public void init() {
        try {
            // 获取本机IP地址
            localIpAddress = getLocalIpAddress();
            log.info("本机IP地址: {}", localIpAddress);
            
            // 从配置中获取集群节点列表
            List<String> serverUrls = configProperties.getServerList();
            if (serverUrls == null || serverUrls.isEmpty()) {
                log.warn("未配置集群节点列表，使用单节点模式");
                return;
            }
            
            // 初始化集群节点
            initializeServers(serverUrls);
            
            log.info("集群初始化完成，共{}个节点", servers.size());
            for (Server server : servers) {
                log.info("节点: {} (本机: {}, Leader: {})", 
                        server.getUrl(), 
                        server.equals(myself), 
                        server.isLeader());
            }
            
        } catch (Exception e) {
            log.error("集群初始化失败", e);
        }
    }
    
    /**
     * 初始化集群服务器列表
     * 
     * @param serverUrls 服务器URL列表
     */
    private void initializeServers(List<String> serverUrls) {
        for (String url : serverUrls) {
            // 处理localhost/127.0.0.1替换为本机真实IP
            String processedUrl = processLocalhostUrl(url);
            
            Server server = new Server(processedUrl);
            
            // 识别本机节点
            if (server.isLocal(localIpAddress)) {
                server.setStatus(true);  // 本机节点始终在线
                myself = server;
                log.info("识别到本机节点: {}", processedUrl);
            }
            
            servers.add(server);
        }
        
        // 如果没有识别到本机节点，创建一个
        if (myself == null) {
            String myUrl = configProperties.getMyUrl();
            if (myUrl != null) {
                myUrl = processLocalhostUrl(myUrl);
                myself = new Server(myUrl, true, false, 0);
                servers.add(myself);
                log.info("创建本机节点: {}", myUrl);
            }
        }
    }
    
    /**
     * 处理localhost/127.0.0.1 URL
     * 
     * 将配置中的localhost或127.0.0.1替换为本机真实IP地址
     * 
     * @param url 原始URL
     * @return 处理后的URL
     */
    private String processLocalhostUrl(String url) {
        if (url == null) return null;
        
        if (url.contains("localhost") || url.contains("127.0.0.1")) {
            return url.replace("localhost", localIpAddress)
                     .replace("127.0.0.1", localIpAddress);
        }
        return url;
    }
    
    /**
     * 获取本机IP地址
     * 
     * @return 本机IP地址字符串
     * @throws UnknownHostException 当无法获取本机地址时抛出
     */
    private String getLocalIpAddress() throws UnknownHostException {
        InetAddress localhost = InetAddress.getLocalHost();
        return localhost.getHostAddress();
    }
    
    /**
     * 获取所有集群节点
     * 
     * @return 集群节点列表的副本
     */
    public List<Server> getServers() {
        return new ArrayList<>(servers);
    }
    
    /**
     * 获取本机节点
     * 
     * @return 本机Server实例
     */
    public Server getMyself() {
        return myself;
    }
    
    /**
     * 获取当前Leader节点
     * 
     * @return 当前Leader节点，如果没有则返回null
     */
    public Server getLeader() {
        return leader;
    }
    
    /**
     * 设置Leader节点
     * 
     * @param leaderServer 新的Leader节点
     */
    public void setLeader(Server leaderServer) {
        // 清除之前所有节点的Leader状态
        for (Server server : servers) {
            server.setLeader(false);
        }
        
        // 设置新的Leader
        if (leaderServer != null) {
            leaderServer.setLeader(true);
            this.leader = leaderServer;
            log.info("选举产生新的Leader: {}", leaderServer.getUrl());
        } else {
            this.leader = null;
            log.info("Leader节点已清除");
        }
    }
    
    /**
     * 根据URL查找节点
     * 
     * @param url 节点URL
     * @return 对应的Server实例，未找到则返回null
     */
    public Server getServerByUrl(String url) {
        if (url == null) return null;
        
        for (Server server : servers) {
            if (url.equals(server.getUrl())) {
                return server;
            }
        }
        return null;
    }
    
    /**
     * 更新节点状态
     * 
     * @param url 节点URL
     * @param status 新的状态
     */
    public void updateServerStatus(String url, boolean status) {
        Server server = getServerByUrl(url);
        if (server != null && server != myself) {
            server.setStatus(status);
            log.debug("更新节点状态: {} -> {}", url, status);
        }
    }
    
    /**
     * 获取在线节点列表
     * 
     * @return 所有状态为在线的节点列表
     */
    public List<Server> getOnlineServers() {
        List<Server> onlineServers = new ArrayList<>();
        for (Server server : servers) {
            if (server.isStatus()) {
                onlineServers.add(server);
            }
        }
        return onlineServers;
    }
    
    /**
     * 检查是否为Leader节点
     * 
     * @return 当前节点是否为Leader
     */
    public boolean isLeader() {
        return myself != null && myself.isLeader();
    }
    
    /**
     * 检查集群是否已初始化
     * 
     * @return 集群是否包含节点信息
     */
    public boolean isInitialized() {
        return !servers.isEmpty();
    }
    
    /**
     * 获取集群大小
     * 
     * @return 集群节点总数
     */
    public int size() {
        return servers.size();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Cluster{");
        sb.append("servers=[");
        
        for (int i = 0; i < servers.size(); i++) {
            if (i > 0) sb.append(", ");
            Server server = servers.get(i);
            sb.append(server.getUrl())
              .append("(status=").append(server.isStatus())
              .append(", leader=").append(server.isLeader())
              .append(")");
        }
        
        sb.append("], myself=").append(myself != null ? myself.getUrl() : "null");
        sb.append(", leader=").append(leader != null ? leader.getUrl() : "null");
        sb.append("}");
        
        return sb.toString();
    }
}