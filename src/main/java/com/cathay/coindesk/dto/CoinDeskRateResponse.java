package com.cathay.coindesk.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 資料轉換後的新 API contract。
 *
 * 三個幣別欄位對應題目要求的「幣別、幣別中文名稱、匯率」。
 *
 * updateTime 宣告為 String 而非 LocalDateTime：題目指定輸出格式為
 * yyyy/MM/dd HH:mm:ss，用字串可確保序列化結果就是這個格式，
 * 不受 Jackson 的日期序列化設定影響。
 */
public class CoinDeskRateResponse {

    private final String updateTime;
    private final List<CurrencyRate> currencies;

    public CoinDeskRateResponse(String updateTime, List<CurrencyRate> currencies) {
        this.updateTime = updateTime;
        this.currencies = currencies;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public List<CurrencyRate> getCurrencies() {
        return currencies;
    }

    public static class CurrencyRate {

        private final String code;
        private final String chineseName;
        private final BigDecimal rate;

        public CurrencyRate(String code, String chineseName, BigDecimal rate) {
            this.code = code;
            this.chineseName = chineseName;
            this.rate = rate;
        }

        public String getCode() {
            return code;
        }

        public String getChineseName() {
            return chineseName;
        }

        public BigDecimal getRate() {
            return rate;
        }
    }
}
