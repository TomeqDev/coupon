# Coupon

Mikroserwis REST-owy do zarządzania kuponami z limitem użycia oraz ograniczeniem kraju

## Uruchomienie

### Wymagania
- Java 21
- Docker dla bazy PostgreSQL (w testach integracyjnych, opcjonalne, gdy brak dockera to używany jest h2db)

### Start bazy danych
```bash
docker-compose up -d
```

### Uruchomienie aplikacji
```bash
./gradlew bootRun
```

### Uruchomienie testów
```bash
./gradlew test              # testy jednostkowe
./gradlew integrationTest   # testy integracyjne (Docker opcjonalnie, fall back do H2)
```

### Dokumentacja API
Dostępna pod: http://localhost:8080/swagger-ui.html

---

## Decyzje technologiczne i architektoniczne

### Core

| Technologia             | Komentarz                                                                                                        |
|-------------------------|------------------------------------------------------------------------------------------------------------------|
| **Java 21**             | Wersja LTS zawierająca rekordy.                                                                                  |
| **Spring Boot 3.2.5**   | Szybki do budowania mikroserwisów opartych na java i REST. Atut szybkiej konfiguracji, wstrzykiwania zależności. |
| **Gradle (Kotlin DSL)** | Budowanie. Kotlin DSL łapie błędy konfiguracji podczas kompilacji.                                               |

### Architektura

| Decyzja                        | Komentarz                                                                                                                                                                                                          |
|--------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Hexagonal Architecture**     | Oddzielenie logiki biznesowej od infrastruktury. Niezależność od frameworków. Zwiększa niezależność, łatwość testowania i zmianę frameworków.                                                                      |
| **DDD (Domain-Driven Design)** | Modele domenowe (`Coupon`, `CouponUsage`) ukrywają reguły biznesowe. Porty umożliwiają niezależność od szczegółów implementacyjnych.                                                                               |
| **Use Case pattern**           | Serwisy w warstwie aplikacji (`CreateCouponUseCase`, `RegisterCouponUsageUseCase`) reprezentują pojedynczą operację biznesową. Co zachowuje Single Responsibility Principle i ułatwia poruszanie się po projekcie. |

### Baza danych

| Technologia                | Komentarz                                                                                                                                                                                                    |
|----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **PostgreSQL**             | Powszechnie używana, darmowa baza danych. Zgodna ze standardem ACID relacyjna baza danych. Odpowiednia do transakcyjnego przetważania używania kuponów w wielowątkowym środowisku.                           |
| **Spring Data JPA**        | Zmniejsza boilerplate dla operacji CRUD. Własne `@Query` pozwala na atomową warunkową inkrementację (`incrementUsage`) bez niepotrzebnego w naszym case pessimistic locking.                                 |
| **Liquibase**              | Zarządzanie skryptami bazy danych. Bezpieczne w użyciu, wersjonowanie zmian bazy danych. Bezpieczne rozwiązanie do zachowania struktury bazy danych. Zmiany dobrze widoczne między poszczególnymi commitami. |
| **Oddzielne JPA entities** | Model w domenie czysty, bez annotacji i uzależnienia od frameworka. MapStruct użyty do konwertowania między domeną, a encjami. Zgodne z architekturą hexagonalną.                                            |

### Wielowątkowość

| Decyzja                                    | Komentarz                                                                                                                                                                                        |
|--------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Atomic conditional UPDATE**              | `UPDATE ... WHERE currentUsages < maxUsages` inkrementacja użycia kuponu w jednym zapytaniu SQL eliminuje race-condition bez locków. Zwracana wartość (0 lub 1) pokazuje czy update się powiódł. |
| **Unique constraint (coupon_id, user_id)** | Zapobiega duplikatom na poziomie bazy danych nawet podczas równoczesnych żądań.                                                                                                                  |

### Odporność

| Technologia                        | Komentarz                                                                                                                                                                               |
|------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Resilience4j**                   | Lekka biblioteka dla Javy. Łatwa do użycia w Spring Boot przez annotacje. Brak konieczności zewnętrznej infrastruktury.                                                                 |
| **Circuit Breaker**                | Zapobiega masowym błędom gdy zewnętrzny serwis (tutaj ip-api.com) nie jest dostępny. Po powtarzających się błędach serwis jest zablokowany.                                             |
| **Retry with exponential backoff** | Wydłużanie oczekiwania na ponowny retry. Zastosowany mnożnik 2x. Zmniejsza presję i ilość ponownych retry w krótkim czasie.                                                             |
| **Configurable aspect order**      | CircuitBreaker (zewnętrzny) → Retry (wewnętrznie) zapewnia, że ponowienia będą wykonane przed otwarciem Circuit Braker. |

### Obserwowalność

| Technologia                         | Komentarz                                                                                                                                                                           |
|-------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Micrometer + Brave**              | Distributed tracing with W3C Trace Context propagation. Correlates requests across microservices via `traceId`. Zero-code instrumentation for RestClient, JDBC, and Spring MVC.     |
| **Trace ID in responses**           | `X-Trace-Id` w nagłówku i pole `traceId` w odpowiedzi na błąd. Pozwala na łatwiejsze debugowanie z użyciem correlation id.                                                          |
| **Structured logging with traceId** | Szablon logów zawiera `[traceId, spanId]`, do łączenia logów.                                                                                                                       |
| **Custom Micrometer counters**      | Metryki biznesowe (`coupon.created`, `coupon.usage.success`, `coupon.usage.failed`) pozwalają an monitoring i ewentualne alertowanie w przypadku zbyt dużej ilości błędnych wykonań. |
| **Spring Actuator**                 | Standardowe endpointy `/health`, `/metrics`, `/prometheus` do monitorowania i zarządania.                                                                                           |

### Walidacja

| Technologia                     | Komentarz                                                                                                                                                            |
|--------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Jakarta Bean Validation**    | Walidacja na API DTOs. Oddziela walidację danych wejściowych od reguł biznesowych.                                                                                   |
| **Custom `@ValidCountryCode`** | Własny walidator do kodó krajów zgodnych z ISO 3166-1 alpha-2 z `Locale.getISOCountries()`. Zapewnia, że zapiszemy kody krajów zgodne z użytym ip-api.com. |

### Integracja z zewnętrznymi usługami

| Decyzja                                   | Komentarz                                                                                                                                                                                                                                                   |
|-------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Port interface (`GeoLocationService`)** | Abstrakcja dla providera geolokacjiza portem domenowym. Ułatwia testowanie z użyciem mocków, wymianę prowidera i oddziela logikę aplikacji. |
| **ip-api.com**                            | Darmowe API zwracające kody ISO kraju. Wyciągane za portem, przez co można zastąpić innym dostawcą bez zmian w kodzie.                                                                                                                                      |
| **Configurable URL**                      | `geolocation.url` konfigurowalne property pozwala łatwo testować z użyciem WireMock bez wpływu na produkcyjny adres bez zmian w kodzie.                                                                                                                     |

### Dokumentacja API

| Technologia                        | Komentarz                                                                                                                                                                                                      |
|------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **SpringDoc OpenAPI (Swagger UI)** | Automatycznie generowana specyfikacja OpenAPI 3.0 na podstawie annotacji z kontrolerów. Łatwe w użyciu UI dla developerów i QA. Ułatwia dzielenie wiedzą między zespołami developerskimi, a także testującymi. |

### Testowanie

| Technologia               | Komentarz                                                                                                                                                                                                       |
|--------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **JUnit 5**              | Powszechny framework do testów. Użyto annotację (`@Nested`), oraz display names do grupowania i przejrzystości testów. Dodatkowo testy sparametryzowane by wykonać ten sam test dla różnych danych wejściowych. |
| **Testcontainers**       | Uruchomienie testów integracyjnych z prawdziwą bazą PostgreSQL w Dockerze. Pozwala zbliżyć infrastrukturę środowiska testowego do tego co będzie na produkcji.                                                  |
| **H2 fallback**          | Gdy Docker nie jest dostępny (u mnie docker działał, ale przy uruchamianiu testów był problem z jego widocznością), testy są uruchamiane z pamięciową bazą H2.                                                  |
| **WireMock**             | Symuluje zewnętrzne usługi HTTP na poziomie sieci. Użyty do sprawdzenia retry i CircuitBreaker bez konieczności wywoływania rzeczywistego API.                                                                  |
| **Separate source sets** | `src/test` dla testów jednostkowych, `src/testIntegration` dla testów integracyjnych. Inne profile wykonania. Nie zawsze chcemy uruchamiać testy jednostkowe razem z integracyjnymi.                            |
| **MockMvc**              | Testy REST kontrolera z pełnym stackiem Springowym MVC (filters, validation, exception handling) bez startowania rzeczywistego serwera HTTP. Szybkie i przewidywalne.                                           |

### Infrastruktura

| Technologia        | Komentarz                                                                                                                                         |
|--------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| **Docker Compose** | Pojedyncza komenda (`docker-compose up`) do wystartowania lokalnie aplikacji (tutaj tylko do bazy danych). Uniezależnienie od środowiska.         |
| **Lombok**         | Eliminacja boilerplate. Kod czytelniejszy nastawiony głównie na logikę.                                                                           |
| **MapStruct**      | Podczas kompilacji generuje kod do mapowania obiektów. Szybkie i wykrywa błędy mapowania podczas budowania. |

---

## Struktura projektu

```
src/main/java/com/task/
├── api/                    # Wewnętrze adaptey (REST kontrolery, DTOs, walidacje, filtry)
├── application/            # Use casy, porty, wyjątki
├── domain/                 # Model domeny i interfejsy do repozytoriów
└── infrastructure/         # Zewnętrzne adaptery (JPA, geolocation HTTP client)
```

## Endpointy API

| HTTP Method | Ścieżka                      | Opis                                        |
|-------------|------------------------------|---------------------------------------------|
| POST        | `/api/coupons`               | Tworzenie nowego kuponu                     |
| POST        | `/api/coupons/{code}/usages` | Rejestracja użycia kuponu przez użytkownika |
