package com.cathay.coindesk.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

/**
 * CoinDesk 下行內容。屬於外部 API contract，與本系統對外的 DTO 分開維護。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoinDeskResponse {

    private Time time;
    private String disclaimer;
    private String chartName;
    private Map<String, Bpi> bpi;

    public Time getTime() {
        return time;
    }

    public void setTime(Time time) {
        this.time = time;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public String getChartName() {
        return chartName;
    }

    public void setChartName(String chartName) {
        this.chartName = chartName;
    }

    public Map<String, Bpi> getBpi() {
        return bpi;
    }

    public void setBpi(Map<String, Bpi> bpi) {
        this.bpi = bpi;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Time {

        private String updated;
        private String updatedISO;
        private String updateduk;

        public String getUpdated() {
            return updated;
        }

        public void setUpdated(String updated) {
            this.updated = updated;
        }

        public String getUpdatedISO() {
            return updatedISO;
        }

        public void setUpdatedISO(String updatedISO) {
            this.updatedISO = updatedISO;
        }

        public String getUpdateduk() {
            return updateduk;
        }

        public void setUpdateduk(String updateduk) {
            this.updateduk = updateduk;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bpi {

        private String code;
        private String symbol;
        private String rate;
        private String description;

        // rate 是含千分位的字串，rate_float 才是可運算的數值；用 BigDecimal 接避免 double 精度誤差。
        @JsonProperty("rate_float")
        private BigDecimal rateFloat;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getRate() {
            return rate;
        }

        public void setRate(String rate) {
            this.rate = rate;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigDecimal getRateFloat() {
            return rateFloat;
        }

        public void setRateFloat(BigDecimal rateFloat) {
            this.rateFloat = rateFloat;
        }
    }
}
