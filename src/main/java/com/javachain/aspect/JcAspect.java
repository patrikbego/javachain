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

    @Before("execution(* com.abcofcoin.bc.*.*(..))")
    public void beforeSampleCreation(JoinPoint jp) {
        LOGGER.info("Running service " + jp.getSignature() + " with the arguments: " +
                Arrays.toString(jp.getArgs()));
    }

    @Around("execution(* com.abcofcoin.bc.*.*(..))")
    public Object aroundSampleCreation(ProceedingJoinPoint pjp) throws Throwable {
        LOGGER.info("=================== Started timing service " + pjp.getSignature() + " with the arguments: " +
                Arrays.toString(pjp.getArgs()));
        long time = Calendar.getInstance().getTimeInMillis();
        Object output = pjp.proceed();
        LOGGER.info("=================== Ended timing service " + pjp.getSignature() + " with the time: " +
                (Calendar.getInstance().getTimeInMillis() - time));
        return output;

    }
}
