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

        // 날짜 조정
        startDate -= 1;
        endDate += 21601;

        // 데이터 요청
        String response = financialService.fetchData(symbol, startDate, endDate);
        System.out.println(response); // 로그 확인용

        FinancialResponse financialResponse = parseResponse(response); // 응답 파싱

        List<FinancialDataDto> result = new ArrayList<>();

        // 날짜별로 처리
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        String previousDate = "";
        FinancialDataDto dto = null;

        for (int i = 0; i < financialResponse.getChart().getResult().get(0).getTimestamp().size(); i++) {
            long timestamp = financialResponse.getChart().getResult().get(0).getTimestamp().get(i);
            Date date = new Date(timestamp * 1000);

            // 날짜를 포맷하여 문자열로 변환
            String currentDate = sdf.format(date);

            // 새로운 날짜일 경우 새로운 DTO 생성
            if (!currentDate.equals(previousDate)) {
                if (dto != null) {
                    // 이전 날짜의 데이터를 result에 추가
                    result.add(dto);
                }

                dto = new FinancialDataDto();
                dto.setDate(currentDate);
                dto.setShortName(financialResponse.getChart().getResult().get(0).getMeta().getShortName());
                dto.setOpen(financialResponse.getChart().getResult().get(0).getIndicators().getQuote().get(0).getOpen().get(i)); // 시가
                previousDate = currentDate;
            }

            // 시간에 따른 데이터를 각 필드에 매핑
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

            // 마지막 시간대의 종가 설정 (예: 15:00 또는 다른 마감 시간)
            if (isEndOfDay(financialResponse, i, currentDate)) {
                dto.setClose(financialResponse.getChart().getResult().get(0).getIndicators().getQuote().get(0).getClose().get(i));
            }
        }

        // 마지막 DTO를 추가
        if (dto != null) {
            result.add(dto);
        }

        return result;
    }

    // 하루의 마지막 인덱스인지 확인하는 로직 수정
    private boolean isEndOfDay(FinancialResponse financialResponse, int index, String currentDate) {
        // 해당 인덱스의 날짜가 하루의 마지막 데이터인지 확인하는 로직
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));

        // 현재 인덱스 날짜와 다음 인덱스 날짜가 다르면 그날의 마지막 데이터임
        if (index + 1 < financialResponse.getChart().getResult().get(0).getTimestamp().size()) {
            long nextTimestamp = financialResponse.getChart().getResult().get(0).getTimestamp().get(index + 1);
            String nextDate = sdf.format(new Date(nextTimestamp * 1000));
            return !nextDate.equals(currentDate);
        }

        // 마지막 데이터일 경우
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
