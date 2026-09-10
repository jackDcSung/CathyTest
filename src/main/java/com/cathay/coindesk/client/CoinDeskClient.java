package com.cathay.coindesk.client;

import com.cathay.coindesk.exception.CoinDeskClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 封裝與 CoinDesk 的 HTTP 溝通，Service 不需要知道 URL 與傳輸細節。
 */
@Component
public class CoinDeskClient {

    private static final Logger log = LoggerFactory.getLogger(CoinDeskClient.class);

    private final RestTemplate restTemplate;
    private final String apiUrl;

    public CoinDeskClient(RestTemplate restTemplate, @Value("${coindesk.api.url}") String apiUrl) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
    }

    public CoinDeskResponse getCurrentPrice() {
        try {
            CoinDeskResponse response = restTemplate.getForObject(apiUrl, CoinDeskResponse.class);
            if (response == null) {
                throw new CoinDeskClientException("CoinDesk 回傳空的下行內容");
            }
            return response;
        } catch (RestClientException ex) {
            // RestTemplate 會把連線逾時、非 2xx 與下行內容解析失敗一律包成 RestClientException，
            // 因此這一個 catch 就涵蓋所有外部呼叫的失敗情境。
            log.error("呼叫 CoinDesk 失敗, url={}", apiUrl, ex);
            throw new CoinDeskClientException("呼叫 CoinDesk 失敗: " + ex.getMessage(), ex);
        }
    }
}
