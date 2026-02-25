//
// MaRegistryController.java
// 服务注册中心REST API控制器
//
// 该类提供了服务注册中心的HTTP接口，对外暴露服务注册、注销和查询功能
// 使用Spring MVC框架实现RESTful风格的API
//

package com.malinghan.maregistry;

import com.malinghan.maregistry.model.InstanceMeta;
import com.malinghan.maregistry.service.RegistryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 服务注册中心REST控制器
 * 
 * 该控制器类负责处理服务注册中心的HTTP请求，提供RESTful API接口。
 * 通过依赖注入的方式使用RegistryService实现具体的业务逻辑。
 * 
 * API设计原则：
 * - 使用标准HTTP方法（GET、POST）
 * - 路径参数传递服务名称
 * - 请求体传递实例元数据
 * - 返回JSON格式的响应数据
 * 
 * 错误处理：通过全局异常处理器统一处理异常情况
 */
@RestController
public class MaRegistryController {

    /**
     * 服务注册业务逻辑接口
     * 
     * 通过构造函数注入，实现控制层与业务层的解耦。
     * 支持依赖注入和单元测试mock。
     */
    private final RegistryService registryService;

    /**
     * 构造函数注入RegistryService依赖
     * 
     * @param registryService 服务注册业务接口实现
     */
    public MaRegistryController(RegistryService registryService) {
        this.registryService = registryService;
    }

    /**
     * 注册服务实例接口
     * 
     * 接收服务实例注册请求，将实例信息保存到注册中心。
     * 
     * @param service 服务名称，通过请求参数传递
     *                例如："com.example.UserService"
     * @param instance 服务实例元数据，通过请求体传递JSON格式
     *                包含：scheme、host、port、context、parameters等信息
     * @return 注册成功的实例信息
     * 
     * 请求示例：
     * POST /reg?service=com.example.UserService
     * Content-Type: application/json
     * {
     *   "scheme": "http",
     *   "host": "192.168.1.100",
     *   "port": 8080,
     *   "context": "userservice",
     *   "parameters": {
     *     "env": "prod",
     *     "version": "1.0.0"
     *   }
     * }
     * 
     * 响应示例：
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * {
     *   "scheme": "http",
     *   "host": "192.168.1.100",
     *   "port": 8080,
     *   "context": "userservice",
     *   "parameters": {
     *     "env": "prod",
     *     "version": "1.0.0"
     *   }
     * }
     */
    @PostMapping("/reg")
    public InstanceMeta register(@RequestParam String service, @RequestBody InstanceMeta instance) {
        return registryService.register(service, instance);
    }

    /**
     * 注销服务实例接口
     * 
     * 从注册中心移除指定的服务实例。
     * 
     * @param service 服务名称
     * @param instance 要注销的服务实例信息
     * @return 被注销的实例信息
     * 
     * 请求示例：
     * POST /unreg?service=com.example.UserService
     * Content-Type: application/json
     * {
     *   "scheme": "http",
     *   "host": "192.168.1.100",
     *   "port": 8080,
     *   "context": "userservice"
     * }
     * 
     * 注意：只需要提供能够唯一标识实例的信息（scheme+host+port+context）
     */
    @PostMapping("/unreg")
    public InstanceMeta unregister(@RequestParam String service, @RequestBody InstanceMeta instance) {
        return registryService.unregister(service, instance);
    }

    /**
     * 查询服务所有实例接口
     * 
     * 获取指定服务名称下所有已注册的实例列表。
     * 
     * @param service 服务名称
     * @return 该服务的所有实例列表，如果服务不存在则返回空列表
     * 
     * 请求示例：
     * GET /findAll?service=com.example.UserService
     * 
     * 响应示例：
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * [
     *   {
     *     "scheme": "http",
     *     "host": "192.168.1.100",
     *     "port": 8080,
     *     "context": "userservice",
     *     "parameters": {}
     *   },
     *   {
     *     "scheme": "http",
     *     "host": "192.168.1.101",
     *     "port": 8080,
     *     "context": "userservice",
     *     "parameters": {}
     *   }
     * ]
     * 
     * 使用场景：
     * - 服务消费者进行负载均衡
     * - 监控系统获取服务实例状态
     * - 配置管理系统查询服务拓扑
     */
    @GetMapping("/findAll")
    public List<InstanceMeta> findAll(@RequestParam String service) {
        return registryService.getAllInstances(service);
    }

    /**
     * 心跳续约单个服务接口
     * 
     * 客户端定期调用此接口报告服务实例仍然存活。
     * 
     * @param service 服务名称
     * @param instance 服务实例信息
     * @return 续约确认信息
     * 
     * 请求示例：
     * POST /renew?service=com.example.UserService
     * Content-Type: application/json
     * {
     *   "scheme": "http",
     *   "host": "192.168.1.100",
     *   "port": 8080,
     *   "context": "userservice"
     * }
     */
    @PostMapping("/renew")
    public InstanceMeta renew(@RequestParam String service, @RequestBody InstanceMeta instance) {
        return registryService.renew(service, instance);
    }

    /**
     * 批量心跳续约多个服务接口
     * 
     * 客户端可以一次性报告多个服务的存活状态。
     * 
     * @param services 逗号分隔的服务名称列表
     * @param instance 服务实例信息
     * @return 续约确认信息
     * 
     * 请求示例：
     * POST /renews?services=com.example.UserService,com.example.OrderService
     * Content-Type: application/json
     * {
     *   "scheme": "http",
     *   "host": "192.168.1.100",
     *   "port": 8080,
     *   "context": "userservice"
     * }
     */
    @PostMapping("/renews")
    public InstanceMeta renews(@RequestParam String services, @RequestBody InstanceMeta instance) {
        String[] serviceArray = services.split(",");
        return registryService.renews(serviceArray, instance);
    }

    /**
     * 获取单个服务版本号接口
     * 
     * 客户端可以通过此接口获取服务的当前版本号，用于判断是否需要更新实例列表。
     * 
     * @param service 服务名称
     * @return 服务版本号
     * 
     * 请求示例：
     * POST /version?service=com.example.UserService
     * 
     * 响应示例：
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * 15
     */
    @PostMapping("/version")
    public Long version(@RequestParam String service) {
        return registryService.version(service);
    }

    /**
     * 批量获取多个服务版本号接口
     * 
     * 客户端可以一次性获取多个服务的版本号信息。
     * 
     * @param services 逗号分隔的服务名称列表
     * @return 服务名称到版本号的映射
     * 
     * 请求示例：
     * POST /versions?services=com.example.UserService,com.example.OrderService
     * 
     * 响应示例：
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * {
     *   "com.example.UserService": 15,
     *   "com.example.OrderService": 8
     * }
     */
    @PostMapping("/versions")
    public Map<String, Long> versions(@RequestParam String services) {
        String[] serviceArray = services.split(",");
        return registryService.versions(serviceArray);
    }
}