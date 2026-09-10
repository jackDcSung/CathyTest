package com.cathay.coindesk.service;

import com.cathay.coindesk.client.CoinDeskResponse;
import com.cathay.coindesk.dto.CoinDeskRateResponse;
import com.cathay.coindesk.exception.CoinDeskClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 資料轉換邏輯的單元測試（對應題目要求的「針對資料轉換相關邏輯作單元測試」）。
 *
 * 不啟動 Spring Context、不碰 DB 與網路，直接 new 出 Mapper 來測，
 * 因此可以用很低的成本把時區換算、格式備援、對照表缺漏等邊界情境全部蓋滿。
 */
@DisplayName("CoinDesk 資料轉換邏輯")
class CoinDeskResponseMapperTest {

    private static final Logger log = LoggerFactory.getLogger(CoinDeskResponseMapperTest.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CoinDeskResponseMapper mapper;
    private Map<String, String> chineseNames;

    @BeforeEach
    void setUp() {
        mapper = new CoinDeskResponseMapper();
        chineseNames = new HashMap<>();
        chineseNames.put("USD", "美元");
        chineseNames.put("GBP", "英鎊");
        chineseNames.put("EUR", "歐元");
    }

    @Test
    @DisplayName("shouldConvertCoinDeskResponseWithChineseCurrencyName")
    void shouldConvertCoinDeskResponseWithChineseCurrencyName() throws Exception {
        CoinDeskRateResponse result = mapper.toRateResponse(loadSample(), chineseNames);

        assertEquals("2024/09/02 07:07:20", result.getUpdateTime());
        assertEquals(3, result.getCurrencies().size());
        assertEquals("EUR", result.getCurrencies().get(0).getCode());
        assertEquals("GBP", result.getCurrencies().get(1).getCode());
        assertEquals("USD", result.getCurrencies().get(2).getCode());

        CoinDeskRateResponse.CurrencyRate usd = result.getCurrencies().get(2);
        assertEquals("美元", usd.getChineseName());
        assertEquals(0, new BigDecimal("57756.2984").compareTo(usd.getRate()));

        log.info("轉換結果: {}", objectMapper.writeValueAsString(result));
    }

    @Test
    @DisplayName("shouldFormatUpdateTimeFromUpdatedIsoField")
    void shouldFormatUpdateTimeFromUpdatedIsoField() {
        CoinDeskResponse.Time time = new CoinDeskResponse.Time();
        time.setUpdatedISO("1990-01-01T00:00:00+00:00");
        time.setUpdated("Jan 1, 1990 00:00:00 UTC");

        assertEquals("1990/01/01 00:00:00", mapper.formatUpdateTime(time));
    }

    @Test
    @DisplayName("shouldConvertNonUtcUpdateTimeToUtc")
    void shouldConvertNonUtcUpdateTimeToUtc() {
        CoinDeskResponse.Time time = new CoinDeskResponse.Time();
        time.setUpdatedISO("2024-09-02T15:07:20+08:00");

        assertEquals("2024/09/02 07:07:20", mapper.formatUpdateTime(time));
    }

    @Test
    @DisplayName("shouldFallBackToUpdatedFieldWhenIsoIsMissing")
    void shouldFallBackToUpdatedFieldWhenIsoIsMissing() {
        CoinDeskResponse.Time time = new CoinDeskResponse.Time();
        time.setUpdated("Sep 2, 2024 07:07:20 UTC");

        assertEquals("2024/09/02 07:07:20", mapper.formatUpdateTime(time));
    }

    @Test
    @DisplayName("shouldReturnNullUpdateTimeWhenBothTimeFieldsAreMissing")
    void shouldReturnNullUpdateTimeWhenBothTimeFieldsAreMissing() {
        assertNull(mapper.formatUpdateTime(null));
        assertNull(mapper.formatUpdateTime(new CoinDeskResponse.Time()));
    }

    @Test
    @DisplayName("shouldThrowWhenUpdateTimeFormatIsUnexpected")
    void shouldThrowWhenUpdateTimeFormatIsUnexpected() {
        CoinDeskResponse.Time time = new CoinDeskResponse.Time();
        time.setUpdatedISO("2024/09/02 07:07:20");

        assertThrows(CoinDeskClientException.class, () -> mapper.formatUpdateTime(time));
    }

    @Test
    @DisplayName("shouldFallBackToEnglishDescriptionWhenCurrencyIsNotMapped")
    void shouldFallBackToEnglishDescriptionWhenCurrencyIsNotMapped() throws Exception {
        CoinDeskRateResponse result = mapper.toRateResponse(loadSample(), Collections.<String, String>emptyMap());

        CoinDeskRateResponse.CurrencyRate usd = result.getCurrencies().get(2);
        assertEquals("USD", usd.getCode());
        assertEquals("United States Dollar", usd.getChineseName());
    }

    @Test
    @DisplayName("shouldReturnEmptyCurrencyListWhenBpiIsAbsent")
    void shouldReturnEmptyCurrencyListWhenBpiIsAbsent() {
        CoinDeskResponse source = new CoinDeskResponse();
        CoinDeskResponse.Time time = new CoinDeskResponse.Time();
        time.setUpdatedISO("2024-09-02T07:07:20+00:00");
        source.setTime(time);

        CoinDeskRateResponse result = mapper.toRateResponse(source, chineseNames);

        assertEquals("2024/09/02 07:07:20", result.getUpdateTime());
        assertTrue(result.getCurrencies().isEmpty());
    }

    @Test
    @DisplayName("shouldRejectNullCoinDeskResponse")
    void shouldRejectNullCoinDeskResponse() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toRateResponse(null, chineseNames));
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
