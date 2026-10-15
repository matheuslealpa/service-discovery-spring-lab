package br.com.matheuslealpa.api2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class Api2Application {

    public static void main(String[] args) {
        SpringApplication.run(Api2Application.class, args);
    }

}

