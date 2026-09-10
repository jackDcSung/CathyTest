package com.cathay.coindesk.exception;

import org.springframework.http.HttpStatus;

/**
 * 應用層錯誤碼。
 *
 * code 是對外的穩定契約：呼叫端可據此做多語系或錯誤分流，
 * 訊息文字之後怎麼改都不影響它。
 *
 * httpStatus 仍沿用 HTTP 協定層的標準語意（RFC 9110），
 * 兩層分工而非互相取代 —— 自訂協定狀態碼會讓瀏覽器、代理伺服器與監控系統無法理解。
 *
 * 前綴代表錯誤來源：CUR 幣別領域、EXT 外部服務、REQ 請求內容、SYS 系統本身。
 */
public enum ErrorCode {

    CURRENCY_NOT_FOUND("CUR001", HttpStatus.NOT_FOUND),
    CURRENCY_ALREADY_EXISTS("CUR002", HttpStatus.CONFLICT),
    COINDESK_UNAVAILABLE("EXT001", HttpStatus.BAD_GATEWAY),
    INVALID_REQUEST("REQ001", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("SYS001", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final HttpStatus httpStatus;

    ErrorCode(String code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
