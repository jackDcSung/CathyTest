package com.cathay.coindesk.controller;

import com.cathay.coindesk.TestFixtures;
import com.cathay.coindesk.client.CoinDeskClient;
import com.cathay.coindesk.client.CoinDeskResponse;
import com.cathay.coindesk.exception.CoinDeskClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CoinDesk 相關 API 的整合測試，走完整的 HTTP 路徑並使用真實的 DB 資料。
 *
 * 以 @MockBean 取代 CoinDeskClient，而不是像 CoinDeskClientTest 那樣用
 * MockRestServiceServer —— 這裡關心的是 Controller、Service、Mapper 與
 * 例外處理的接線是否正確，HTTP 傳輸細節已在 Client 的測試驗過了。
 *
 * 這樣測試結果也不受 CoinDesk / GitHub Pages 是否可用影響。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CoinDesk API")
class CoinDeskControllerTest {

    private static final Logger log = LoggerFactory.getLogger(CoinDeskControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CoinDeskClient coinDeskClient;

    @Test
    @DisplayName("shouldReturnOriginalCoinDeskResponse")
    void shouldReturnOriginalCoinDeskResponse() throws Exception {
        given(coinDeskClient.getCurrentPrice()).willReturn(sampleResponse());

        MvcResult result = mockMvc.perform(get("/api/coindesk/original"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chartName").value("Bitcoin"))
                .andExpect(jsonPath("$.time.updated").value("Sep 2, 2024 07:07:20 UTC"))
                .andExpect(jsonPath("$.bpi.USD.rate_float").value(57756.2984))
                .andReturn();

        log.info("CoinDesk 原始 API 內容: {}", body(result));
    }

    @Test
    @DisplayName("shouldReturnConvertedResponseWithChineseCurrencyNameFromDatabase")
    void shouldReturnConvertedResponseWithChineseCurrencyNameFromDatabase() throws Exception {
        given(coinDeskClient.getCurrentPrice()).willReturn(sampleResponse());

        MvcResult result = mockMvc.perform(get("/api/coindesk/converted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updateTime").value("2024/09/02 07:07:20"))
                .andExpect(jsonPath("$.currencies.length()").value(3))
                .andExpect(jsonPath("$.currencies[0].code").value("EUR"))
                .andExpect(jsonPath("$.currencies[0].chineseName").value("歐元"))
                .andExpect(jsonPath("$.currencies[1].chineseName").value("英鎊"))
                .andExpect(jsonPath("$.currencies[2].code").value("USD"))
                .andExpect(jsonPath("$.currencies[2].chineseName").value("美元"))
                .andExpect(jsonPath("$.currencies[2].rate").value(57756.2984))
                .andReturn();

        log.info("資料轉換後 API 內容: {}", body(result));
    }

    @Test
    @DisplayName("shouldReturnBadGatewayWhenCoinDeskIsUnavailable")
    void shouldReturnBadGatewayWhenCoinDeskIsUnavailable() throws Exception {
        given(coinDeskClient.getCurrentPrice())
                .willThrow(new CoinDeskClientException("呼叫 CoinDesk 失敗: connection timed out"));

        mockMvc.perform(get("/api/coindesk/converted"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.path").value("/api/coindesk/converted"));
    }

    private CoinDeskResponse sampleResponse() throws Exception {
        return objectMapper.readValue(TestFixtures.coinDeskJson(), CoinDeskResponse.class);
    }

    private String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
