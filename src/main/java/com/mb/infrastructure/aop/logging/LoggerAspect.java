package com.mb.infrastructure.aop.logging;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * AOP aspect for method execution timing across the controller, service, and DAO layers.
 *
 * <p>Logs method start and completion with elapsed time at INFO level. Exceptions are not logged
 * here — they propagate naturally and are handled once by {@code GlobalExceptionHandler}, avoiding
 * duplicate ERROR lines per layer.
 *
 * <p>Limitation: Spring AOP is proxy-based. Internal self-invocation (a method calling another
 * method on {@code this} within the same class) bypasses the proxy and will not be intercepted.
 *
 * @author rohit.kavthekar
 */
@Aspect
@Component
@Slf4j
public class LoggerAspect {

  // -------------------------------------------------------------------------
  // Constants
  // -------------------------------------------------------------------------

  private static final String CONTROLLER_LAYER = "CONTROLLER";
  private static final String SERVICE_LAYER = "SERVICE";
  private static final String DAO_LAYER = "DAO";

  // -------------------------------------------------------------------------
  // Named pointcuts — single source of truth for expressions
  // -------------------------------------------------------------------------

  /** All public methods in any controller class under com.mb. */
  @Pointcut("execution(public * com.mb..controller..*(..))")
  public void controllerMethods() {}

  /** All public methods in any service implementation under com.mb. */
  @Pointcut("execution(public * com.mb..service..*(..))")
  public void serviceMethods() {}

  /** All public methods in any DAO implementation under com.mb. */
  @Pointcut("execution(public * com.mb..dao..*(..))")
  public void daoMethods() {}

  // -------------------------------------------------------------------------
  // Layer-specific @Around advice — eliminates string-matching misclassification
  // -------------------------------------------------------------------------

  /** Logs controller method execution with CONTROLLER layer context. */
  @Around("controllerMethods()")
  public Object logControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
    return logMethodExecution(joinPoint, CONTROLLER_LAYER);
  }

  /** Logs service method execution with SERVICE layer context. */
  @Around("serviceMethods()")
  public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
    return logMethodExecution(joinPoint, SERVICE_LAYER);
  }

  /** Logs DAO method execution with DAO layer context. */
  @Around("daoMethods()")
  public Object logDaoExecution(ProceedingJoinPoint joinPoint) throws Throwable {
    return logMethodExecution(joinPoint, DAO_LAYER);
  }

  // -------------------------------------------------------------------------
  // Shared execution logic
  // -------------------------------------------------------------------------

  /**
   * Shared method execution logging with known layer context. Eliminates the need for
   * string-matching layer resolution.
   */
  private Object logMethodExecution(ProceedingJoinPoint joinPoint, String layer) throws Throwable {

    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String className = signature.getDeclaringType().getName();
    String methodName = signature.getName();

    log.info("[{}] {}.{}() started", layer, className, methodName);

    long start = System.nanoTime();
    Object result = joinPoint.proceed();
    long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

    log.info("[{}] {}.{}() ended in {} ms", layer, className, methodName, elapsedMs);

    return result;
  }
}
