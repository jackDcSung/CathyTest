package com.cathay.coindesk.repository;

import com.cathay.coindesk.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CurrencyRepository extends JpaRepository<Currency, String> {

    List<Currency> findAllByOrderByCodeAsc();

    // 一次撈回轉換所需的幣別，避免在迴圈中逐筆查詢造成 N+1。
    List<Currency> findByCodeIn(Collection<String> codes);
}
