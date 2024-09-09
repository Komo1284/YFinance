package api.finance.service;

import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    // 서버 메모리에 기업 목록을 저장하는 리스트
    private List<String> symbols = new ArrayList<>();

    // 기업 목록에 새로운 symbol 추가
    public void addSymbol(String symbol) {
        if (!symbols.contains(symbol)) {
            symbols.add(symbol);  // 중복되지 않으면 추가
        }
    }

    // 기업 목록에서 symbol 제거
    public void removeSymbol(String symbol) {
        symbols.remove(symbol);
    }

    // 저장된 기업 목록을 반환
    public List<String> getSymbols() {
        return new ArrayList<>(symbols);  // 변경 방지를 위해 복사본 반환
    }

}