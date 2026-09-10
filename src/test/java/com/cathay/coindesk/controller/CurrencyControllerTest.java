package com.cathay.coindesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 幣別維護 API 的整合測試：啟動完整 Spring context，使用真實的 Service 與 H2，不用 mock。
 * 這裡要驗證的是只有走完整請求路徑才測得到的行為 —— @Valid 是否觸發、
 * 例外是否被 @RestControllerAdvice 接住、HTTP 狀態碼與回應結構是否正確。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// 每個測試結束後自動回滾，避免互相污染：
// 例如「刪除 JPY」的測試不會影響後面「查詢全部」的預期筆數。
@Transactional
@DisplayName("幣別維護 API")
class CurrencyControllerTest {

    private static final Logger log = LoggerFactory.getLogger(CurrencyControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("shouldReturnAllCurrenciesOrderedByCode")
    void shouldReturnAllCurrenciesOrderedByCode() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$[0].code").value("EUR"))
                .andReturn();

        log.info("幣別清單: {}", body(result));
    }

    @Test
    @DisplayName("shouldReturnCurrencyByCodeIgnoringLetterCase")
    void shouldReturnCurrencyByCodeIgnoringLetterCase() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/currencies/{code}", "usd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USD"))
                .andExpect(jsonPath("$.chineseName").value("美元"))
                .andReturn();

        log.info("查詢單一幣別: {}", body(result));
    }

    @Test
    @DisplayName("shouldReturn404WhenCurrencyDoesNotExist")
    void shouldReturn404WhenCurrencyDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/currencies/{code}", "XXX"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/currencies/XXX"));
    }

    @Test
    @DisplayName("shouldCreateCurrencySuccessfully")
    void shouldCreateCurrencySuccessfully() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("AUD", "澳幣")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/currencies/AUD"))
                .andExpect(jsonPath("$.code").value("AUD"))
                .andExpect(jsonPath("$.chineseName").value("澳幣"))
                .andReturn();

        log.info("新增幣別: {}", body(result));

        mockMvc.perform(get("/api/currencies/{code}", "AUD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chineseName").value("澳幣"));
    }

    @Test
    @DisplayName("shouldNormalizeCurrencyCodeToUpperCaseOnCreate")
    void shouldNormalizeCurrencyCodeToUpperCaseOnCreate() throws Exception {
        mockMvc.perform(post("/api/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("cad", "加幣")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CAD"));

        mockMvc.perform(get("/api/currencies/{code}", "CAD"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("shouldReturn409WhenCurrencyAlreadyExists")
    void shouldReturn409WhenCurrencyAlreadyExists() throws Exception {
        mockMvc.perform(post("/api/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("usd", "美元")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("shouldReturn400WhenRequestFieldsAreInvalid")
    void shouldReturn400WhenRequestFieldsAreInvalid() throws Exception {
        mockMvc.perform(post("/api/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("US", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("shouldReturn400WhenRequestBodyIsMalformed")
    void shouldReturn400WhenRequestBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/api/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("shouldUpdateChineseNameSuccessfully")
    void shouldUpdateChineseNameSuccessfully() throws Exception {
        MvcResult result = mockMvc.perform(put("/api/currencies/{code}", "usd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("美金")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USD"))
                .andExpect(jsonPath("$.chineseName").value("美金"))
                .andReturn();

        log.info("修改幣別: {}", body(result));
    }

    @Test
    @DisplayName("shouldReturn404WhenUpdatingCurrencyThatDoesNotExist")
    void shouldReturn404WhenUpdatingCurrencyThatDoesNotExist() throws Exception {
        mockMvc.perform(put("/api/currencies/{code}", "XXX")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("測試幣別")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("shouldDeleteCurrencySuccessfully")
    void shouldDeleteCurrencySuccessfully() throws Exception {
        mockMvc.perform(delete("/api/currencies/{code}", "jpy"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/currencies/{code}", "JPY"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("shouldReturn404WhenDeletingCurrencyThatDoesNotExist")
    void shouldReturn404WhenDeletingCurrencyThatDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/currencies/{code}", "XXX"))
                .andExpect(status().isNotFound());
    }

    private String json(String code, String chineseName) throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("code", code);
        request.put("chineseName", chineseName);
        return objectMapper.writeValueAsString(request);
    }

    private String updateJson(String chineseName) throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("chineseName", chineseName);
        return objectMapper.writeValueAsString(request);
    }

    // MockMvc 回應的預設字元集是 ISO-8859-1，直接取字串會讓中文變成亂碼，
    // 因此明確指定 UTF-8。
    private String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
