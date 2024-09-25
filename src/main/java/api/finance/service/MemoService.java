package api.finance.service;

import org.springframework.stereotype.Service;

@Service
public class MemoService {
    private String memo = ""; // 메모를 저장할 서버 메모리 변수

    // 메모를 저장하는 메서드
    public void saveMemo(String newMemo) {
        this.memo = newMemo;
    }

    // 메모를 불러오는 메서드
    public String getMemo() {
        return this.memo;
    }
}