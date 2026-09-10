package com.cathay.coindesk.exception;

/**
 * 新增時幣別代碼已存在。
 * 由 Service 先檢查後拋出，讓呼叫端收到有語意的 409，
 * 而不是 DB 主鍵衝突直接冒上來變成 500。
 */
public class CurrencyAlreadyExistsException extends RuntimeException {

    public CurrencyAlreadyExistsException(String code) {
        super("幣別已存在: " + code);
    }
}
