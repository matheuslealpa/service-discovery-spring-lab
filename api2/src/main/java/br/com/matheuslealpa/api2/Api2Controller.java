package br.com.matheuslealpa.api2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class Api2Controller {

    private static final Logger log = LoggerFactory.getLogger(Api2Controller.class);

    @GetMapping("/hello")
    public String hello() {
        log.info("hello");
        return "Hello from API 2!";
    }
}
