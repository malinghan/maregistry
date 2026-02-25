//
// MaregistryApplication.java
// maregistry服务注册中心 - Spring Boot启动类
//
// 项目描述：这是一个轻量级的分布式服务注册中心，基于Spring Boot实现
// 主要功能：服务注册、服务发现、健康检查等微服务治理核心功能
//
// 技术栈：
// - Java 17
// - Spring Boot 3.2.5
// - Lombok简化代码
//
// 作者：malinghan
// 创建时间：2026年
//

package com.malinghan.maregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot应用程序启动类
 * 
 * 该类是整个maregistry服务注册中心的入口点，负责启动Spring Boot应用上下文。
 * 使用@SpringBootApplication注解启用自动配置、组件扫描和Spring Boot的其他特性。
 * 
 * 功能说明：
 * - 自动扫描并注册所有@Component、@Service、@Repository、@Controller等注解的组件
 * - 启用Spring Boot自动配置机制
 * - 启动内嵌的Web服务器（默认Tomcat）
 * - 加载application.properties/yml配置文件
 */
@SpringBootApplication
public class MaregistryApplication {

    /**
     * 应用程序主入口方法
     * 
     * @param args 命令行参数
     * 
     * 启动流程：
     * 1. 创建SpringApplication实例
     * 2. 执行应用上下文初始化
     * 3. 启动内嵌Web服务器
     * 4. 应用进入就绪状态，开始监听HTTP请求
     * 
     * 启动示例：
     * java -jar maregistry-0.0.1-SNAPSHOT.jar --server.port=8080
     */
    public static void main(String[] args) {
        SpringApplication.run(MaregistryApplication.class, args);
    }

}
