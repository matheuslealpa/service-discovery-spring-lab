package br.com.matheuslealpa.api1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class Api1Controller {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from API 1!";
    }
}
