//
// MaRegistryConfig.java
// 服务注册中心配置类
//
// 该类负责配置和管理服务注册中心的各种组件和Bean
//

package com.malinghan.maregistry;

import com.malinghan.maregistry.cluster.MaRegistryConfigProperties;
import com.malinghan.maregistry.health.HealthChecker;
import com.malinghan.maregistry.health.MaHealthChecker;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 服务注册中心配置类
 * 
 * 负责配置和管理服务注册中心的核心组件，包括：
 * - 健康检查器的生命周期管理
 * - 各种服务Bean的配置
 * - 系统级配置参数
 * 
 * 设计原则：
 * - 使用@Bean注解明确声明组件依赖关系
 * - 通过initMethod/destroyMethod管理Bean生命周期
 * - 集中管理配置参数和系统设置
 */
@Configuration
@EnableConfigurationProperties(MaRegistryConfigProperties.class)
public class MaRegistryConfig {
    
    /**
     * 配置健康检查器Bean
     * 
     * 使用initMethod和destroyMethod自动管理健康检查器的生命周期：
     * - 应用启动时自动调用start()方法
     * - 应用关闭时自动调用stop()方法
     * 
     * @return HealthChecker实例
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public HealthChecker healthChecker() {
        return new MaHealthChecker();
    }
}