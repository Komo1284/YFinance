package api.finance.controller;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@AllArgsConstructor
public class HomeController {


    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/test")
    public String test() {
        return "test";
    }

    @GetMapping("/new")
    public String new1() {
        return "new";
    }
}
