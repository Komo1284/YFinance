package api.finance.dto;

import lombok.Data;

@Data
public class FinancialDataDto {

    private String date;
    private String shortName;
    private double open;
    private double price10;
    private double price11;
    private double price13;
    private double price14;
    private double close;
    private String symbol;
}
