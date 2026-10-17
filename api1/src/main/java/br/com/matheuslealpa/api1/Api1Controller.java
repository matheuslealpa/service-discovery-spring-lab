package br.com.matheuslealpa.api1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping
public class Api1Controller {

    private static final Logger log = LoggerFactory.getLogger(Api1Controller.class);

    private final RestClient api2;

    // Usa o RestClient.Builder padrão do Boot (já instrumentado com tracing) e adiciona
    // o interceptor de load balancing para resolver "api2" via Eureka.
    // Não declarar um bean @LoadBalanced RestClient.Builder: o cliente Eureka pega
    // qualquer RestClient.Builder do contexto e tentaria resolver "localhost" como serviço.
    public Api1Controller(RestClient.Builder builder, LoadBalancerInterceptor loadBalancer) {
        this.api2 = builder.baseUrl("http://api2").requestInterceptor(loadBalancer).build();
    }

    @GetMapping("/hello")
    public String hello() {
        log.info("hello");
        return "Hello from API 1!";
    }

    @GetMapping("/chain")
    public String chain() {
        log.info("chain -> api2");
        String reply = api2.get().uri("/hello").retrieve().body(String.class);
        return "API 1 -> " + reply;
    }
}
