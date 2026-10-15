package br.com.matheuslealpa.apigatewayspringlab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RateLimitAuditFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAuditFilter.class);
    private static final String RANKING_KEY = "rate_limit:blocked";

    private final ReactiveStringRedisTemplate redis;

    public RateLimitAuditFilter(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.defer(() -> {
            if (exchange.getResponse().getStatusCode() != HttpStatus.TOO_MANY_REQUESTS) {
                return Mono.empty();
            }
            String ip = RateLimitConfig.clientIp(exchange.getRequest());
            log.warn("429 ip={} path={}", ip, exchange.getRequest().getPath());
            return redis.opsForZSet().incrementScore(RANKING_KEY, ip, 1).then();
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
