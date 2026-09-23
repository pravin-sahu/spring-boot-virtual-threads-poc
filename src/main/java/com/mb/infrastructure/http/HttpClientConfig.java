package com.mb.infrastructure.http;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Configuration class for RestClient bean. This bean can be used to make HTTP requests to external
 * services. It is configured with a default header of Content-Type: application/json. This
 * configuration class can be extended to include additional configurations such as connection
 * timeouts, error handling, etc. as needed.
 *
 * @author rohit.kavthekar
 */
@Configuration
public class HttpClientConfig {

  @Value("${app.http.client.connection-timeout-ms:5000}")
  private int connectionTimeout;

  @Value("${app.http.client.read-timeout-ms:10000}")
  private int readTimeout;

  @Bean
  RestClient restClient() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setConnectionRequestTimeout(Duration.ofMillis(connectionTimeout));
    factory.setReadTimeout(Duration.ofMillis(readTimeout));

    return RestClient.builder()
        .requestFactory(factory)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}
