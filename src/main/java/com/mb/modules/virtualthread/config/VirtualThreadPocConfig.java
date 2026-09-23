package com.mb.modules.virtualthread.config;

import com.mb.modules.virtualthread.benchmark.BenchmarkRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Exposes the plain-Java {@link BenchmarkRunner} as a bean, keeping the benchmark package free of
 * Spring annotations.
 *
 * <p>All PoC beans (this config, the service and the controller) exist only when the {@value
 * #PROFILE} profile is active. In every other profile the {@code /v1/virtual-threads/**} endpoints
 * do not exist and return 404, so the PoC-only {@code permitAll} rule in {@code SecurityConfig}
 * matches nothing.
 *
 * @author pravin.sahu
 */
@Configuration
@Profile(VirtualThreadPocConfig.PROFILE)
public class VirtualThreadPocConfig {

  public static final String PROFILE = "virtual-thread-poc";

  @Bean
  BenchmarkRunner benchmarkRunner() {
    return new BenchmarkRunner();
  }
}
