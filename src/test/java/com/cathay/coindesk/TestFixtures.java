package com.cathay.coindesk;

import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * 測試資料來源。
 *
 * coindesk.json 是從題目指定的 API 實際抓下來的回應，而不是手寫的假資料 ——
 * 這樣 DTO 的欄位對應若與真實結構不符，測試就會失敗。
 */
public final class TestFixtures {

    private static final String COINDESK_SAMPLE = "/coindesk.json";

    private TestFixtures() {
    }

    public static String coinDeskJson() {
        try (InputStream in = TestFixtures.class.getResourceAsStream(COINDESK_SAMPLE)) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("讀取測試資料失敗: " + COINDESK_SAMPLE, ex);
        }
    }
}
