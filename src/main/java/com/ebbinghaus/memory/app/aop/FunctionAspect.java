package com.ebbinghaus.memory.app.aop;

import com.ebbinghaus.memory.app.model.InputUserData;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class FunctionAspect {

    @Around("@annotation(com.ebbinghaus.memory.app.aop.Interceptable)")
    public Object aroundFunctionCall(ProceedingJoinPoint joinPoint) throws Throwable {
        // Add your pre-function call logic here
        System.out.println("Before function call: " + joinPoint.getSignature());

        // Proceed with the original function call
        Object result = joinPoint.proceed();

        // Optionally, add post-function call logic here
        System.out.println("After function call: " + joinPoint.getSignature());

        return result;
    }
}
