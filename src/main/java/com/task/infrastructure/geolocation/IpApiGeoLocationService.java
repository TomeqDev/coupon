package com.task.infrastructure.geolocation;

import com.task.application.exception.GeoLocationException;
import com.task.application.port.GeoLocationService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class IpApiGeoLocationService implements GeoLocationService {

    private final String geoLocationUrl;
    private final RestClient restClient;

    public IpApiGeoLocationService(RestClient.Builder restClientBuilder,
                                    @Value("${geolocation.url:http://ip-api.com/json/{ip}?fields=countryCode}") String geoLocationUrl) {
        this.restClient = restClientBuilder.build();
        this.geoLocationUrl = geoLocationUrl;
    }

    @Override
    @CircuitBreaker(name = "geolocation", fallbackMethod = "fallback")
    @Retry(name = "geolocation")
    public String getCountryCode(String ipAddress) {
        log.debug("Resolving country for IP: {}", ipAddress);

        IpApiResponse response = restClient.get()
                .uri(geoLocationUrl, ipAddress)
                .retrieve()
                .body(IpApiResponse.class);

        validateResponse(ipAddress, response);

        log.debug("Resolved IP {} to country {}", ipAddress, response.countryCode());
        return response.countryCode().toUpperCase();
    }

    private String fallback(String ipAddress, GeoLocationException ex) {
        throw ex;
    }

    private String fallback(String ipAddress, Throwable throwable) {
        log.error("Geolocation service failed for IP: {}, cause: {}",
                ipAddress, throwable.getMessage());
        throw new GeoLocationException("Geolocation service unavailable (circuit open)", throwable);
    }

    private static void validateResponse(String ipAddress, IpApiResponse response) {
        if (response == null || response.countryCode() == null) {
            log.warn("Could not resolve country for IP: {}", ipAddress);
            throw new GeoLocationException("Unable to resolve country for IP: " + ipAddress);
        }
    }

    private record IpApiResponse(String countryCode) {
    }
}
