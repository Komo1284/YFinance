package api.finance.controller;

import api.finance.service.MemoService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/memo")
@AllArgsConstructor
public class MemoController {

    private final MemoService memoService;

    // 메모를 저장하는 API
    @PostMapping("/save")
    public void saveMemo(@RequestParam String memo) {
        memoService.saveMemo(memo);
    }

    // 메모를 불러오는 API
    @GetMapping("/get")
    public String getMemo() {
        return memoService.getMemo();
    }
}