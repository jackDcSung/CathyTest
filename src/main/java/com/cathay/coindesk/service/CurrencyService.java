package com.cathay.coindesk.service;

import com.cathay.coindesk.dto.CurrencyRequest;
import com.cathay.coindesk.dto.CurrencyResponse;
import com.cathay.coindesk.dto.CurrencyUpdateRequest;
import com.cathay.coindesk.entity.Currency;
import com.cathay.coindesk.exception.CurrencyAlreadyExistsException;
import com.cathay.coindesk.exception.CurrencyNotFoundException;
import com.cathay.coindesk.repository.CurrencyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Transactional
public class CurrencyService {

    private final CurrencyRepository currencyRepository;

    public CurrencyService(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    @Transactional(readOnly = true)
    public List<CurrencyResponse> findAll() {
        return currencyRepository.findAllByOrderByCodeAsc().stream()
                .map(CurrencyResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CurrencyResponse findByCode(String code) {
        return CurrencyResponse.from(getOrThrow(code));
    }

    public CurrencyResponse create(CurrencyRequest request) {
        String code = normalize(request.getCode());
        // 先檢查再新增，讓重複幣別回 409 而不是把 DB constraint violation 拋給呼叫端。
        if (currencyRepository.existsById(code)) {
            throw new CurrencyAlreadyExistsException(code);
        }
        return CurrencyResponse.from(currencyRepository.save(new Currency(code, request.getChineseName())));
    }

    public CurrencyResponse update(String code, CurrencyUpdateRequest request) {
        Currency currency = getOrThrow(code);
        currency.setChineseName(request.getChineseName());
        // saveAndFlush 讓 @PreUpdate 立即觸發，回傳的 updatedTime 才是這次異動後的值。
        return CurrencyResponse.from(currencyRepository.saveAndFlush(currency));
    }

    // 刪除不存在的資料回 404，與查詢、修改保持一致。
    // REST 上也可設計成冪等的 204，這裡選擇讓維護人員明確知道
    // 「這筆本來就不存在」，而不是誤以為自己刪掉了什麼。
    public void delete(String code) {
        currencyRepository.delete(getOrThrow(code));
    }

    private Currency getOrThrow(String code) {
        String normalized = normalize(code);
        return currencyRepository.findById(normalized)
                .orElseThrow(() -> new CurrencyNotFoundException(normalized));
    }

    // 幣別代碼一律以大寫進出系統，避免 usd / USD 被視為兩筆資料。
    private String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}
