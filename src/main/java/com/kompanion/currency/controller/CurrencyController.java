package com.kompanion.currency.controller;

import com.kompanion.currency.dto.CurrenciesDateTimeDto;
import com.kompanion.currency.dto.CurrenciesDto;
import com.kompanion.currency.service.impl.CurrencyServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/currency")
public class CurrencyController {

    private final CurrencyServiceImpl currencyServiceImpl;

    // get present currency values
    @GetMapping
    public ResponseEntity<Map<String, CurrenciesDto>> getCurrentValues() {
        return ResponseEntity.ok(currencyServiceImpl.getCurrentValues());
    }

    // get currency values by date
    @GetMapping("/date/{day}/{month}/{year}")
    public ResponseEntity<Map<String, List<CurrenciesDateTimeDto>>> getCurrenciesByDate(@PathVariable int day,
                                                                                        @PathVariable int month,
                                                                                        @PathVariable int year) {
        return ResponseEntity.ok(currencyServiceImpl.getCurrenciesByDate(LocalDate.of(year, month, day)));
    }
}
