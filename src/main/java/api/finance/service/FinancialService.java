package api.finance.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FinancialService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String fetchData(String symbol, long startUnixTime, long endUnixTime) {
        String url = String.format("https://query2.finance.yahoo.com/v8/finance/chart/%s.T?period1=%d&period2=%d&interval=1h",
                symbol, startUnixTime, endUnixTime);

        return restTemplate.getForObject(url, String.class);
    }
}