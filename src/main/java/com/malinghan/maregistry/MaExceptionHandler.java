//
// MaExceptionHandler.java
// 全局异常处理器
//
// 该类负责统一处理系统中抛出的各种异常，提供标准化的错误响应
//

package com.malinghan.maregistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 * 
 * 使用@ControllerAdvice注解捕获整个应用中的异常，
 * 提供统一的异常处理和错误响应格式。
 * 
 * 处理的异常类型：
 * - 运行时异常(RuntimeException)
 * - 非法参数异常(IllegalArgumentException)
 * - 空指针异常(NullPointerException)
 * - 通用异常(Exception)
 * 
 * 设计原则：
 * - 统一错误响应格式
 * - 详细的日志记录
 * - 适当的HTTP状态码返回
 * - 安全的错误信息暴露（避免敏感信息泄露）
 */
@ControllerAdvice
public class MaExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(MaExceptionHandler.class);
    
    /**
     * 处理运行时异常
     * 
     * @param ex 运行时异常
     * @param request HTTP请求
     * @return 错误响应实体
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ExceptionResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        
        log.error("运行时异常: {}", ex.getMessage(), ex);
        
        ExceptionResponse response = new ExceptionResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "系统内部错误",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 处理非法参数异常
     * 
     * @param ex 非法参数异常
     * @param request HTTP请求
     * @return 错误响应实体
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        log.warn("非法参数异常: {}", ex.getMessage());
        
        ExceptionResponse response = new ExceptionResponse(
            HttpStatus.BAD_REQUEST.value(),
            "请求参数错误",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理空指针异常
     * 
     * @param ex 空指针异常
     * @param request HTTP请求
     * @return 错误响应实体
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ExceptionResponse> handleNullPointerException(
            NullPointerException ex, HttpServletRequest request) {
        
        log.error("空指针异常: {}", ex.getMessage(), ex);
        
        ExceptionResponse response = new ExceptionResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "系统内部错误",
            "数据访问异常，请稍后重试",
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 处理服务不可用异常
     * 
     * @param ex 服务不可用异常
     * @param request HTTP请求
     * @return 错误响应实体
     */
    @ExceptionHandler(java.util.concurrent.RejectedExecutionException.class)
    public ResponseEntity<ExceptionResponse> handleRejectedExecutionException(
            java.util.concurrent.RejectedExecutionException ex, HttpServletRequest request) {
        
        log.error("服务不可用异常: {}", ex.getMessage(), ex);
        
        ExceptionResponse response = new ExceptionResponse(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "服务暂时不可用",
            "系统繁忙，请稍后重试",
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    /**
     * 处理通用异常
     * 
     * 作为兜底异常处理器，处理所有未被特定处理器捕获的异常。
     * 
     * @param ex 通用异常
     * @param request HTTP请求
     * @return 错误响应实体
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        log.error("未处理的异常: {}", ex.getMessage(), ex);
        
        ExceptionResponse response = new ExceptionResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "系统错误",
            "未知错误，请联系系统管理员",
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 处理健康检查相关异常
     * 
     * 专门处理健康检查过程中可能发生的异常。
     * 
     * @param ex 健康检查异常
     * @param request HTTP请求
     * @return 错误响应实体
     */
    @ExceptionHandler(HealthCheckException.class)
    public ResponseEntity<ExceptionResponse> handleHealthCheckException(
            HealthCheckException ex, HttpServletRequest request) {
        
        log.warn("健康检查异常: {}", ex.getMessage());
        
        ExceptionResponse response = new ExceptionResponse(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "健康检查失败",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    /**
     * 健康检查异常类
     * 
     * 专门用于健康检查过程中抛出的异常。
     */
    public static class HealthCheckException extends RuntimeException {
        public HealthCheckException(String message) {
            super(message);
        }
        
        public HealthCheckException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}