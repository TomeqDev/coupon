package com.task.infrastructure.geolocation;

import com.task.application.exception.GeoLocationException;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.client.HttpServerErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IpApiGeoLocationServiceTest {

    private static final String BASE_URL = "http://ip-api.com/json/%s?fields=countryCode";

    private IpApiGeoLocationService geoLocationService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        geoLocationService = new IpApiGeoLocationService(builder, "http://ip-api.com/json/{ip}?fields=countryCode");
    }

    private void givenApiReturns(String ip, String responseBody) {
        mockServer.expect(requestTo(BASE_URL.formatted(ip)))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    private void givenApiReturnsError(String ip) {
        mockServer.expect(requestTo(BASE_URL.formatted(ip)))
                .andRespond(withServerError());
    }

    @Nested
    @DisplayName("Successful resolution")
    class SuccessfulResolution {

        @Test
        @DisplayName("should return country code for valid IP")
        void shouldReturnCountryCodeForValidIp() {
            givenApiReturns("185.0.0.1", "{\"countryCode\":\"PL\"}");

            String result = geoLocationService.getCountryCode("185.0.0.1");

            assertThat(result).isEqualTo("PL");
            mockServer.verify();
        }

        @Test
        @DisplayName("should normalize country code to uppercase")
        void shouldReturnUppercaseCountryCode() {
            givenApiReturns("8.8.8.8", "{\"countryCode\":\"us\"}");

            String result = geoLocationService.getCountryCode("8.8.8.8");

            assertThat(result).isEqualTo("US");
            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("Failed resolution")
    class FailedResolution {

        @Test
        @DisplayName("should throw when response contains null country code")
        void shouldThrowWhenResponseIsNull() {
            givenApiReturns("0.0.0.0", "{\"countryCode\":null}");

            assertThatThrownBy(() -> geoLocationService.getCountryCode("0.0.0.0"))
                    .isInstanceOf(GeoLocationException.class)
                    .hasMessageContaining("Unable to resolve country");

            mockServer.verify();
        }

        @Test
        @DisplayName("should throw when external service is unavailable")
        void shouldThrowWhenServiceUnavailable() {
            givenApiReturnsError("1.1.1.1");

            assertThatThrownBy(() -> geoLocationService.getCountryCode("1.1.1.1"))
                    .isInstanceOf(HttpServerErrorException.class);

            mockServer.verify();
        }
    }
}
