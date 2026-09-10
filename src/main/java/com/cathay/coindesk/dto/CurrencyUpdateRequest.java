package com.cathay.coindesk.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class CurrencyUpdateRequest {

    // 長度限制對應 schema.sql 的 chinese_name VARCHAR(50)。
    @NotBlank(message = "幣別中文名稱不可為空")
    @Size(max = 50, message = "幣別中文名稱長度不可超過 50")
    private String chineseName;

    public CurrencyUpdateRequest() {
    }

    public CurrencyUpdateRequest(String chineseName) {
        this.chineseName = chineseName;
    }

    public String getChineseName() {
        return chineseName;
    }

    public void setChineseName(String chineseName) {
        this.chineseName = chineseName;
    }
}
