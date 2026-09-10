package com.cathay.coindesk.exception;

/**
 * 查無幣別資料。
 * 採 unchecked exception，讓它自然往上冒到 @RestControllerAdvice 統一處理，
 * 中間各層不必宣告 throws，也不必寫 try/catch。
 */
public class CurrencyNotFoundException extends RuntimeException {

    public CurrencyNotFoundException(String code) {
        super("查無幣別資料: " + code);
    }
}
