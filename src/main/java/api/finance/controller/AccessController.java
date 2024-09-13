package api.finance.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AccessController {

    @GetMapping("/test")
    public String test() {
        return "test";
    }

    // 비밀번호 페이지로 이동
    @GetMapping("/access")
    public String accessPage() {
        return "access"; // access.html 페이지로 이동
    }

    // 비밀번호 입력 처리
    @PostMapping("/verify")
    public String verifyCode(@RequestParam("code") String code, HttpSession session, Model model) {
        // 여기서 임의의 코드 확인
        String validCode = "!ezfinance@"; // 임의로 설정한 코드
        if (validCode.equals(code)) {
            // 세션에 접근 허용 정보 저장
            session.setAttribute("accessAllowed", true);
            return "redirect:/protected"; // 성공하면 보호된 페이지로 리다이렉트
        } else {
            // 비밀번호 틀렸을 경우
            model.addAttribute("error", "無効なコードです。もう一度入力してください。");
            return "access"; // 다시 비밀번호 입력 페이지로 이동
        }
    }

    // 보호된 페이지로 이동
    @GetMapping("/protected")
    public String protectedPage(HttpSession session) {
        // 세션에서 접근 허용 정보 확인
        if (Boolean.TRUE.equals(session.getAttribute("accessAllowed"))) {
            return "protected"; // 보호된 페이지로 이동
        }
        return "redirect:/access"; // 세션 정보 없으면 비밀번호 입력 페이지로 리다이렉트
    }
}