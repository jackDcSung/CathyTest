package com.cathay.coindesk.exception;

/**
 * 呼叫 CoinDesk 失敗，涵蓋連線逾時、非 2xx 回應與下行內容無法解析。
 *
 * 與 CurrencyNotFoundException 等業務例外不同：這是「外部相依故障」而非
 * 「呼叫端用法有誤」，因此對應 502 而不是 4xx —— 呼叫端據此可判斷
 * 是上游出問題（稍後重試可能會成功），而不是自己的請求有錯。
 */
public class CoinDeskClientException extends RuntimeException {

    public CoinDeskClientException(String message) {
        super(message);
    }

    public CoinDeskClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
