package api.finance.service;

import api.finance.dto.FinancialResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        symbolToNameMap.put("4062", "IBIDEN CO LTD");
        symbolToNameMap.put("4568", "DAIICHI SANKYO COMPANY LIMITED");
        symbolToNameMap.put("5401", "NIPPON STEEL CORPORATION");
        symbolToNameMap.put("6146", "DISCO CORPORATION");
        symbolToNameMap.put("6201", "TOYOTA INDUSTRIES CORP");
        symbolToNameMap.put("6273", "SMC CORP");
        symbolToNameMap.put("6501", "HITACHI");
        symbolToNameMap.put("6701", "NEC CORP");
        symbolToNameMap.put("6702", "FUJITSU");
        symbolToNameMap.put("6762", "TDK CORP");
        symbolToNameMap.put("6857", "ADVANTEST CORP");
        symbolToNameMap.put("6920", "LASERTEC CORP");
        symbolToNameMap.put("7011", "MITSUBISHI HEAVY INDUSTRIES");
        symbolToNameMap.put("7012", "KAWASAKI HEAVY INDUSTRIES");
        symbolToNameMap.put("7013", "IHI CORPORATION");
        symbolToNameMap.put("7735", "SCREEN HOLDINGS CO LTD");
        symbolToNameMap.put("8035", "TOKYO ELECTRON");
        symbolToNameMap.put("8766", "TOKIO MARINE HOLDINGS INC");
        symbolToNameMap.put("9101", "NIPPON YUSEN KABUSHIKI KAISHA");
        symbolToNameMap.put("9104", "MITSUI O.S.K. LINES LTD");
        symbolToNameMap.put("9501", "TOKYO ELEC POWER CO HLDGS INC");
        symbolToNameMap.put("9503", "KANSAI ELECTRIC POWER CO INC");
        symbolToNameMap.put("9843", "NITORI HOLDINGS CO LTD");
        symbolToNameMap.put("9983", "FAST RETAILING CO LTD");
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