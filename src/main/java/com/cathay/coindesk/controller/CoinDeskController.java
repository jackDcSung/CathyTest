package com.cathay.coindesk.controller;

import com.cathay.coindesk.client.CoinDeskResponse;
import com.cathay.coindesk.service.CoinDeskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coindesk")
public class CoinDeskController {

    private final CoinDeskService coinDeskService;

    public CoinDeskController(CoinDeskService coinDeskService) {
        this.coinDeskService = coinDeskService;
    }

    // 題目要求提供「呼叫 coindesk 的 API」，因此這是刻意的直通端點，
    // 直接回傳上游的原始結構，不做轉換。
    // 轉換後的版本另外提供，兩者用途不同。
    @GetMapping("/original")
    public CoinDeskResponse getOriginal() {
        return coinDeskService.getOriginalData();
    }
}
