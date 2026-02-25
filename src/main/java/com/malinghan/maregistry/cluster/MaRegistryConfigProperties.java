//
// MaRegistryConfigProperties.java
// 服务注册中心配置属性类
//
// 该类用于读取和管理服务注册中心的配置属性
//

package com.malinghan.maregistry.cluster;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 服务注册中心配置属性类
 * 
 * 通过@ConfigurationProperties注解自动绑定配置文件中的属性。
 * 支持从application.properties或application.yml中读取配置。
 * 
 * 配置示例：
 * maregistry.server-list[0]=http://192.168.1.100:8081
 * maregistry.server-list[1]=http://192.168.1.101:8081
 * maregistry.my-url=http://192.168.1.100:8081
 * 
 * 设计原则：
 * - 配置外部化：将配置从代码中分离
 * - 类型安全：使用明确的数据类型
 * - 默认值支持：为重要配置提供合理默认值
 * - 易于扩展：支持未来添加新的配置项
 */
@Component
@Primary
@ConfigurationProperties(prefix = "maregistry")
public class MaRegistryConfigProperties {
    
    /**
     * 集群节点列表
     * 格式：http://host:port
     * 例如：["http://192.168.1.100:8081", "http://192.168.1.101:8081"]
     */
    private List<String> serverList;
    
    /**
     * 当前节点URL
     * 用于明确指定本机节点地址
     * 格式：http://host:port
     */
    private String myUrl;
    
    /**
     * 集群模式开关
     * true表示启用集群模式，false表示单机模式
     * 默认值：true
     */
    private boolean clusterMode = true;
    
    /**
     * 心跳检查间隔（毫秒）
     * 默认值：5000ms（5秒）
     */
    private int heartbeatInterval = 5000;
    
    /**
     * 节点超时时间（毫秒）
     * 超过此时间未收到心跳认为节点离线
     * 默认值：15000ms（15秒）
     */
    private int nodeTimeout = 15000;
    
    // Getter和Setter方法
    public List<String> getServerList() {
        return serverList;
    }
    
    public void setServerList(List<String> serverList) {
        this.serverList = serverList;
    }
    
    public String getMyUrl() {
        return myUrl;
    }
    
    public void setMyUrl(String myUrl) {
        this.myUrl = myUrl;
    }
    
    public boolean isClusterMode() {
        return clusterMode;
    }
    
    public void setClusterMode(boolean clusterMode) {
        this.clusterMode = clusterMode;
    }
    
    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }
    
    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }
    
    public int getNodeTimeout() {
        return nodeTimeout;
    }
    
    public void setNodeTimeout(int nodeTimeout) {
        this.nodeTimeout = nodeTimeout;
    }
    
    /**
     * 验证配置的有效性
     * 
     * @return 配置是否有效
     */
    public boolean isValid() {
        // 集群模式下必须配置节点列表
        if (clusterMode && (serverList == null || serverList.isEmpty())) {
            return false;
        }
        
        // 心跳间隔必须大于0
        if (heartbeatInterval <= 0) {
            return false;
        }
        
        // 超时时间必须大于心跳间隔
        if (nodeTimeout <= heartbeatInterval) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取配置摘要信息
     * 
     * @return 配置信息摘要
     */
    public String getConfigSummary() {
        return String.format("配置信息 - 集群模式: %s, 节点数: %d, 心跳间隔: %dms, 超时时间: %dms",
                clusterMode,
                serverList != null ? serverList.size() : 0,
                heartbeatInterval,
                nodeTimeout);
    }
    
    @Override
    public String toString() {
        return "MaRegistryConfigProperties{" +
                "serverList=" + serverList +
                ", myUrl='" + myUrl + '\'' +
                ", clusterMode=" + clusterMode +
                ", heartbeatInterval=" + heartbeatInterval +
                ", nodeTimeout=" + nodeTimeout +
                '}';
    }
}