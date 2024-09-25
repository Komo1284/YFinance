package api.finance.controller;

import api.finance.dto.FinancialDataDto;
import api.finance.dto.FinancialResponse;
import api.finance.service.FinancialService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@AllArgsConstructor
public class FinancialController {

    private final FinancialService financialService;

    @PostMapping("/add-symbol")
    public ResponseEntity<String> addSymbol(@RequestParam String symbol) {
        try {
            financialService.addSymbol(symbol);
            return ResponseEntity.ok("Symbol added successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/get-symbol-name")
    @ResponseBody
    public String getSymbolName(@RequestParam String symbol) {
        return financialService.getShortName(symbol);
    }

    // 기업 목록에서 symbol 제거하는 API
    @PostMapping("/remove-symbol")
    public void removeSymbol(@RequestParam String symbol) {
        financialService.removeSymbol(symbol);
    }

    // 저장된 기업 목록을 조회하는 API (기업명과 함께 반환)
    @GetMapping("/get-symbols")
    public Map<String, String> getSymbolsWithNames() {
        return financialService.getSymbolsWithNames();
    }

    @GetMapping("/get-japanese-name")
    public String getJapaneseName(@RequestParam String symbol) {
        return financialService.getJapaneseName(symbol);
    }

    // 초기 기업 목록을 설정하는 리셋 API
    @PostMapping("/reset-symbols")
    public void resetSymbols() {
        financialService.resetSymbols();  // 초기 기업 목록으로 리셋
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));

        for (String sym : symbols) {
            String response = financialService.fetchData(sym, startDate, endDate);
            FinancialResponse financialResponse = parseResponse(response);

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

                    // 기업명(shortName)을 일본어로 설정
                    String japaneseName = financialService.getJapaneseName(sym);
                    dto.setShortName(japaneseName.isEmpty() ? financialResponse.getChart().getResult().get(0).getMeta().getShortName() : japaneseName);

                    dto.setOpen(financialResponse.getChart().getResult().get(0).getIndicators().getQuote().get(0).getOpen().get(i));
                    dto.setSymbol(sym);
                    previousDate = currentDate;
                }

                // 시간대별 가격 설정 (서버 시간대 - 배포시)
                String timeString = new SimpleDateFormat("HH:mm").format(date);
                if (timeString.equals("01:00")) {
                    dto.setPrice10(findPriceAtTime(financialResponse, i));
                } else if (timeString.equals("02:00")) {
                    dto.setPrice11(findPriceAtTime(financialResponse, i));
                } else if (timeString.equals("04:00")) {
                    dto.setPrice13(findPriceAtTime(financialResponse, i));
                } else if (timeString.equals("05:00")) {
                    dto.setPrice14(findPriceAtTime(financialResponse, i));
                }

//                // 시간대별 가격 설정 (로컬 시간대)
//                String timeString = new SimpleDateFormat("HH:mm").format(date);
//                if (timeString.equals("10:00")) {
//                    dto.setPrice10(findPriceAtTime(financialResponse, i));
//                } else if (timeString.equals("11:00")) {
//                    dto.setPrice11(findPriceAtTime(financialResponse, i));
//                } else if (timeString.equals("13:00")) {
//                    dto.setPrice13(findPriceAtTime(financialResponse, i));
//                } else if (timeString.equals("14:00")) {
//                    dto.setPrice14(findPriceAtTime(financialResponse, i));
//                }

                // 현재 날짜와 이전 날짜를 구분하여 종가 설정
                if (currentDate.equals(sdf.format(new Date()))) {  // 현재 날짜일 때
                    // 오후 3시 이전인지 확인하고 종가를 출력하지 않음
                    Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
                    int hour = now.get(Calendar.HOUR_OF_DAY);
                    if (hour >= 15 && isEndOfDay(financialResponse, i, currentDate, endDate * 1000)) {
                        dto.setClose(financialResponse.getChart().getResult().get(0).getIndicators().getQuote().get(0).getClose().get(i));
                    }
                } else {  // 과거 날짜일 때
                    // 정상적으로 종가를 설정
                    if (isEndOfDay(financialResponse, i, currentDate, endDate * 1000)) {
                        dto.setClose(financialResponse.getChart().getResult().get(0).getIndicators().getQuote().get(0).getClose().get(i));
                    }
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

    // 장 종료 시간(오후 3시) 이후인지 확인하는 메서드
    private boolean isAfterMarketClose() {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);

        // 오후 3시(15:00) 이후인지 확인
        return hour > 15 || (hour == 15 && minute > 0);
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
        separatorDto.setDate("-----------------");
        separatorDto.setShortName("----------------------------------------------");
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