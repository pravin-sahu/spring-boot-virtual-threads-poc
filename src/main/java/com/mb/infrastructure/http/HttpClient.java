package com.mb.infrastructure.http;

import com.mb.common.exception.AppException;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * HttpClient is a component responsible for executing HTTP requests using a RestClient. It provides
 * a method to execute an HTTP request based on the provided HttpRequest object and returns the
 * response mapped to the specified response type. The class also logs the request details, the time
 * taken for the request, and handles any exceptions that may occur during the execution of the HTTP
 * request.
 *
 * @author rohit.kavthekar
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HttpClient {

  private final RestClient restClient;

  /**
   * Executes an HTTP request based on the provided HttpRequest object and returns the response
   * mapped to the specified response type. It logs the request details, the time taken for the
   * request, and handles any exceptions that may occur during the execution of the HTTP request.
   *
   * @author rohit.kavthekar
   * @param <T>
   * @param <R>
   * @param request
   * @param responseType
   * @return response mapped to the specified response type
   */
  public <T, R> R execute(HttpRequest<T> request, Class<R> responseType) {

    long start = System.currentTimeMillis();

    log.info("HTTP Request → method={}, url={}", request.getMethod(), request.getUrl());

    try {

      URI uri = this.buildUri(request);

      return restClient
          .method(request.getMethod())
          .uri(uri)
          .headers(
              h -> {
                if (request.getHeaders() != null) {
                  h.setAll(request.getHeaders());
                }
              })
          .body(request.getBody())
          .retrieve()
          .body(responseType);

    } catch (RestClientResponseException e) {

      HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
      log.error(
          "HTTP request failed - status={}, url={}, message={}",
          e.getStatusCode(),
          request.getUrl(),
          e.getMessage());
      throw new AppException(
          "Http request failed", e.getMessage(), status != null ? status : HttpStatus.BAD_GATEWAY);

    } catch (Exception e) {

      log.error("HTTP request failed - url={}, message={}", request.getUrl(), e.getMessage());
      throw new AppException(
          "Http request failed", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    } finally {

      log.info("HTTP request took {} ms", System.currentTimeMillis() - start);
    }
  }

  /**
   * Builds a URI from the given HttpRequest, including query parameters if present.
   *
   * @author rohit.kavthekar
   * @param <T>
   * @param request
   * @return {@link URI}
   */
  private <T> URI buildUri(HttpRequest<T> request) {

    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(request.getUrl());

    if (request.getQueryParams() != null && !request.getQueryParams().isEmpty()) {

      request.getQueryParams().forEach(builder::queryParam);
    }

    return builder.build().encode().toUri();
  }
}
