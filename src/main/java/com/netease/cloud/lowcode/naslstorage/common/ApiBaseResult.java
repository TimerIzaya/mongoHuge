package com.netease.cloud.lowcode.naslstorage.common;

/**
 * OpenAPI通用返回
 */
public class ApiBaseResult {
    /**
     * 错误码
     */
    private int code;
    /**
     * 英文错误信息
     */
    private String msg;

    /**
     * 数据，取名应该跟继承此类的其他变量不会冲突，修改此变量名应该同时修改：GlobalAPIHandler 中的参数字符串
     */
    private Object result;

    public ApiBaseResult() {
    }

    public ApiBaseResult(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public ApiBaseResult(ApiErrorCode apiErrorCode) {
        this.code = apiErrorCode.getStatusCode();
        this.msg = apiErrorCode.getZnMessage();
    }

    public ApiBaseResult(Integer code, String msg, Object result) {
        this.code = code;
        this.msg = msg;
        this.result = result;
    }

    public ApiBaseResult(Integer code, String msg, String result) {
        this.code = code;
        this.msg = msg;
        this.result = result;
    }

    public static ApiBaseResult successRet() {
        return new ApiBaseResult(ApiErrorCode.SUCCESS);
    }

    public static ApiBaseResult successRet(Object data) {
        ApiBaseResult baseResult = new ApiBaseResult(ApiErrorCode.SUCCESS);
        baseResult.result = data;
        return baseResult;
    }

    public static ApiBaseResult errorOf(ApiErrorCode apiErrorCode, Object... args) {
        String znMessage = String.format(apiErrorCode.getZnMessage(), args);
        return new ApiBaseResult(apiErrorCode.getStatusCode(), znMessage);
    }

    public static ApiBaseResult errorOf(Integer code, String msg) {
        return new ApiBaseResult(code, msg);
    }

    public boolean isSuccess() {
        return ApiErrorCode.SUCCESS.getStatusCode() == code;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    @SuppressWarnings("unchecked")
    public <T> T getResult() {
        return (T) result;
    }

    @Override
    public String toString() {
        return "ApiBaseResult{" + "code='" + code + '\'' + ", msg='" + msg + '\'' + '}';
    }


    public static void main(String[] args) {
        ApiErrorCode internalServerError = ApiErrorCode.INTERNAL_SERVER_ERROR;
        System.out.println(internalServerError);
        ApiBaseResult apiBaseResult = ApiBaseResult.errorOf(ApiErrorCode.INTERNAL_SERVER_ERROR.getStatusCode(), ApiErrorCode.INTERNAL_SERVER_ERROR.getZnMessage());
        System.out.println(apiBaseResult);

        ApiBaseResult apiBaseResult1 = ApiBaseResult.successRet();
        System.out.println(apiBaseResult1);

    }
}
