package com.cathay.coindesk.service;

import com.cathay.coindesk.client.CoinDeskClient;
import com.cathay.coindesk.client.CoinDeskResponse;
import org.springframework.stereotype.Service;

@Service
/**
 * CoinDesk 相關的 use case。
 *
 * 目前只是轉呼叫 Client，仍保留這一層的理由：Controller 不應直接依賴外部服務的
 * 存取細節，而下一步的資料轉換需要同時組合 CoinDesk 與資料庫兩個來源，
 * 那段邏輯的位置就在這裡。
 */
public class CoinDeskService {

    private final CoinDeskClient coinDeskClient;

    public CoinDeskService(CoinDeskClient coinDeskClient) {
        this.coinDeskClient = coinDeskClient;
    }

    public CoinDeskResponse getOriginalData() {
        return coinDeskClient.getCurrentPrice();
    }
}
