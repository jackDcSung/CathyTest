package com.cathay.coindesk.dto;

import com.cathay.coindesk.exception.ErrorCode;

import java.time.LocalDateTime;

/**
 * 統一的錯誤回應格式。所有欄位皆為 final，建立後不可變動。
 *
 * status / error 來自 HTTP 協定層，code 來自應用層，兩者並存：
 * 呼叫端用 status 決定要不要進錯誤流程，用 code 對應顯示文字。
 */
public class ErrorResponse {

    // 發生時間由物件自己產生，呼叫端不需要也不應該傳入。
    private final LocalDateTime timestamp = LocalDateTime.now();

    private final int status;
    private final String error;
    private final String code;
    private final String message;
    private final String path;

    private ErrorResponse(int status, String error, String code, String message, String path) {
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.path = path;
    }

    /**
     * 一律由 ErrorCode 建立，確保 status、error、code 三者永遠來自同一個定義，
     * 不會出現「狀態碼改了但錯誤碼沒改」的不一致。
     */
    public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
        return new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getHttpStatus().getReasonPhrase(),
                errorCode.getCode(),
                message,
                path);
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }
}
