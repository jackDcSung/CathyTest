package com.cathay.coindesk.client;

import com.cathay.coindesk.TestFixtures;
import com.cathay.coindesk.exception.CoinDeskClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 以 MockRestServiceServer 模擬 CoinDesk，測試不依賴外部網路是否可用。
 */
@DisplayName("CoinDesk API Client")
class CoinDeskClientTest {

    private static final Logger log = LoggerFactory.getLogger(CoinDeskClientTest.class);
    private static final String API_URL = "https://coindesk.test/coindesk.json";

    private MockRestServiceServer server;
    private CoinDeskClient coinDeskClient;

    // MockRestServiceServer 綁在 RestTemplate 上攔截請求，
    // 因此測試完全不會發出真實的網路連線。
    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        coinDeskClient = new CoinDeskClient(restTemplate, API_URL);
    }

    @Test
    @DisplayName("shouldReturnParsedCoinDeskResponse")
    void shouldReturnParsedCoinDeskResponse() {
        server.expect(requestTo(API_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(TestFixtures.coinDeskJson(), MediaType.APPLICATION_JSON));

        CoinDeskResponse response = coinDeskClient.getCurrentPrice();

        assertEquals("Bitcoin", response.getChartName());
        assertEquals("Sep 2, 2024 07:07:20 UTC", response.getTime().getUpdated());
        assertEquals(3, response.getBpi().size());
        // BigDecimal 的 equals 會連 scale 一起比較（57756.2984 與 57756.29840 會判定不相等），
        // 這裡只在意數值是否相同，因此用 compareTo。
        assertEquals(0, new BigDecimal("57756.2984").compareTo(response.getBpi().get("USD").getRateFloat()));
        // 確認請求真的被發出，而不是被略過。
        server.verify();

        log.info("CoinDesk 原始下行內容: chartName={}, updated={}, bpi={}",
                response.getChartName(), response.getTime().getUpdated(), response.getBpi().keySet());
    }

    @Test
    @DisplayName("shouldThrowCoinDeskClientExceptionWhenUpstreamReturnsServerError")
    void shouldThrowCoinDeskClientExceptionWhenUpstreamReturnsServerError() {
        server.expect(requestTo(API_URL)).andRespond(withServerError());

        assertThrows(CoinDeskClientException.class, () -> coinDeskClient.getCurrentPrice());
    }

    @Test
    @DisplayName("shouldThrowCoinDeskClientExceptionWhenResponseIsNotParsable")
    void shouldThrowCoinDeskClientExceptionWhenResponseIsNotParsable() {
        server.expect(requestTo(API_URL))
                .andRespond(withSuccess("not-a-json", MediaType.APPLICATION_JSON));

        assertThrows(CoinDeskClientException.class, () -> coinDeskClient.getCurrentPrice());
    }

    // 連線逾時在 RestTemplate 會表現為 ResourceAccessException，
    // 這裡直接讓 mock 拋出它，驗證逾時同樣被轉成語意化例外。
    @Test
    @DisplayName("shouldThrowCoinDeskClientExceptionWhenConnectionTimesOut")
    void shouldThrowCoinDeskClientExceptionWhenConnectionTimesOut() {
        server.expect(requestTo(API_URL))
                .andRespond(request -> {
                    throw new ResourceAccessException("Read timed out");
                });

        assertThrows(CoinDeskClientException.class, () -> coinDeskClient.getCurrentPrice());
    }

    // 回歸測試：非 2xx 時 RestClientException 的訊息會夾帶上游的完整 response body，
    // 若直接串進對外訊息就會外洩上游資訊。這裡確保對外訊息乾淨、細節只留在 cause。
    @Test
    @DisplayName("shouldNotExposeUpstreamResponseBodyInExceptionMessage")
    void shouldNotExposeUpstreamResponseBodyInExceptionMessage() {
        String upstreamSecret = "internal-host-should-not-leak";
        server.expect(requestTo(API_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_HTML)
                        .body("<html>" + upstreamSecret + "</html>"));

        CoinDeskClientException ex = assertThrows(CoinDeskClientException.class,
                () -> coinDeskClient.getCurrentPrice());

        assertFalse(ex.getMessage().contains(upstreamSecret));
        assertNotNull(ex.getCause());
    }

    // 對應 CoinDeskClient 內的 null 檢查：getForObject 在 204 無內容時會回傳 null。
    // 少了這個測試，那段防護就是未經驗證的程式碼。
    @Test
    @DisplayName("shouldThrowCoinDeskClientExceptionWhenResponseBodyIsEmpty")
    void shouldThrowCoinDeskClientExceptionWhenResponseBodyIsEmpty() {
        server.expect(requestTo(API_URL))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThrows(CoinDeskClientException.class, () -> coinDeskClient.getCurrentPrice());
    }
}
