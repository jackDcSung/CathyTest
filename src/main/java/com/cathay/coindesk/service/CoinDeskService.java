package com.cathay.coindesk.service;

import com.cathay.coindesk.client.CoinDeskClient;
import com.cathay.coindesk.client.CoinDeskResponse;
import com.cathay.coindesk.dto.CoinDeskRateResponse;
import com.cathay.coindesk.entity.Currency;
import com.cathay.coindesk.repository.CurrencyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
/**
 * CoinDesk 相關的 use case，負責把外部服務與資料庫兩個來源組合起來。
 *
 * 轉換規則本身放在 CoinDeskResponseMapper（純函式、不碰 IO），
 * 這一層只負責「先把需要的中文名稱一次查好」再交給它 —— 兩者責任分開，
 * 轉換規則才能用最便宜的單元測試覆蓋。
 */
public class CoinDeskService {

    private final CoinDeskClient coinDeskClient;
    private final CurrencyRepository currencyRepository;
    private final CoinDeskResponseMapper mapper;

    public CoinDeskService(CoinDeskClient coinDeskClient,
                           CurrencyRepository currencyRepository,
                           CoinDeskResponseMapper mapper) {
        this.coinDeskClient = coinDeskClient;
        this.currencyRepository = currencyRepository;
        this.mapper = mapper;
    }

    public CoinDeskResponse getOriginalData() {
        return coinDeskClient.getCurrentPrice();
    }

    // 標記為唯讀交易：明確宣告不會寫入，Hibernate 可略過 dirty checking。
    // 即使目前只有一次查詢，交易邊界仍統一放在 Service 層，與 CurrencyService 一致。
    @Transactional(readOnly = true)
    public CoinDeskRateResponse getConvertedData() {
        CoinDeskResponse source = coinDeskClient.getCurrentPrice();
        return mapper.toRateResponse(source, loadChineseNames(source));
    }

    /**
     * 一次查回本次需要的幣別中文名稱，避免在轉換迴圈中逐筆查 DB（N+1）。
     */
    private Map<String, String> loadChineseNames(CoinDeskResponse source) {
        if (source.getBpi() == null || source.getBpi().isEmpty()) {
            // 沒有任何幣別時直接回空 Map，連 DB 都不需要查。
            return Collections.emptyMap();
        }

        // 收成 TreeSet 而非 HashSet：代碼順序固定，IN 查詢的參數順序可預期，
        // 測試才能穩定斷言實際傳給 Repository 的內容。
        Set<String> codes = source.getBpi().entrySet().stream()
                .map(CoinDeskService::codeOf)
                .filter(Objects::nonNull)
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(TreeSet::new));

        return currencyRepository.findByCodeIn(codes).stream()
                .collect(Collectors.toMap(Currency::getCode, Currency::getChineseName));
    }

    // 幣別代碼以 bpi 物件內的 code 為準，缺值時退回鍵值，兩者在正常資料下相同。
    private static String codeOf(Map.Entry<String, CoinDeskResponse.Bpi> entry) {
        CoinDeskResponse.Bpi bpi = entry.getValue();
        return bpi != null && bpi.getCode() != null ? bpi.getCode() : entry.getKey();
    }
}
