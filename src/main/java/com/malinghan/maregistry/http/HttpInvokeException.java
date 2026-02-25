//
// HttpInvokeException.java
// HTTP调用异常类
//
// 该类用于封装HTTP调用过程中发生的各种异常情况
//

package com.malinghan.maregistry.http;

/**
 * HTTP调用异常类
 * 
 * 封装HTTP调用过程中可能发生的各种异常情况，提供统一的错误处理机制。
 * 包含详细的错误信息，便于问题诊断和日志记录。
 * 
 * 异常类型：
 * - 网络连接异常
 * - 超时异常
 * - HTTP状态码异常
 * - 数据格式异常
 * 
 * 设计原则：
 * - 继承RuntimeException：避免强制异常处理
 * - 包含详细上下文信息：URL、HTTP方法、状态码等
 * - 支持链式异常：保留原始异常信息
 */
public class HttpInvokeException extends RuntimeException {
    
    /**
     * 请求URL
     */
    private final String url;
    
    /**
     * HTTP方法
     */
    private final String method;
    
    /**
     * HTTP状态码
     */
    private final Integer statusCode;
    
    /**
     * 响应体内容
     */
    private final String responseBody;
    
    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param url 请求URL
     * @param method HTTP方法
     */
    public HttpInvokeException(String message, String url, String method) {
        super(message);
        this.url = url;
        this.method = method;
        this.statusCode = null;
        this.responseBody = null;
    }
    
    /**
     * 构造函数（包含状态码）
     * 
     * @param message 错误消息
     * @param url 请求URL
     * @param method HTTP方法
     * @param statusCode HTTP状态码
     */
    public HttpInvokeException(String message, String url, String method, Integer statusCode) {
        super(message);
        this.url = url;
        this.method = method;
        this.statusCode = statusCode;
        this.responseBody = null;
    }
    
    /**
     * 构造函数（包含状态码和响应体）
     * 
     * @param message 错误消息
     * @param url 请求URL
     * @param method HTTP方法
     * @param statusCode HTTP状态码
     * @param responseBody 响应体内容
     */
    public HttpInvokeException(String message, String url, String method, 
                              Integer statusCode, String responseBody) {
        super(message);
        this.url = url;
        this.method = method;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
    
    /**
     * 构造函数（包含原因异常）
     * 
     * @param message 错误消息
     * @param cause 原因异常
     * @param url 请求URL
     * @param method HTTP方法
     */
    public HttpInvokeException(String message, Throwable cause, String url, String method) {
        super(message, cause);
        this.url = url;
        this.method = method;
        this.statusCode = null;
        this.responseBody = null;
    }
    
    // Getter方法
    public String getUrl() {
        return url;
    }
    
    public String getMethod() {
        return method;
    }
    
    public Integer getStatusCode() {
        return statusCode;
    }
    
    public String getResponseBody() {
        return responseBody;
    }
    
    /**
     * 获取完整的错误信息
     * 
     * @return 包含所有上下文信息的错误描述
     */
    public String getFullMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP调用失败: ").append(getMessage());
        sb.append(" [").append(method).append(" ").append(url).append("]");
        
        if (statusCode != null) {
            sb.append(" 状态码: ").append(statusCode);
        }
        
        if (responseBody != null && !responseBody.isEmpty()) {
            sb.append(" 响应体: ").append(responseBody);
        }
        
        if (getCause() != null) {
            sb.append(" 原因: ").append(getCause().getMessage());
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return getFullMessage();
    }
}