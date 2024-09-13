package api.finance.service;

import api.finance.dto.FinancialResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class FinancialService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, String> symbolToJapaneseNameMap = new HashMap<>();

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

            // 유효하지 않은 symbol일 경우 예외 발생
            if (shortName == null) {
                throw new IllegalArgumentException("존재하지 않는 기업 코드입니다.");
            }

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
    // FinancialService 클래스의 fetchShortName 메서드 수정
    private String fetchShortName(String symbol) {
        String url = String.format("https://query2.finance.yahoo.com/v8/finance/chart/%s.T", symbol);
        String response = restTemplate.getForObject(url, String.class);

        FinancialResponse financialResponse = parseResponse(response);

        // 유효하지 않은 symbol인 경우 null 반환
        if (financialResponse == null || financialResponse.getChart().getResult().isEmpty() ||
                financialResponse.getChart().getResult().get(0).getMeta().getShortName() == null) {
            return null;
        }

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

    // 일본어 기업명 로딩 메서드
    public void loadJapaneseNames() {
        try {
            // ClassPathResource로 resource/static 안에 있는 엑셀 파일을 읽음
            ClassPathResource resource = new ClassPathResource("static/japanessCode.xlsx");
            InputStream inputStream = resource.getInputStream();
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // 헤더 스킵

                Cell codeCell = row.getCell(1);
                Cell nameCell = row.getCell(2);

                String code = getCellValueAsString(codeCell);
                String japaneseName = getCellValueAsString(nameCell);

                if (code != null && japaneseName != null) {
                    symbolToJapaneseNameMap.put(code, japaneseName);
                }
            }

            workbook.close();
        } catch (IOException e) {
            // 오류 로그 출력하고 예외를 throw하지 않음
            System.err.println("Failed to load Japanese names from Excel: " + e.getMessage());
        }
    }

    // 셀의 값을 문자열로 가져오는 유틸리티 메서드
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    public String getJapaneseName(String symbol) {
        return symbolToJapaneseNameMap.getOrDefault(symbol, "");
    }

    // 서버 시작 시 데이터 로드
    @PostConstruct
    public void init() {
        loadJapaneseNames();
    }

}