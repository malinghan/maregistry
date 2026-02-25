//
// Server.java
// 集群节点模型类
//
// 该类表示注册中心集群中的一个节点，包含节点的基本信息和状态
//

package com.malinghan.maregistry.cluster;

/**
 * 集群节点模型类
 * 
 * 表示注册中心集群中的一个节点实例，包含节点的网络地址、状态信息和角色信息。
 * 用于集群管理和主从选举等场景。
 * 
 * 核心属性：
 * - url: 节点的访问地址
 * - status: 节点在线状态
 * - leader: 是否为Leader节点
 * - version: 节点的数据版本号
 * 
 * 设计原则：
 * - 不可变性：一旦创建，核心属性不应随意修改
 * - 状态一致性：status和leader状态应保持逻辑一致性
 * - 版本跟踪：通过version字段跟踪数据同步状态
 */
public class Server {
    
    /**
     * 节点URL地址
     * 格式：http://host:port
     * 例如：http://192.168.1.100:8081
     */
    private final String url;
    
    /**
     * 节点在线状态
     * true表示节点在线可用，false表示离线或不可达
     */
    private boolean status;
    
    /**
     * 是否为Leader节点
     * true表示该节点是当前集群的Leader
     */
    private boolean leader;
    
    /**
     * 节点数据版本号
     * 用于跟踪该节点数据的更新情况，支持数据同步
     */
    private long version;
    
    /**
     * 构造函数
     * 
     * @param url 节点URL地址
     */
    public Server(String url) {
        this.url = url;
        this.status = true;  // 默认认为节点在线
        this.leader = false; // 默认不是Leader
        this.version = 0;    // 初始版本号为0
    }
    
    /**
     * 完整构造函数
     * 
     * @param url 节点URL地址
     * @param status 节点状态
     * @param leader 是否为Leader
     * @param version 数据版本号
     */
    public Server(String url, boolean status, boolean leader, long version) {
        this.url = url;
        this.status = status;
        this.leader = leader;
        this.version = version;
    }
    
    // Getter方法
    public String getUrl() {
        return url;
    }
    
    public boolean isStatus() {
        return status;
    }
    
    public boolean isLeader() {
        return leader;
    }
    
    public long getVersion() {
        return version;
    }
    
    // Setter方法
    public void setStatus(boolean status) {
        this.status = status;
    }
    
    public void setLeader(boolean leader) {
        this.leader = leader;
    }
    
    public void setVersion(long version) {
        this.version = version;
    }
    
    /**
     * 获取主机地址（从URL中解析）
     * 
     * @return 主机地址部分，例如：192.168.1.100
     */
    public String getHost() {
        if (url == null) return null;
        
        // 解析URL格式：http://host:port
        int protocolEnd = url.indexOf("://");
        if (protocolEnd == -1) return url;
        
        int hostStart = protocolEnd + 3;
        int portStart = url.indexOf(":", hostStart);
        
        if (portStart == -1) {
            return url.substring(hostStart);
        } else {
            return url.substring(hostStart, portStart);
        }
    }
    
    /**
     * 获取端口号（从URL中解析）
     * 
     * @return 端口号，例如：8081
     */
    public Integer getPort() {
        if (url == null) return null;
        
        int portStart = url.lastIndexOf(":");
        if (portStart == -1) return null;
        
        try {
            String portStr = url.substring(portStart + 1);
            // 移除可能的路径部分
            int pathStart = portStr.indexOf("/");
            if (pathStart != -1) {
                portStr = portStr.substring(0, pathStart);
            }
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 检查是否为本机节点
     * 
     * 通过比较主机地址判断是否为当前运行的节点
     * 
     * @param localHost 本机主机地址
     * @return 是否为本机节点
     */
    public boolean isLocal(String localHost) {
        if (localHost == null || url == null) return false;
        
        String host = getHost();
        if (host == null) return false;
        
        // 处理localhost和127.0.0.1的特殊情况
        if ("localhost".equals(localHost) || "127.0.0.1".equals(localHost)) {
            return "localhost".equals(host) || "127.0.0.1".equals(host);
        }
        
        return host.equals(localHost);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Server server = (Server) obj;
        return url != null ? url.equals(server.url) : server.url == null;
    }
    
    @Override
    public int hashCode() {
        return url != null ? url.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "Server{" +
                "url='" + url + '\'' +
                ", status=" + status +
                ", leader=" + leader +
                ", version=" + version +
                '}';
    }
}