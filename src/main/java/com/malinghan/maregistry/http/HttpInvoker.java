//
// HttpInvoker.java
// HTTP调用接口
//
// 该接口定义了集群节点间HTTP通信的标准契约
//

package com.malinghan.maregistry.http;

import java.util.Map;

/**
 * HTTP调用接口
 * 
 * 定义集群节点间HTTP通信的标准接口，用于服务注册中心集群内部的数据同步。
 * 支持GET、POST等HTTP方法，提供连接池管理和超时控制。
 * 
 * 设计原则：
 * - 接口抽象：隐藏具体HTTP客户端实现细节
 * - 连接复用：通过连接池提高通信效率
 * - 超时控制：防止长时间等待影响系统响应
 * - 异常处理：统一的错误处理机制
 */
public interface HttpInvoker {
    
    /**
     * HTTP GET请求
     * 
     * 发送GET请求到指定URL，不带请求体。
     * 
     * @param url 目标URL
     * @return 响应内容字符串
     * @throws HttpInvokeException 当请求失败时抛出
     * 
     * 使用场景：
     * - 查询集群节点状态
     * - 获取快照数据
     * - 健康检查探活
     */
    String get(String url) throws HttpInvokeException;
    
    /**
     * HTTP GET请求（带查询参数）
     * 
     * 发送GET请求到指定URL，携带查询参数。
     * 
     * @param url 基础URL
     * @param params 查询参数映射
     * @return 响应内容字符串
     * @throws HttpInvokeException 当请求失败时抛出
     * 
     * 示例：
     * get("http://localhost:8081/info", Map.of("detail", "true"))
     */
    String get(String url, Map<String, String> params) throws HttpInvokeException;
    
    /**
     * HTTP POST请求（发送JSON数据）
     * 
     * 发送POST请求到指定URL，请求体为JSON格式数据。
     * 
     * @param url 目标URL
     * @param json 请求体JSON字符串
     * @return 响应内容字符串
     * @throws HttpInvokeException 当请求失败时抛出
     * 
     * 使用场景：
     * - 发送快照数据
     * - 同步注册信息
     * - 集群状态更新
     */
    String post(String url, String json) throws HttpInvokeException;
    
    /**
     * HTTP POST请求（发送JSON数据，带查询参数）
     * 
     * 发送POST请求到指定URL，携带查询参数和JSON请求体。
     * 
     * @param url 基础URL
     * @param params 查询参数映射
     * @param json 请求体JSON字符串
     * @return 响应内容字符串
     * @throws HttpInvokeException 当请求失败时抛出
     */
    String post(String url, Map<String, String> params, String json) throws HttpInvokeException;
    
    /**
     * 获取默认的HTTP调用器实例
     * 
     * 返回预配置的默认实现，包含连接池和超时设置。
     * 
     * @return 默认HttpInvoker实例
     * 
     * 配置详情：
     * - 连接池大小：16个连接
     * - 连接超时：500ms
     * - 读取超时：500ms
     * - 写入超时：500ms
     */
    static HttpInvoker getDefault() {
        return OkHttpInvoker.getInstance();
    }
}