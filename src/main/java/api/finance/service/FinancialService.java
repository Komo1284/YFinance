package api.finance.service;

import api.finance.dto.FinancialResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class FinancialService {

    private final RestTemplate restTemplate = new RestTemplate();

    // 저장된 기업 목록을 반환
    // 서버 메모리에 기업 목록을 저장

    public String fetchData(String symbol, long startUnixTime, long endUnixTime) {
        String url = String.format("https://query2.finance.yahoo.com/v8/finance/chart/%s.T?period1=%d&period2=%d&interval=1h",
                symbol, startUnixTime, endUnixTime);

        return restTemplate.getForObject(url, String.class);
    }

    /// 기업 코드와 이름을 저장하는 Map
    private Map<String, String> symbolToNameMap = new HashMap<>();

    // 기업 목록에 새로운 symbol 추가
    public void addSymbol(String symbol) {
        // 이미 저장된 symbol이 없다면 API 호출로 기업명 가져오기
        if (!symbolToNameMap.containsKey(symbol)) {
            String shortName = fetchShortName(symbol);  // 기업명 가져오는 메소드
            symbolToNameMap.put(symbol, shortName);  // 기업코드와 이름을 Map에 저장
        }
    }

    // 기업 목록에서 symbol 제거
    public void removeSymbol(String symbol) {
        symbolToNameMap.remove(symbol);
    }

    public String getShortName(String symbol) {
        return symbolToNameMap.getOrDefault(symbol, "");
    }

    // 저장된 기업 목록을 반환
    public Map<String, String> getSymbolsWithNames() {
        return new HashMap<>(symbolToNameMap);  // Map을 복사해서 반환
    }

    // 초기 기업 목록으로 리셋하는 메소드
    public void resetSymbols() {
        symbolToNameMap.clear();  // 기존 목록을 초기화
        // 유명한 10개 기업의 코드와 이름 예시
        symbolToNameMap.put("6758", "SONY GROUP CORPORATION");
        symbolToNameMap.put("7203", "TOYOTA MOTOR CORP");
        symbolToNameMap.put("9984", "SOFTBANK GROUP CORP");
        symbolToNameMap.put("9432", "NIPPON TEL & TEL CORP");
        symbolToNameMap.put("6861", "KEYENCE CORP");
        symbolToNameMap.put("8035", "TOKYO ELECTRON");
        symbolToNameMap.put("6954", "FANUC CORPORATION");
        symbolToNameMap.put("9983", "FAST RETAILING CO LTD");
        symbolToNameMap.put("7733", "OLYMPUS CORPORATION");
        symbolToNameMap.put("7267", "HONDA MOTOR CO");
    }

    // 기업명(shortName)을 가져오는 메소드
    private String fetchShortName(String symbol) {
        String url = String.format("https://query2.finance.yahoo.com/v8/finance/chart/%s.T", symbol);
        String response = restTemplate.getForObject(url, String.class);

        FinancialResponse financialResponse = parseResponse(response);
        return financialResponse.getChart().getResult().get(0).getMeta().getShortName();
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