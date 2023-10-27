package com.kompanion.currency.service.impl;

import com.kompanion.currency.dto.CurrenciesDto;
import com.kompanion.currency.dto.CurrencyDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class Scheduler {

    private final Logger logger = LoggerFactory.getLogger(Scheduler.class);
    private final DateTimeFormatter saveFileFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");


    @Scheduled(fixedDelay = 43200000) // 2 times per day
    private void generate() {
        save(getMbankValues(), "mbank");
        save(getKompanionValues(), "kompanion");
        save(getAyylBankValues(), "ab");
    }

    private void save(CurrenciesDto currenciesDto, String bank) {
        String storagePath = "C:\\Users\\Tilek\\Desktop\\Java\\kompanion-task\\src\\main\\resources\\storage";

        String fileName = String.format("%s\\%s\\currency_data_%s.txt",
                storagePath, bank, LocalDateTime.now().format(saveFileFormatter));

        try (FileWriter fileWriter = new FileWriter(fileName)) {

            fileWriter.write(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) + "\n");

            for (CurrencyDto dto : currenciesDto.getCurrencies()) {
                fileWriter.write(dto.getCurrency() + "\n");
                fileWriter.write(dto.getPurchaseValue() + "\n");
                fileWriter.write(dto.getSaleValue() + "\n");
            }
        } catch (Exception e) {
            logger.info("Something went from during extraction currency information from file");
        }
    }

    public CurrenciesDto getMbankValues() {
        CurrenciesDto currenciesDto = new CurrenciesDto();

        try {
            Document document = Jsoup.connect("https://mbank.kg/en").get();

            for (Element row : document.select(
                    "div.CbkHomeExchangeRates_header__5Bm1j table tr")) {
                Elements tds = row.select("td");

                if (tds.size() >= 3) {
                    currenciesDto.getCurrencies()
                            .add(CurrencyDto.builder()
                                    .currency(tds.get(0).text())
                                    .purchaseValue(tds.get(1).text())
                                    .saleValue(tds.get(2).text())
                                    .build());
                }
            }
        } catch (Exception e) {
            logger.info("Something went from during extraction currency information from https://mbank.kg/en");
        }

        return currenciesDto;
    }

    public CurrenciesDto getKompanionValues() {
        CurrenciesDto currenciesDto = new CurrenciesDto();

        Set<String> printedCurrencies = new HashSet<>();
        try {
            Document document = Jsoup.connect("https://www.kompanion.kg/ru/").get();

            for (Element row : document.select("div.tab-content table tr")) {
                Elements tds = row.select("td");

                if (tds.size() >= 3) {
                    String currencyCode = tds.get(0).text();
                    String purchaseValue = tds.get(1).text();
                    String saleValue = tds.get(2).text();

                    if (currencyCode.length() >= 3 && !printedCurrencies.contains(currencyCode)) {
                        currenciesDto.getCurrencies()
                                .add(CurrencyDto.builder()
                                        .currency(currencyCode)
                                        .purchaseValue(purchaseValue)
                                        .saleValue(saleValue)
                                        .build());

                        printedCurrencies.add(currencyCode);
                    }
                }
            }
        } catch (Exception e) {
            logger.info("Something went from during extraction currency information from https://www.kompanion.kg/ru/");
        }

        return currenciesDto;
    }

    public CurrenciesDto getAyylBankValues() {
        CurrenciesDto currenciesDto = new CurrenciesDto();

        Set<String> currencyCodes = new HashSet<>(Arrays.asList("USD", "EUR", "RUB", "KZT"));
        try {
            Document document = Jsoup.connect("https://www.ab.kg/").get();

            for (Element row : document.select("div.tabs-descr__content.act tr")) {
                Elements tds = row.select("td");

                if (tds.size() >= 3) {
                    String currencyCode = tds.get(0).select("span.course__name").text();

                    if (currencyCodes.contains(currencyCode)) {
                        String purchaseValue = tds.get(1).text();
                        String saleValue = tds.get(2).text();

                        currenciesDto.getCurrencies()
                                .add(CurrencyDto.builder()
                                        .currency(currencyCode)
                                        .purchaseValue(purchaseValue)
                                        .saleValue(saleValue)
                                        .build());
                    }
                }
            }
        } catch (Exception e) {
            logger.info("Something went from during extraction currency information from https://www.ab.kg/");
        }

        return currenciesDto;
    }
}
