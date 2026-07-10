package ir.ebb.base.web;

import ir.ebb.common.dto.response.BaseResponse;

/**
 * Replaces the dropped {@code ir.ebb.common.controller.BaseController}'s
 * {@code success(...)} methods. Pekko HTTP routes call these to wrap payloads in
 * the {@link BaseResponse} success envelope.
 */
public final class ResponseHelper {

    private ResponseHelper() {}

    public static final String SUCCESS = "SUCCESS";

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(SUCCESS, 200, data);
    }

    public static <T> BaseResponse<T> success(T data, int status) {
        return new BaseResponse<>(SUCCESS, status, data);
    }

    public static <T> BaseResponse<T> success(T data, int status, String message) {
        return new BaseResponse<>(message, status, data);
    }

    public static BaseResponse<Void> success(int status) {
        return new BaseResponse<>(SUCCESS, status, null);
    }

    public static BaseResponse<Void> success(int status, String message) {
        return new BaseResponse<>(message, status, null);
    }

    public static BaseResponse<Void> success(String message) {
        return new BaseResponse<>(message, 200, null);
    }
}
