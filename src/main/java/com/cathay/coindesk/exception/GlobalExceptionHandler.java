package com.cathay.coindesk.exception;

import com.cathay.coindesk.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

/**
 * 統一 API 錯誤格式。繼承 ResponseEntityExceptionHandler，
 * 讓 Spring MVC 自身的例外（如 405、參數缺漏）也回相同結構。
 *
 * 這裡同時是整個系統的錯誤對照表：每個例外對應到哪個 ErrorCode，一眼看得完。
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CurrencyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCurrencyNotFound(CurrencyNotFoundException ex,
                                                                HttpServletRequest request) {
        return build(ErrorCode.CURRENCY_NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(CurrencyAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCurrencyAlreadyExists(CurrencyAlreadyExistsException ex,
                                                                     HttpServletRequest request) {
        return build(ErrorCode.CURRENCY_ALREADY_EXISTS, ex.getMessage(), request.getRequestURI());
    }

    // 保底處理。完整堆疊只寫進 log，對外固定回一句話，
    // 避免把內部類別名稱、SQL、檔案路徑等細節洩漏給呼叫端。
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("未預期的錯誤, uri={}", request.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, "系統發生未預期的錯誤", request.getRequestURI());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatus status,
                                                                  WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return handleExceptionInternal(ex, message, headers, ErrorCode.INVALID_REQUEST.getHttpStatus(), request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatus status,
                                                                  WebRequest request) {
        return handleExceptionInternal(ex, "請求內容格式錯誤，無法解析", headers,
                ErrorCode.INVALID_REQUEST.getHttpStatus(), request);
    }

    // Spring MVC 自身的例外最後都會匯流到這個方法，
    // 覆寫它就能讓所有錯誤共用同一個 ErrorResponse 結構。
    // 回傳型別被父類別簽章固定為 ResponseEntity<Object>：Java 泛型不具協變性，
    // 無法在覆寫時窄化成 ResponseEntity<ErrorResponse>，但放進去的 body 一律是 ErrorResponse。
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             Object body,
                                                             HttpHeaders headers,
                                                             HttpStatus status,
                                                             WebRequest request) {
        String message = body instanceof String ? (String) body : status.getReasonPhrase();
        ErrorResponse errorResponse = ErrorResponse.of(resolveErrorCode(status), message, pathOf(request));
        return new ResponseEntity<Object>(errorResponse, headers, status);
    }

    // Spring MVC 內建的例外（405、415、缺少參數…）沒有專屬的業務錯誤碼，
    // 依 4xx / 5xx 歸到請求類或系統類，維持回應結構一致。
    private ErrorCode resolveErrorCode(HttpStatus status) {
        return status.is4xxClientError() ? ErrorCode.INVALID_REQUEST : ErrorCode.INTERNAL_ERROR;
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode errorCode, String message, String path) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode, message, path));
    }

    private String pathOf(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return request.getDescription(false);
    }
}
