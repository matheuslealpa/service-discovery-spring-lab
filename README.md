# Service Discovery Spring Lab

Laboratório de service discovery com Spring Cloud Netflix Eureka, Spring Cloud Gateway e tracing distribuído com OpenTelemetry.

## Service discovery

Em vez de cada cliente conhecer o endereço fixo dos serviços, os serviços se registram em um servidor de registro (Eureka) ao subir e enviam heartbeats periódicos. Quem precisa chamar um serviço consulta o registro pelo nome lógico (`api1`, `api2`) e recebe as instâncias disponíveis. Isso permite portas dinâmicas, múltiplas instâncias e balanceamento de carga no lado do cliente.

## Arquitetura

```
cliente -> api-gateway (8080) -> lb://api1 --/chain--> lb://api2
                 |                     |                 |
                 +---- Eureka (8761) <-+-----------------+   (registro e descoberta)
                 |                     |                 |
                 +---- Redis (6379)    |                 |   (rate limit)
                 |                     |                 |
                 +---- Jaeger (4318) <-+-----------------+   (traces via OTLP)
```

| Módulo                         | Porta     | Função                                              |
|--------------------------------|-----------|-----------------------------------------------------|
| `service-discovery-spring-lab` | 8761      | Servidor Eureka                                     |
| `api-gateway-spring-lab`       | 8080      | Gateway com rotas via discovery e rate limit        |
| `api1`                         | aleatória | Serviço de exemplo (`server.port=0`)                |
| `api2`                         | aleatória | Serviço de exemplo (`server.port=0`)                |
| `infra/redis`                  | 6379      | Redis usado pelo `RequestRateLimiter` do gateway    |
| `infra/jaeger`                 | 16686     | Jaeger: UI de traces; recebe OTLP em 4318 (HTTP)    |

## Gateway

- `discovery.locator.enabled=true` cria uma rota para cada serviço registrado no Eureka: `/{serviceId}/**` -> `lb://{serviceId}`.
- O prefixo `/{serviceId}` é removido antes do encaminhamento. `GET /api1/hello` chega no `api1` como `GET /hello`.
- Rate limit global (`default-filters`) com `RequestRateLimiter` e Redis, por IP do cliente: 5 req/s, pico de 10. Acima disso retorna `429`.

## Tracing distribuído

Cada módulo usa `spring-boot-starter-opentelemetry` (Micrometer Tracing com bridge OpenTelemetry). Com isso:

- Toda requisição que entra cria um span; chamadas de saída (gateway -> api, api1 -> api2, gateway -> Redis) viram spans filhos.
- O contexto é propagado entre serviços pelo header W3C `traceparent`. O gateway injeta, o api1 recebe e repassa ao api2.
- Os spans são exportados via OTLP HTTP para o Jaeger em `localhost:4318`.
- Os logs ganham `[traceId-spanId]` automaticamente, então dá para cruzar log com trace.

Propriedades usadas (nomes do Spring Boot 4.1; os antigos `management.otlp.tracing.*` não têm efeito):

```properties
management.tracing.sampling.probability=1.0
management.opentelemetry.tracing.export.otlp.endpoint=http://localhost:4318/v1/traces
management.otlp.metrics.export.enabled=false
management.logging.export.otlp.enabled=false
```

Para o trace ter mais de um salto, o `api1` expõe `GET /chain`, que chama `http://api2/hello` com um `RestClient` resolvido pelo Eureka. Um trace de `GET /api1/chain` fica assim:

```
api-gateway-spring-lab  http get  /api1/chain
  api-gateway-spring-lab  evalsha                 (rate limiter no Redis)
  api-gateway-spring-lab  HTTP GET                (client -> api1)
    api1                    http get /chain
      api1                    http get  http://api2/hello
        api2                    http get /hello
```

Detalhe de implementação: o `api1` não declara um bean `@LoadBalanced RestClient.Builder`. O cliente Eureka pega qualquer `RestClient.Builder` do contexto para falar com o servidor de registro e, se ele for balanceado, tenta resolver `localhost` como serviço e o registro falha. O controller usa o builder padrão do Boot (já instrumentado) e adiciona o `LoadBalancerInterceptor` à mão.

## Requisitos

- Java 25 (ver `.sdkmanrc` em cada módulo)
- Docker

## Como rodar

```bash
# infraestrutura
docker compose up -d

# em terminais separados, nesta ordem
cd service-discovery-spring-lab && ./mvnw spring-boot:run
cd api1 && ./mvnw spring-boot:run
cd api2 && ./mvnw spring-boot:run
cd api-gateway-spring-lab && ./mvnw spring-boot:run
```

## Testando

```bash
# painel do Eureka
open http://localhost:8761

# chamadas via gateway
curl localhost:8080/api1/hello
curl localhost:8080/api2/hello

# rate limit
for i in $(seq 1 500); do curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/api1/hello; done

# tracing: gateway -> api1 -> api2 no mesmo trace
curl localhost:8080/api1/chain
open http://localhost:16686          # Jaeger UI, buscar pelo serviço api-gateway-spring-lab

# ou pela API do Jaeger (v3): serviços que exportaram spans
curl localhost:16686/api/v3/services
```
