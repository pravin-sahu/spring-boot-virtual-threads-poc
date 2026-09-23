package com.mb.infrastructure.aop.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link LoggerAspect}.
 *
 * <p>Verifies each layer-specific advice method ({@code logControllerExecution}, {@code
 * logServiceExecution}, {@code logDaoExecution}) by using a mocked {@link ProceedingJoinPoint}.
 * Covers the success path (result returned) and the exception path (exception propagates unchanged
 * — the aspect does not log or swallow it). {@link MockitoSettings} is set to lenient so that the
 * shared {@code @BeforeEach} stubs do not cause "unnecessary stubbing" errors in tests that also
 * stub {@code joinPoint.proceed()}.
 *
 * @author rohit.kavthekar
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LoggerAspect – AOP method execution logging")
class LoggerAspectTest {

  @Mock private ProceedingJoinPoint joinPoint;
  @Mock private MethodSignature methodSignature;

  private final LoggerAspect loggerAspect = new LoggerAspect();

  @BeforeEach
  void stubSignature() {
    when(joinPoint.getSignature()).thenReturn(methodSignature);
    when(methodSignature.getDeclaringType()).thenReturn(Object.class);
    when(methodSignature.getName()).thenReturn("testMethod");
  }

  // -------------------------------------------------------------------------
  // logControllerExecution – happy path
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("logControllerExecution: successful join point → returns the original result")
  void logControllerExecutionWhenSuccessReturnsResult() throws Throwable {
    when(joinPoint.proceed()).thenReturn("controller-result");

    Object result = loggerAspect.logControllerExecution(joinPoint);

    assertThat(result).isEqualTo("controller-result");
  }

  // -------------------------------------------------------------------------
  // logControllerExecution – exception paths
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("logControllerExecution: RuntimeException → same instance rethrown")
  void logControllerExecutionWhenRuntimeExceptionRethrowsSameInstance() throws Throwable {
    RuntimeException cause = new RuntimeException("controller error");
    when(joinPoint.proceed()).thenThrow(cause);

    assertThatThrownBy(() -> loggerAspect.logControllerExecution(joinPoint)).isSameAs(cause);
  }

  @Test
  @DisplayName("logControllerExecution: checked exception → same instance rethrown")
  void logControllerExecutionWhenCheckedExceptionRethrowsSameInstance() throws Throwable {
    IOException cause = new IOException("I/O failure");
    when(joinPoint.proceed()).thenThrow(cause);

    assertThatThrownBy(() -> loggerAspect.logControllerExecution(joinPoint))
        .isInstanceOf(IOException.class)
        .isSameAs(cause);
  }

  @Test
  @DisplayName("logControllerExecution: Error subclass → same instance rethrown")
  void logControllerExecutionWhenErrorThrownRethrowsSameInstance() throws Throwable {
    OutOfMemoryError cause = new OutOfMemoryError("heap exhausted");
    when(joinPoint.proceed()).thenThrow(cause);

    assertThatThrownBy(() -> loggerAspect.logControllerExecution(joinPoint))
        .isInstanceOf(OutOfMemoryError.class)
        .isSameAs(cause);
  }

  // -------------------------------------------------------------------------
  // logServiceExecution – happy path
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("logServiceExecution: successful join point → returns the original result")
  void logServiceExecutionWhenSuccessReturnsResult() throws Throwable {
    when(joinPoint.proceed()).thenReturn(42);

    Object result = loggerAspect.logServiceExecution(joinPoint);

    assertThat(result).isEqualTo(42);
  }

  // -------------------------------------------------------------------------
  // logServiceExecution – exception path
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("logServiceExecution: RuntimeException → same instance rethrown")
  void logServiceExecutionWhenExceptionThrownRethrowsSameInstance() throws Throwable {
    RuntimeException cause = new RuntimeException("service error");
    when(joinPoint.proceed()).thenThrow(cause);

    assertThatThrownBy(() -> loggerAspect.logServiceExecution(joinPoint)).isSameAs(cause);
  }

  @Test
  @DisplayName("logServiceExecution: Error subclass → same instance rethrown")
  void logServiceExecutionWhenErrorThrownRethrowsSameInstance() throws Throwable {
    StackOverflowError cause = new StackOverflowError("infinite recursion");
    when(joinPoint.proceed()).thenThrow(cause);

    assertThatThrownBy(() -> loggerAspect.logServiceExecution(joinPoint))
        .isInstanceOf(StackOverflowError.class)
        .isSameAs(cause);
  }

  // -------------------------------------------------------------------------
  // logDaoExecution – happy path
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("logDaoExecution: null return from join point → returns null without exception")
  void logDaoExecutionWhenSuccessWithNullResultReturnsNull() throws Throwable {
    when(joinPoint.proceed()).thenReturn(null);

    Object result = loggerAspect.logDaoExecution(joinPoint);

    assertThat(result).isNull();
  }

  // -------------------------------------------------------------------------
  // logDaoExecution – exception path
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("logDaoExecution: RuntimeException → same instance rethrown")
  void logDaoExecutionWhenExceptionThrownRethrowsSameInstance() throws Throwable {
    RuntimeException cause = new RuntimeException("dao error");
    when(joinPoint.proceed()).thenThrow(cause);

    assertThatThrownBy(() -> loggerAspect.logDaoExecution(joinPoint)).isSameAs(cause);
  }

  @Test
  @DisplayName("logDaoExecution: Error subclass → same instance rethrown")
  void logDaoExecutionWhenErrorThrownRethrowsSameInstance() throws Throwable {
    OutOfMemoryError cause = new OutOfMemoryError("heap exhausted");
    when(joinPoint.proceed()).thenThrow(cause);

    assertThatThrownBy(() -> loggerAspect.logDaoExecution(joinPoint))
        .isInstanceOf(OutOfMemoryError.class)
        .isSameAs(cause);
  }

  // -------------------------------------------------------------------------
  // Additional coverage tests
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("logServiceExecution: returns complex object → object returned as-is")
  void logServiceExecutionWhenReturningComplexObjectReturnsObjectAsIs() throws Throwable {
    java.util.List<String> complexResult = java.util.List.of("item1", "item2");
    when(joinPoint.proceed()).thenReturn(complexResult);

    Object result = loggerAspect.logServiceExecution(joinPoint);

    assertThat(result).isSameAs(complexResult);
  }

  @Test
  @DisplayName("logControllerExecution: checked exception → same instance rethrown")
  void logControllerExecutionWhenCustomCheckedExceptionRethrowsSameInstance() throws Throwable {
    Exception cause = new Exception("custom checked exception");
    when(joinPoint.proceed()).thenThrow(cause);

    assertThatThrownBy(() -> loggerAspect.logControllerExecution(joinPoint))
        .isInstanceOf(Exception.class)
        .isSameAs(cause);
  }

  @Test
  @DisplayName("logDaoExecution: returns primitive value → primitive returned as-is")
  void logDaoExecutionWhenReturningPrimitiveReturnsValueAsIs() throws Throwable {
    when(joinPoint.proceed()).thenReturn(true);

    Object result = loggerAspect.logDaoExecution(joinPoint);

    assertThat(result).isEqualTo(true);
  }

  // -------------------------------------------------------------------------
  // Pointcut method coverage tests
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("controllerMethods pointcut → method executes without exception")
  void controllerMethodsPointcutExecutesWithoutException() {
    assertThatCode(loggerAspect::controllerMethods).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("serviceMethods pointcut → method executes without exception")
  void serviceMethodsPointcutExecutesWithoutException() {
    assertThatCode(loggerAspect::serviceMethods).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("daoMethods pointcut → method executes without exception")
  void daoMethodsPointcutExecutesWithoutException() {
    assertThatCode(loggerAspect::daoMethods).doesNotThrowAnyException();
  }
}
