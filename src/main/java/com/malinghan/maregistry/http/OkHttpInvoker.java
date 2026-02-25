//
// OkHttpInvoker.java
// OkHttp实现的HTTP调用器
//
// 该类使用OkHttp库实现HttpInvoker接口，提供高效的HTTP通信能力
//

package com.malinghan.maregistry.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp实现的HTTP调用器
 * 
 * 基于OkHttp库实现的HttpInvoker接口，提供高性能的HTTP通信能力。
 * 支持连接池、超时控制、JSON序列化等功能。
 * 
 * 核心特性：
 * - 连接池复用：默认16个连接，提高通信效率
 * - 超时控制：连接、读取、写入超时均为500ms
 * - JSON支持：内置Jackson ObjectMapper处理JSON数据
 * - 单例模式：全局共享一个实例
 * 
 * 使用场景：
 * - 集群节点间数据同步
 * - 快照传输
 * - 健康检查探活
 * - 状态信息交换
 */
public class OkHttpInvoker implements HttpInvoker {
    
    private static final Logger log = LoggerFactory.getLogger(OkHttpInvoker.class);
    
    /**
     * 默认连接池大小
     */
    private static final int DEFAULT_POOL_SIZE = 16;
    
    /**
     * 默认超时时间（毫秒）
     */
    private static final int DEFAULT_TIMEOUT_MS = 500;
    
    /**
     * 单例实例
     */
    private static volatile OkHttpInvoker instance;
    
    /**
     * OkHttp客户端
     */
    private final OkHttpClient client;
    
    /**
     * JSON序列化工具
     */
    private final ObjectMapper objectMapper;
    
    /**
     * 私有构造函数
     */
    private OkHttpInvoker() {
        this(DEFAULT_POOL_SIZE, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * 带参数的构造函数
     * 
     * @param poolSize 连接池大小
     * @param timeoutMs 超时时间（毫秒）
     */
    private OkHttpInvoker(int poolSize, int timeoutMs) {
        // 初始化OkHttpClient
        this.client = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(poolSize, 5, TimeUnit.MINUTES))
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();
        
        // 初始化ObjectMapper
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 获取单例实例
     * 
     * @return OkHttpInvoker单例
     */
    public static OkHttpInvoker getInstance() {
        if (instance == null) {
            synchronized (OkHttpInvoker.class) {
                if (instance == null) {
                    instance = new OkHttpInvoker();
                }
            }
        }
        return instance;
    }
    
    /**
     * 获取自定义配置的实例
     * 
     * @param poolSize 连接池大小
     * @param timeoutMs 超时时间
     * @return OkHttpInvoker实例
     */
    public static OkHttpInvoker getInstance(int poolSize, int timeoutMs) {
        return new OkHttpInvoker(poolSize, timeoutMs);
    }
    
    @Override
    public String get(String url) throws HttpInvokeException {
        return get(url, null);
    }
    
    @Override
    public String get(String url, Map<String, String> params) throws HttpInvokeException {
        try {
            // 构建请求URL
            HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
            if (params != null) {
                params.forEach(urlBuilder::addQueryParameter);
            }
            
            String finalUrl = urlBuilder.build().toString();
            
            // 构建请求
            Request request = new Request.Builder()
                    .url(finalUrl)
                    .get()
                    .build();
            
            log.debug("发送GET请求: {}", finalUrl);
            
            // 执行请求
            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? 
                    response.body().string() : "";
                
                if (!response.isSuccessful()) {
                    throw new HttpInvokeException(
                        "HTTP GET请求失败",
                        finalUrl,
                        "GET",
                        response.code(),
                        responseBody
                    );
                }
                
                log.debug("GET请求成功: {} 状态码: {}", finalUrl, response.code());
                return responseBody;
            }
            
        } catch (IOException e) {
            log.error("GET请求IO异常: {}", url, e);
            throw new HttpInvokeException(
                "网络IO异常: " + e.getMessage(),
                e,
                url,
                "GET"
            );
        } catch (Exception e) {
            log.error("GET请求未知异常: {}", url, e);
            throw new HttpInvokeException(
                "未知异常: " + e.getMessage(),
                e,
                url,
                "GET"
            );
        }
    }
    
    @Override
    public String post(String url, String json) throws HttpInvokeException {
        return post(url, null, json);
    }
    
    @Override
    public String post(String url, Map<String, String> params, String json) throws HttpInvokeException {
        try {
            // 构建请求URL
            HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
            if (params != null) {
                params.forEach(urlBuilder::addQueryParameter);
            }
            
            String finalUrl = urlBuilder.build().toString();
            
            // 构建请求体
            RequestBody requestBody = RequestBody.create(
                json, MediaType.get("application/json; charset=utf-8")
            );
            
            // 构建请求
            Request request = new Request.Builder()
                    .url(finalUrl)
                    .post(requestBody)
                    .build();
            
            log.debug("发送POST请求: {} 数据长度: {}", finalUrl, json.length());
            
            // 执行请求
            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? 
                    response.body().string() : "";
                
                if (!response.isSuccessful()) {
                    throw new HttpInvokeException(
                        "HTTP POST请求失败",
                        finalUrl,
                        "POST",
                        response.code(),
                        responseBody
                    );
                }
                
                log.debug("POST请求成功: {} 状态码: {}", finalUrl, response.code());
                return responseBody;
            }
            
        } catch (IOException e) {
            log.error("POST请求IO异常: {}", url, e);
            throw new HttpInvokeException(
                "网络IO异常: " + e.getMessage(),
                e,
                url,
                "POST"
            );
        } catch (Exception e) {
            log.error("POST请求未知异常: {}", url, e);
            throw new HttpInvokeException(
                "未知异常: " + e.getMessage(),
                e,
                url,
                "POST"
            );
        }
    }
    
    /**
     * 关闭HTTP客户端
     * 
     * 清理连接池和相关资源。
     */
    public void close() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }
}