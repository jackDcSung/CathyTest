package com.cathay.coindesk.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class CurrencyRequest {

    // 3 碼的限制對應 schema.sql 的 code VARCHAR(3)。
    // 在 API 入口就擋下，避免超長或格式錯誤的輸入到了 DB 才失敗，
    // 把「使用者輸入有問題」(400) 變成「系統壞了」(500)。
    @NotBlank(message = "幣別代碼不可為空")
    @Pattern(regexp = "[A-Za-z]{3}", message = "幣別代碼須為 3 碼英文字母")
    private String code;

    // 同上，長度對應 chinese_name VARCHAR(50)。
    @NotBlank(message = "幣別中文名稱不可為空")
    @Size(max = 50, message = "幣別中文名稱長度不可超過 50")
    private String chineseName;

    public CurrencyRequest() {
    }

    public CurrencyRequest(String code, String chineseName) {
        this.code = code;
        this.chineseName = chineseName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getChineseName() {
        return chineseName;
    }

    public void setChineseName(String chineseName) {
        this.chineseName = chineseName;
    }
}
