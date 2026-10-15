# Service Discovery Spring Lab

Laboratório de service discovery com Spring Cloud Netflix Eureka e Spring Cloud Gateway.

## Service discovery

Em vez de cada cliente conhecer o endereço fixo dos serviços, os serviços se registram em um servidor de registro (Eureka) ao subir e enviam heartbeats periódicos. Quem precisa chamar um serviço consulta o registro pelo nome lógico (`api1`, `api2`) e recebe as instâncias disponíveis. Isso permite portas dinâmicas, múltiplas instâncias e balanceamento de carga no lado do cliente.

## Arquitetura

```
cliente -> api-gateway (8080) -> lb://api1 | lb://api2
                 |                     |
                 +---- Eureka (8761) <-+   (registro e descoberta)
                 |
                 +---- Redis (6379)        (rate limit)
```

| Módulo                         | Porta     | Função                                              |
|--------------------------------|-----------|-----------------------------------------------------|
| `service-discovery-spring-lab` | 8761      | Servidor Eureka                                     |
| `api-gateway-spring-lab`       | 8080      | Gateway com rotas via discovery e rate limit        |
| `api1`                         | aleatória | Serviço de exemplo (`server.port=0`)                |
| `api2`                         | aleatória | Serviço de exemplo (`server.port=0`)                |
| `infra/redis`                  | 6379      | Redis usado pelo `RequestRateLimiter` do gateway    |

## Gateway

- `discovery.locator.enabled=true` cria uma rota para cada serviço registrado no Eureka: `/{serviceId}/**` -> `lb://{serviceId}`.
- O prefixo `/{serviceId}` é removido antes do encaminhamento. `GET /api1/hello` chega no `api1` como `GET /hello`.
- Rate limit global (`default-filters`) com `RequestRateLimiter` e Redis, por IP do cliente: 5 req/s, pico de 10. Acima disso retorna `429`.

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
```
