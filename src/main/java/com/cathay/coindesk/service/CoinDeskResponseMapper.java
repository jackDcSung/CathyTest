package com.cathay.coindesk.service;

import com.cathay.coindesk.client.CoinDeskResponse;
import com.cathay.coindesk.dto.CoinDeskRateResponse;
import com.cathay.coindesk.exception.CoinDeskClientException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CoinDesk 下行內容 -> 對外 API contract 的轉換。
 * 不依賴 DB 與網路，中文名稱由呼叫端一次查好後傳入，方便單獨測試。
 */
@Component
public class CoinDeskResponseMapper {

    // DateTimeFormatter 是 immutable / thread-safe，可以安全共用（SimpleDateFormat 則不行）。
    private static final DateTimeFormatter OUTPUT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    // time.updated 的格式，例如 "Sep 2, 2024 07:07:20 UTC"。
    private static final DateTimeFormatter COINDESK_UPDATED_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss z", Locale.ENGLISH);

    /**
     * @param source            CoinDesk 下行內容，不可為 null（傳 null 代表呼叫端有錯）
     * @param chineseNameByCode 幣別代碼對中文名稱，允許為 null 或空 ——
     *                          代表對照表尚未維護，此時退回 CoinDesk 的英文描述，
     *                          屬於正常的降級而非錯誤
     */
    public CoinDeskRateResponse toRateResponse(CoinDeskResponse source, Map<String, String> chineseNameByCode) {
        if (source == null) {
            // 傳 null 進來屬於程式錯誤，不是外部服務故障，
            // 因此不轉成 CoinDeskClientException（那會誤報成 502）。
            throw new IllegalArgumentException("CoinDesk 下行內容不可為 null");
        }

        List<CoinDeskRateResponse.CurrencyRate> currencies = source.getBpi() == null
                ? Collections.emptyList()
                : source.getBpi().entrySet().stream()
                        .filter(entry -> entry.getValue() != null)
                        .map(entry -> toCurrencyRate(entry, chineseNameByCode))
                        // bpi 是 Map，走訪順序不保證固定；排序後回應順序才可預期，
                        // 呼叫端不必自行排序，測試也才能穩定斷言第幾筆是哪個幣別。
                        .sorted(Comparator.comparing(CoinDeskRateResponse.CurrencyRate::getCode))
                        .collect(Collectors.toList());

        return new CoinDeskRateResponse(formatUpdateTime(source.getTime()), currencies);
    }

    private CoinDeskRateResponse.CurrencyRate toCurrencyRate(Map.Entry<String, CoinDeskResponse.Bpi> entry,
                                                             Map<String, String> chineseNameByCode) {
        CoinDeskResponse.Bpi bpi = entry.getValue();
        // 幣別代碼以物件內的 code 為準，缺值時退回 bpi 的鍵值，兩者在正常資料下相同。
        String code = normalize(bpi.getCode() != null ? bpi.getCode() : entry.getKey());
        return new CoinDeskRateResponse.CurrencyRate(
                code, chineseNameOf(code, bpi, chineseNameByCode), bpi.getRateFloat());
    }

    /**
     * 優先使用 updatedISO：帶有明確 offset，不需猜測時區；updated 只在缺值時作為備援。
     * 兩者皆為 UTC 基準，輸出統一以 UTC 呈現。
     *
     * 上游完全沒提供時間資訊時回傳 null，而不是讓整支 API 失敗 ——
     * 匯率本身仍然有效，不該因為少一個顯示欄位就中斷。
     * 但「有值卻解析不出來」代表上游契約改變，那會轉成 CoinDeskClientException。
     *
     * 刻意保留為 package-private 而非 private：讓單元測試能直接驗證
     * 時間解析與時區換算，不必每次都組出完整的 CoinDeskResponse。
     */
    String formatUpdateTime(CoinDeskResponse.Time time) {
        if (time == null) {
            return null;
        }
        try {
            if (StringUtils.hasText(time.getUpdatedISO())) {
                return OffsetDateTime.parse(time.getUpdatedISO().trim())
                        .withOffsetSameInstant(ZoneOffset.UTC)
                        .format(OUTPUT_FORMATTER);
            }
            if (StringUtils.hasText(time.getUpdated())) {
                return ZonedDateTime.parse(time.getUpdated().trim(), COINDESK_UPDATED_FORMATTER)
                        .withZoneSameInstant(ZoneOffset.UTC)
                        .format(OUTPUT_FORMATTER);
            }
        } catch (DateTimeParseException ex) {
            throw new CoinDeskClientException("CoinDesk 更新時間格式無法解析", ex);
        }
        return null;
    }

    // 中文名稱以幣別對應表為主；對應表尚未維護時，退回 CoinDesk 的英文描述，避免回傳 null 欄位。
    private String chineseNameOf(String code, CoinDeskResponse.Bpi bpi, Map<String, String> chineseNameByCode) {
        if (chineseNameByCode != null) {
            String name = chineseNameByCode.get(code);
            if (name != null) {
                return name;
            }
        }
        return bpi.getDescription();
    }

    private String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}
