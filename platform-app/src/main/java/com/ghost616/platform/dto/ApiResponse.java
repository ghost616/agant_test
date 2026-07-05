package com.ghost616.platform.dto;


import com.ghost616.agentbase.enums.ErrorCode;


/**
 * 统一 API 响应体。
 *
 * @param <T> 响应数据类型
 */
public class ApiResponse<T> {

    private String code;
    private boolean success;
    private String message;
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(String code, boolean success, String message, T data) {
        this.code = code;
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /**
     * 构建成功响应（仅数据）。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SYS-000", true, "操作成功", data);
    }

    /**
     * 构建成功响应（含自定义消息）。
     *
     * @param msg  消息
     * @param data 响应数据
     * @param <T>  数据类型
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success(String msg, T data) {
        return new ApiResponse<>("SYS-000", true, msg, data);
    }

    /**
     * 构建失败响应。
     *
     * @param code 错误码
     * @param msg  错误消息
     * @param <T>  数据类型
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> fail(String code, String msg) {
        return new ApiResponse<>(code, false, msg, null);
    }

    /**
     * 通过 ErrorCode 构建失败响应。
     *
     * @param errorCode 错误码枚举
     * @param <T>       数据类型
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), false, errorCode.getMessage(), null);
    }

    /**
     * 通过 ErrorCode 构建失败响应（含详细信息）。
     *
     * @param errorCode 错误码枚举
     * @param detail    详细信息
     * @param <T>       数据类型
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String detail) {
        return new ApiResponse<>(errorCode.getCode(), false, detail, null);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
