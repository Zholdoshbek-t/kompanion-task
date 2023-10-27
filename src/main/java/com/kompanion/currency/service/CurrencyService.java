package com.kompanion.currency.service;

import com.kompanion.currency.dto.CurrenciesDateTimeDto;
import com.kompanion.currency.dto.CurrenciesDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CurrencyService {
    Map<String, CurrenciesDto> getCurrentValues();
    Map<String, List<CurrenciesDateTimeDto>> getCurrenciesByDate(LocalDate date);
}
