package com.task.infrastructure.geolocation;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.task.application.exception.GeoLocationException;
import com.task.application.port.GeoLocationService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("GeoLocation Resilience4j integration")
class GeoLocationResilienceIntegrationTest {

    private static final String IP = "185.0.0.1";
    private static final String GEO_PATH = "/json/" + IP + "?fields=countryCode";

    private static WireMockServer wireMockServer;

    @Autowired
    private GeoLocationService geoLocationService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("geolocation.url", () ->
                "http://localhost:" + wireMockServer.port() + "/json/{ip}?fields=countryCode");
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:resilience_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.H2Dialect");
    }

    @BeforeEach
    void resetState() {
        wireMockServer.resetAll();
        circuitBreakerRegistry.circuitBreaker("geolocation").reset();
    }

    @Nested
    @DisplayName("Retry with exponential backoff")
    class RetryBehavior {

        @Test
        @DisplayName("should retry on server error and succeed on subsequent attempt")
        void shouldRetryAndSucceed() {
            wireMockServer.stubFor(get(urlEqualTo(GEO_PATH))
                    .inScenario("retry")
                    .whenScenarioStateIs("Started")
                    .willReturn(aResponse().withStatus(500))
                    .willSetStateTo("second-attempt"));

            wireMockServer.stubFor(get(urlEqualTo(GEO_PATH))
                    .inScenario("retry")
                    .whenScenarioStateIs("second-attempt")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"countryCode\":\"PL\"}")));

            String result = geoLocationService.getCountryCode(IP);

            assertThat(result).isEqualTo("PL");
            wireMockServer.verify(2, getRequestedFor(urlEqualTo(GEO_PATH)));
        }

        @Test
        @DisplayName("should exhaust retries and throw after max attempts")
        void shouldExhaustRetriesAndThrow() {
            wireMockServer.stubFor(get(urlEqualTo(GEO_PATH))
                    .willReturn(aResponse().withStatus(500)));

            assertThatThrownBy(() -> geoLocationService.getCountryCode(IP))
                    .isInstanceOf(GeoLocationException.class);

            wireMockServer.verify(3, getRequestedFor(urlEqualTo(GEO_PATH)));
        }
    }

    @Nested
    @DisplayName("Circuit breaker")
    class CircuitBreakerBehavior {

        @Test
        @DisplayName("should open circuit after repeated failures")
        void shouldOpenCircuitAfterRepeatedFailures() {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("geolocation");

            wireMockServer.stubFor(get(urlEqualTo(GEO_PATH))
                    .willReturn(aResponse().withStatus(500)));

            for (int i = 0; i < 5; i++) {
                try {
                    geoLocationService.getCountryCode(IP);
                } catch (GeoLocationException ignored) {
                }
            }

            assertThat(circuitBreaker.getState())
                    .isEqualTo(CircuitBreaker.State.OPEN);
        }

        @Test
        @DisplayName("should reject calls immediately when circuit is open")
        void shouldRejectWhenCircuitOpen() {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("geolocation");
            circuitBreaker.transitionToOpenState();

            assertThatThrownBy(() -> geoLocationService.getCountryCode(IP))
                    .isInstanceOf(GeoLocationException.class)
                    .hasMessageContaining("circuit open");

            wireMockServer.verify(0, getRequestedFor(urlEqualTo(GEO_PATH)));
        }

        @Test
        @DisplayName("should allow calls when circuit is closed")
        void shouldAllowCallsWhenCircuitClosed() {
            wireMockServer.stubFor(get(urlEqualTo(GEO_PATH))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"countryCode\":\"PL\"}")));

            String result = geoLocationService.getCountryCode(IP);

            assertThat(result).isEqualTo("PL");
            wireMockServer.verify(1, getRequestedFor(urlEqualTo(GEO_PATH)));
        }
    }

    @Nested
    @DisplayName("No retry on business exceptions")
    class NoRetryOnBusinessException {

        @Test
        @DisplayName("should not retry when response has null country code")
        void shouldNotRetryOnInvalidResponse() {
            wireMockServer.stubFor(get(urlEqualTo(GEO_PATH))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"countryCode\":null}")));

            assertThatThrownBy(() -> geoLocationService.getCountryCode(IP))
                    .isInstanceOf(GeoLocationException.class)
                    .hasMessageContaining("Unable to resolve country");

            wireMockServer.verify(1, getRequestedFor(urlEqualTo(GEO_PATH)));
        }
    }
}
