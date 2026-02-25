//
// InstanceMeta.java
// 服务实例元数据模型
//
// 该类用于表示一个服务实例的完整信息，包括网络地址、协议、上下文路径等
// 是服务注册中心的核心数据模型之一
//

package com.malinghan.maregistry.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务实例元数据模型类
 * 
 * 该类封装了一个服务实例的所有必要信息，用于在注册中心中标识和管理服务实例。
 * 使用Lombok注解简化getter/setter等样板代码的生成。
 * 
 * 核心设计原则：
 * - 使用scheme+host+port+context四个字段作为唯一标识
 * - parameters字段用于存储扩展信息，不参与相等性比较
 * - 提供便捷的URL生成方法
 */
@Data
@EqualsAndHashCode(of = {"scheme", "host", "port", "context"})
public class InstanceMeta {

    /**
     * 通信协议方案，如"http"、"https"
     * 例如："http"表示使用HTTP协议
     */
    private String scheme;
    
    /**
     * 主机地址，可以是IP地址或域名
     * 例如："192.168.1.100" 或 "service.example.com"
     */
    private String host;
    
    /**
     * 端口号
     * 例如：8080、9090等
     */
    private Integer port;
    
    /**
     * 上下文路径，服务部署的根路径
     * 例如："api"、"userservice"等
     */
    private String context;
    
    /**
     * 自定义参数映射，用于存储额外的元数据信息
     * 例如：环境标识、版本号、标签等
     * 注意：此字段不参与对象相等性比较
     */
    private Map<String, String> parameters = new HashMap<>();

    /**
     * 构造函数，创建一个新的服务实例元数据对象
     * 
     * @param scheme 通信协议方案
     * @param host 主机地址
     * @param port 端口号
     * @param context 上下文路径
     */
    public InstanceMeta(String scheme, String host, Integer port, String context) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.context = context;
    }

    /**
     * 将服务实例信息转换为标准URL格式
     * 
     * @return 格式为 scheme://host:port/context 的URL字符串
     * 例如："http://192.168.1.100:8080/userservice"
     */
    public String toUrl() {
        return scheme + "://" + host + ":" + port + "/" + context;
    }

    /**
     * 创建HTTP协议的服务实例元数据的便捷方法
     * 
     * @param host 主机地址
     * @param port 端口号
     * @return HTTP协议的InstanceMeta实例，context为空字符串
     * 
     * 使用示例：
     * InstanceMeta instance = InstanceMeta.http("localhost", 8080);
     */
    public static InstanceMeta http(String host, Integer port) {
        return new InstanceMeta("http", host, port, "");
    }
}