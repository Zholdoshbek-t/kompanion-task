package com.kompanion.currency.service.impl;

import com.kompanion.currency.dto.CurrenciesDateTimeDto;
import com.kompanion.currency.dto.CurrenciesDto;
import com.kompanion.currency.dto.CurrencyDto;
import com.kompanion.currency.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CurrencyServiceImpl implements CurrencyService {

    private final DateTimeFormatter targetDateFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");
    private final Logger logger = LoggerFactory.getLogger(CurrencyServiceImpl.class);
    private final Scheduler scheduler;


    @Override
    public Map<String, CurrenciesDto> getCurrentValues() {
        Map<String, CurrenciesDto> map = new HashMap<>();

        map.put("Kompanion Bank", scheduler.getKompanionValues());
        map.put("MBank", scheduler.getMbankValues());
        map.put("Ayyl Bank", scheduler.getAyylBankValues());

        return map;
    }

    @Override
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
