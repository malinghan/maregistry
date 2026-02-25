//
// ExceptionResponse.java
// 全局异常响应封装类
//
// 该类用于统一封装异常信息，提供标准化的错误响应格式
//

package com.malinghan.maregistry;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 异常响应封装类
 * 
 * 用于统一处理和返回系统异常信息，提供标准化的错误响应格式。
 * 包含错误码、错误消息、时间戳等关键信息。
 * 
 * 设计原则：
 * - 统一错误格式：所有异常响应保持一致的结构
 * - 详细错误信息：提供足够的调试信息
 * - 时间戳记录：便于问题追踪和分析
 * - 可扩展性：支持未来添加更多错误信息字段
 */
public class ExceptionResponse {
    
    /**
     * 错误码
     * 用于标识错误类型，便于客户端程序化处理
     */
    private int code;
    
    /**
     * 错误消息
     * 人类可读的错误描述信息
     */
    private String message;
    
    /**
     * 详细错误信息
     * 可选字段，提供更详细的错误上下文
     */
    private String details;
    
    /**
     * 错误发生时间戳
     * ISO 8601格式的时间字符串
     */
    private String timestamp;
    
    /**
     * 请求路径
     * 发生错误的API端点路径
     */
    private String path;
    
    /**
     * 默认构造函数
     */
    public ExceptionResponse() {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    
    /**
     * 带参数的构造函数
     * 
     * @param code 错误码
     * @param message 错误消息
     */
    public ExceptionResponse(int code, String message) {
        this();
        this.code = code;
        this.message = message;
    }
    
    /**
     * 完整参数构造函数
     * 
     * @param code 错误码
     * @param message 错误消息
     * @param details 详细信息
     * @param path 请求路径
     */
    public ExceptionResponse(int code, String message, String details, String path) {
        this(code, message);
        this.details = details;
        this.path = path;
    }
    
    // Getter和Setter方法
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    @Override
    public String toString() {
        return "ExceptionResponse{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", details='" + details + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}