package api.finance.controller;

import api.finance.dto.FinancialDataDto;
import api.finance.dto.FinancialResponse;
import api.finance.service.FinancialService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

@RestController
public class FinancialController {

    private final FinancialService financialService;

    public FinancialController(FinancialService financialService) {
        this.financialService = financialService;
    }

    @GetMapping("/financial-data")
    @ResponseBody
    public List<FinancialDataDto> getFinancialData(
            @RequestParam String symbol,
            @RequestParam long startDate,
            @RequestParam long endDate) {

        startDate -= 1;  // 시작 날짜 조정
        endDate += 21601;  // 종료 날짜 조정

        String[] symbols = symbol.split(",");
        List<FinancialDataDto> result = new ArrayList<>();

        // 현재 날짜 타임스탬프를 얻음
        long currentDateTimestamp = System.currentTimeMillis() / 1000;

        for (String sym : symbols) {
            String response = financialService.fetchData(sym, startDate, endDate);
            FinancialResponse financialResponse = parseResponse(response);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
            String previousDate = "";
            FinancialDataDto dto = null;

            for (int i = 0; i < financialResponse.getChart().getResult().get(0).getTimestamp().size(); i++) {
                long timestamp = financialResponse.getChart().getResult().get(0).getTimestamp().get(i);

                // 현재 날짜의 데이터는 건너뜀
                if (timestamp >= currentDateTimestamp) {
                    continue;
                }

                Date date = new Date(timestamp * 1000);
                String currentDate = sdf.format(date);

                // 새로운 날짜의 데이터이면 DTO 생성
                if (!currentDate.equals(previousDate)) {
                    if (dto != null) {
                        // 기존 DTO를 결과에 추가
                        if (isDtoContainsZeroValues(dto)) {
                            result.add(createSeparatorDto());
                        } else {
                            result.add(dto);
                        }
                    }

                    dto = new FinancialDataDto();
                    dto.setDate(currentDate);
                    dto.setShortName(financialResponse.getChart().getResult().get(0).getMeta().getShortName());
                    dto.setOpen(financialResponse.getChart().getResult().get(0).getIndicators().getQuote().get(0).getOpen().get(i));
                    previousDate = currentDate;
                }

                // 시간대별 가격 설정
                String timeString = new SimpleDateFormat("HH:mm").format(date);
                if (timeString.equals("10:00")) {
                    dto.setPrice10(findPriceAtTime(financialResponse, i));
                } else if (timeString.equals("11:00")) {
                    dto.setPrice11(findPriceAtTime(financialResponse, i));
                } else if (timeString.equals("13:00")) {
                    dto.setPrice13(findPriceAtTime(financialResponse, i));
                } else if (timeString.equals("14:00")) {
                    dto.setPrice14(findPriceAtTime(financialResponse, i));
                }

                // 종료 시간대의 종가 설정
                if (isEndOfDay(financialResponse, i, currentDate, endDate * 1000)) {
                    dto.setClose(financialResponse.getChart().getResult().get(0).getIndicators().getQuote().get(0).getClose().get(i));
                }
            }

            // 마지막 데이터의 값을 구분선으로 변경
            if (dto != null) {
                if (isDtoContainsZeroValues(dto)) {
                    result.add(createSeparatorDto());
                } else {
                    result.add(dto);
                    result.add(createSeparatorDto());
                }
            }
        }

        return result;
    }

    // DTO의 중간 값이 모두 0인지 확인
    private boolean isDtoContainsZeroValues(FinancialDataDto dto) {
        return  dto.getPrice10() == 0.0 &&
                dto.getPrice11() == 0.0 &&
                dto.getPrice13() == 0.0 &&
                dto.getPrice14() == 0.0;
    }

    // 구분선 DTO 생성
    private FinancialDataDto createSeparatorDto() {
        FinancialDataDto separatorDto = new FinancialDataDto();
        separatorDto.setDate("-----------");
        separatorDto.setShortName("-----------");
        separatorDto.setOpen(0.0);
        separatorDto.setPrice10(0.0);
        separatorDto.setPrice11(0.0);
        separatorDto.setPrice13(0.0);
        separatorDto.setPrice14(0.0);
        separatorDto.setClose(0.0);
        return separatorDto;
    }

    // 하루의 마지막 인덱스인지 확인하는 로직
    private boolean isEndOfDay(FinancialResponse financialResponse, int index, String currentDate, long endDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));

        // 현재 인덱스의 타임스탬프
        long currentTimestamp = financialResponse.getChart().getResult().get(0).getTimestamp().get(index);

        // 현재 날짜가 endDate보다 뒤일 경우 그 데이터를 무시 (오늘 날짜 추가 방지)
        if (currentTimestamp * 1000 > endDate) {
            return true; // 오늘 날짜 이후 데이터는 추가하지 않음
        }

        // 다음 인덱스가 존재하는 경우에만 비교
        if (index + 1 < financialResponse.getChart().getResult().get(0).getTimestamp().size()) {
            long nextTimestamp = financialResponse.getChart().getResult().get(0).getTimestamp().get(index + 1);
            String nextDate = sdf.format(new Date(nextTimestamp * 1000));
            return !nextDate.equals(currentDate);  // 다음 데이터의 날짜가 다르면 오늘 마지막
        }

        // 마지막 인덱스일 경우
        return true;
    }

    private double findPriceAtTime(FinancialResponse financialResponse, int index) {
        // 시간대별로 가장 가까운 가격을 반환하는 로직
        return financialResponse.getChart().getResult().get(0).getIndicators().getQuote().get(0).getOpen().get(index);
    }

    private FinancialResponse parseResponse(String response) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(response, FinancialResponse.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}