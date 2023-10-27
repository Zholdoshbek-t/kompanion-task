package com.kompanion.currency.service;

import com.kompanion.currency.dto.CurrenciesDateTimeDto;
import com.kompanion.currency.dto.CurrenciesDto;
import com.kompanion.currency.dto.CurrencyDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CurrencyService {

    private final Logger logger = LoggerFactory.getLogger(CurrencyService.class);
    private final DateTimeFormatter saveFileFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
    private final DateTimeFormatter targetDateFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");

    @Scheduled(fixedDelay = 43200000) // 2 times per day
    public void generate() {
        save(getMbankValues(), "mbank");
        save(getKompanionValues(), "kompanion");
        save(getAyylBankValues(), "ab");
    }

    private void save(CurrenciesDto currenciesDto, String bank) {
        String storagePath = "C:\\Users\\Tilek\\Desktop\\Java\\kompanion-task\\src\\main\\resources\\storage";

        String fileName = String.format("%s\\%s\\currency_data_%s.txt",
                storagePath, bank, LocalDateTime.now().format(saveFileFormatter));

        try (FileWriter fileWriter = new FileWriter(fileName)) {

            fileWriter.write(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));

            for (CurrencyDto dto : currenciesDto.getCurrencies()) {
                fileWriter.write(dto.getCurrency() + "\n");
                fileWriter.write(dto.getPurchaseValue() + "\n");
                fileWriter.write(dto.getSaleValue() + "\n");
            }
        } catch (Exception e) {
            logger.info("Something went from during extraction currency information from file");
        }
    }

    private CurrenciesDto getMbankValues() {
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

    private CurrenciesDto getKompanionValues() {
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

    private CurrenciesDto getAyylBankValues() {
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

    public Map<String, CurrenciesDto> getCurrentValues() {
        Map<String, CurrenciesDto> map = new HashMap<>();

        map.put("Kompanion Bank", getKompanionValues());
        map.put("MBank", getMbankValues());
        map.put("Ayyl Bank", getAyylBankValues());

        return map;
    }

    public Map<String, List<CurrenciesDateTimeDto>> getCurrenciesByDate(LocalDate date) {
        Map<String, List<CurrenciesDateTimeDto>> bankCurrenciesDateTimeMap = new HashMap<>();

        List<CurrenciesDateTimeDto> kompanion = getCurrenciesByDateBank("kompanion", date);
        List<CurrenciesDateTimeDto> mbank = getCurrenciesByDateBank("mbank", date);
        List<CurrenciesDateTimeDto> ab = getCurrenciesByDateBank("ab", date);

        bankCurrenciesDateTimeMap.put("Kompanion Bank", kompanion);
        bankCurrenciesDateTimeMap.put("MBank", mbank);
        bankCurrenciesDateTimeMap.put("Ayyl Bank", ab);

        return bankCurrenciesDateTimeMap;
    }

    private List<CurrenciesDateTimeDto> getCurrenciesByDateBank(String bank, LocalDate date) {
        List<CurrenciesDateTimeDto> currenciesDateTimeDtos = new ArrayList<>();

        String directoryPath = "C:\\Users\\Tilek\\Desktop\\Java\\kompanion-task\\src\\main\\resources\\storage\\" + bank;

        FileFilter dateFilter = file -> file.getName().contains(date.format(targetDateFormatter));

        File directory = new File(directoryPath);

        File[] matchingFiles = directory.listFiles(dateFilter);

        if (matchingFiles != null && matchingFiles.length > 0) {
            for (File file : matchingFiles) {
                try {
                    currenciesDateTimeDtos.add(readFile(new FileReader(file)));
                } catch (FileNotFoundException e) {
                    logger.info("File was not found");
                }
            }
        } else {
            logger.info("Files with the given date were not found");
        }

        return currenciesDateTimeDtos;
    }

    private CurrenciesDateTimeDto readFile(FileReader fileReader) {
        CurrenciesDateTimeDto currenciesDateTimeDto = new CurrenciesDateTimeDto();

        CurrenciesDto currenciesDto = new CurrenciesDto();

        try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            String dateTime;
            String currencyCode;
            String purchaseValue;
            String saleValue;

            if ((dateTime = bufferedReader.readLine()) != null) currenciesDateTimeDto.setDateTime(dateTime);

            while ((currencyCode = bufferedReader.readLine()) != null) {
                purchaseValue = bufferedReader.readLine();
                saleValue = bufferedReader.readLine();

                if (purchaseValue != null && saleValue != null) {
                    currenciesDto.getCurrencies()
                            .add(CurrencyDto.builder()
                                    .currency(currencyCode)
                                    .purchaseValue(purchaseValue)
                                    .saleValue(saleValue)
                                    .build());
                }
            }
        } catch (IOException e) {
            logger.info("Something went wrong during extraction currency information from file");
        }

        currenciesDateTimeDto.setCurrenciesDto(currenciesDto);

        return currenciesDateTimeDto;
    }
}
