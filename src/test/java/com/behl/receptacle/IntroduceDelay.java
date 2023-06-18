package com.behl.receptacle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IntroduceDelay {

    int seconds();

    class Extension implements BeforeTestExecutionCallback {

        @Override
        public void beforeTestExecution(ExtensionContext context) throws Exception {
            final var introduceDelay = context.getRequiredTestMethod().getAnnotation(IntroduceDelay.class);
            if (introduceDelay != null) {
                TimeUnit.SECONDS.sleep(introduceDelay.seconds());
            }
        }

    }

}