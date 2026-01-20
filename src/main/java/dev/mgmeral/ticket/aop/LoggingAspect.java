package dev.mgmeral.ticket.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* dev.mgmeral.ticket.service..*(..))")
    public Object logServiceMethods(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String sig = pjp.getSignature().toShortString();

        try {
            Object result = pjp.proceed();
            long took = System.currentTimeMillis() - start;
            log.info("{} took={}ms", sig, took);
            return result;
        } catch (Throwable t) {
            long took = System.currentTimeMillis() - start;
            log.error("{} failed took={}ms err={}", sig, took, t.toString());
            throw t;
        }
    }
}
