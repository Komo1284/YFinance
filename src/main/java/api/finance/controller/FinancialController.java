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
        for (int i = 0; i < financialResponse.getChart().getResult().get(0).getTimestamp().size(); i++) {
            long timestamp = financialResponse.getChart().getResult().get(0).getTimestamp().get(i);
            Date date = new Date(timestamp * 1000);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));

            // 날짜를 포맷하여 문자열로 변환
            String dateString = sdf.format(date);

            // 해당 날짜의 데이터
            double open = financialResponse.getChart().getResult().get(0).getIndicators().getQuote().get(0).getOpen().get(i);
            double close = financialResponse.getChart().getResult().get(0).getIndicators().getQuote().get(0).getClose().get(i);

            FinancialDataDto dto = new FinancialDataDto();
            dto.setDate(dateString);
            dto.setShortName(financialResponse.getChart().getResult().get(0).getMeta().getShortName());

            // 시가
            dto.setOpen(open);
            // 시간대별 가격 (예시: 가장 가까운 시간대의 가격을 설정)
            dto.setPrice10(findPriceAtTime(financialResponse, i, "10:00"));
            dto.setPrice11(findPriceAtTime(financialResponse, i, "11:00"));
            dto.setPrice13(findPriceAtTime(financialResponse, i, "13:00"));
            dto.setPrice14(findPriceAtTime(financialResponse, i, "14:00"));
            // 종가
            dto.setClose(close);

            result.add(dto);
        }

        return result;
    }

    private double findPriceAtTime(FinancialResponse financialResponse, int index, String time) {
        // 시간대별 가격 결정 로직 구현
        // 예를 들어, 가장 가까운 시간대의 가격을 반환
        return financialResponse.getChart().getResult().get(0).getIndicators().getQuote().get(0).getClose().get(index); // Placeholder
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
