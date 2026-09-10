package com.cathay.coindesk.service;

import com.cathay.coindesk.client.CoinDeskClient;
import com.cathay.coindesk.client.CoinDeskResponse;
import com.cathay.coindesk.dto.CoinDeskRateResponse;
import com.cathay.coindesk.entity.Currency;
import com.cathay.coindesk.exception.CoinDeskClientException;
import com.cathay.coindesk.repository.CurrencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
/**
 * 轉換流程的單元測試：驗證 Service 如何「組合」外部服務與資料庫兩個來源。
 *
 * 用 Mockito 而非 @SpringBootTest —— 這裡要驗的是呼叫順序與查詢次數，
 * 不需要真實的 HTTP 與 DB，啟動 Spring 只會讓測試變慢且失焦。
 */
@DisplayName("CoinDesk 轉換流程（不啟動 Spring Context）")
class CoinDeskServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CoinDeskClient coinDeskClient;

    @Mock
    private CurrencyRepository currencyRepository;

    private CoinDeskService coinDeskService;

    @BeforeEach
    void setUp() {
        // Mapper 是純轉換邏輯，直接用真實物件，測試才涵蓋真正會上線的行為。
        coinDeskService = new CoinDeskService(coinDeskClient, currencyRepository, new CoinDeskResponseMapper());
    }

    @Test
    @DisplayName("shouldConvertCoinDeskResponseWithChineseCurrencyNameFromDatabase")
    void shouldConvertCoinDeskResponseWithChineseCurrencyNameFromDatabase() throws Exception {
        given(coinDeskClient.getCurrentPrice()).willReturn(loadSample());
        given(currencyRepository.findByCodeIn(anyCollection())).willReturn(Arrays.asList(
                new Currency("USD", "美元"),
                new Currency("GBP", "英鎊"),
                new Currency("EUR", "歐元")));

        CoinDeskRateResponse result = coinDeskService.getConvertedData();

        assertEquals("2024/09/02 07:07:20", result.getUpdateTime());
        assertEquals(3, result.getCurrencies().size());
        assertEquals("歐元", result.getCurrencies().get(0).getChineseName());
        assertEquals("英鎊", result.getCurrencies().get(1).getChineseName());
        assertEquals("美元", result.getCurrencies().get(2).getChineseName());
    }

    // 這個測試是 N+1 的防線：用 times(1) 確認只查一次，
    // 並用 ArgumentCaptor 檢查實際傳入的代碼集合，避免日後有人改回逐筆查詢。
    @Test
    @DisplayName("shouldLoadCurrencyNamesInOneQueryInsteadOfPerCurrency")
    void shouldLoadCurrencyNamesInOneQueryInsteadOfPerCurrency() throws Exception {
        given(coinDeskClient.getCurrentPrice()).willReturn(loadSample());
        given(currencyRepository.findByCodeIn(anyCollection())).willReturn(Collections.<Currency>emptyList());

        coinDeskService.getConvertedData();

        // ArgumentCaptor.forClass 無法帶入泛型參數，這裡的轉型是安全的。
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> requestedCodes =
                (ArgumentCaptor<Collection<String>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Collection.class);

        verify(currencyRepository, times(1)).findByCodeIn(requestedCodes.capture());
        // 轉成 List 比較，才驗證得到「順序」而不只是「內容」—— 這是 TreeSet 的作用。
        assertEquals(Arrays.asList("EUR", "GBP", "USD"), new ArrayList<>(requestedCodes.getValue()));
    }

    @Test
    @DisplayName("shouldSkipCurrencyLookupWhenCoinDeskReturnsNoRates")
    void shouldSkipCurrencyLookupWhenCoinDeskReturnsNoRates() {
        CoinDeskResponse source = new CoinDeskResponse();
        CoinDeskResponse.Time time = new CoinDeskResponse.Time();
        time.setUpdatedISO("2024-09-02T07:07:20+00:00");
        source.setTime(time);
        given(coinDeskClient.getCurrentPrice()).willReturn(source);

        CoinDeskRateResponse result = coinDeskService.getConvertedData();

        assertTrue(result.getCurrencies().isEmpty());
        then(currencyRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("shouldPropagateCoinDeskClientExceptionWhenUpstreamFails")
    void shouldPropagateCoinDeskClientExceptionWhenUpstreamFails() {
        given(coinDeskClient.getCurrentPrice()).willThrow(new CoinDeskClientException("upstream unavailable"));

        assertThrows(CoinDeskClientException.class, () -> coinDeskService.getConvertedData());
        then(currencyRepository).shouldHaveNoInteractions();
    }

    private CoinDeskResponse loadSample() throws Exception {
        InputStream in = getClass().getResourceAsStream("/coindesk.json");
        try {
            return objectMapper.readValue(in, CoinDeskResponse.class);
        } finally {
            in.close();
        }
    }
}
