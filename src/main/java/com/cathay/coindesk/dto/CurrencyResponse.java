package com.cathay.coindesk.dto;

import com.cathay.coindesk.entity.Currency;

import java.time.LocalDateTime;

public class CurrencyResponse {

    private final String code;
    private final String chineseName;
    private final LocalDateTime createdTime;
    private final LocalDateTime updatedTime;

    public CurrencyResponse(String code, String chineseName,
                            LocalDateTime createdTime, LocalDateTime updatedTime) {
        this.code = code;
        this.chineseName = chineseName;
        this.createdTime = createdTime;
        this.updatedTime = updatedTime;
    }

    public static CurrencyResponse from(Currency currency) {
        return new CurrencyResponse(currency.getCode(), currency.getChineseName(),
                currency.getCreatedTime(), currency.getUpdatedTime());
    }

    public String getCode() {
        return code;
    }

    public String getChineseName() {
        return chineseName;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }
}
