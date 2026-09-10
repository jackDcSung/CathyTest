package com.cathay.coindesk.controller;

import com.cathay.coindesk.dto.CurrencyRequest;
import com.cathay.coindesk.dto.CurrencyResponse;
import com.cathay.coindesk.dto.CurrencyUpdateRequest;
import com.cathay.coindesk.service.CurrencyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/currencies")
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @GetMapping
    public List<CurrencyResponse> findAll() {
        return currencyService.findAll();
    }

    @GetMapping("/{code}")
    public CurrencyResponse findByCode(@PathVariable String code) {
        return currencyService.findByCode(code);
    }

    @PostMapping
    public ResponseEntity<CurrencyResponse> create(@Valid @RequestBody CurrencyRequest request) {
        CurrencyResponse created = currencyService.create(request);
        return ResponseEntity.created(URI.create("/api/currencies/" + created.getCode())).body(created);
    }

    @PutMapping("/{code}")
    public CurrencyResponse update(@PathVariable String code,
                                   @Valid @RequestBody CurrencyUpdateRequest request) {
        return currencyService.update(code, request);
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String code) {
        currencyService.delete(code);
    }
}
