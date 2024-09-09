package api.finance.dto;

import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
public class FinancialResponse {

    private Chart chart;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Chart {
        private List<Result> result;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private Meta meta;
        private List<Long> timestamp;
        private Indicators indicators;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        private String symbol;
        private String longName;
        private String shortName;
        private String timezone;
        private String currency;
        private String exchangeName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Indicators {
        private List<Quote> quote;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Quote {
        private List<Double> low;
        private List<Double> open;
        private List<Double> high;
        private List<Double> close;
        private List<Long> volume;
    }

}
