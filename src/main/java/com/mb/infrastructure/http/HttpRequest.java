package com.mb.infrastructure.http;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpMethod;

/**
 * @author rohit.kavthekar
 * @param <T>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpRequest<T> {

  private String url;

  private HttpMethod method;

  private T body;

  private Map<String, String> headers;

  private Map<String, String> queryParams;
}
