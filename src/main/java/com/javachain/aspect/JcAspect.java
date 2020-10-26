package com.javachain.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Calendar;

@Aspect
@Component
public class JcAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(JcAspect.class);

    @Before("execution(* com.javachain.service.*.*(..))")
    public void beforeSampleCreation(JoinPoint jp) {
        LOGGER.info("Running service {} with the arguments: {}", jp.getSignature()
                , jp.getArgs().length > 0 ? Arrays.toString(jp.getArgs()) : null);
    }

    @Around("execution(* com.javachain.service.*.*(..))")
    public Object aroundSampleCreation(ProceedingJoinPoint pjp) throws Throwable {
        LOGGER.info("=================== Started timing service {} with the arguments: {}", pjp.getSignature(),
                Arrays.toString(pjp.getArgs()));
        long time = Calendar.getInstance().getTimeInMillis();
        Object output = pjp.proceed();
        LOGGER.info("=================== Ended timing service {} with the time: {}", pjp.getSignature(),
                (Calendar.getInstance().getTimeInMillis() - time));
        return output;

    }
}
