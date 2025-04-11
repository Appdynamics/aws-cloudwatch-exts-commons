package com.appdynamics.extensions.aws.interceptors;
import com.appdynamics.extensions.aws.collectors.NamespaceMetricStatisticsCollector;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import org.slf4j.Logger;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;

public class LoggingInterceptor implements ExecutionInterceptor {
    private static Logger LOGGER = ExtensionsLoggerFactory.getLogger(LoggingInterceptor.class);
    @Override
    public void beforeTransmission(Context.BeforeTransmission context,
                                   ExecutionAttributes executionAttributes) {
        SdkHttpRequest request = context.httpRequest();
        LOGGER.info("Before Transmission: " + request);
    }

    @Override
    public void afterTransmission(Context.AfterTransmission context,
                                  ExecutionAttributes executionAttributes) {
        SdkHttpResponse response = context.httpResponse();
        LOGGER.info("After Transmission: " + response);
    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        Throwable exception = context.exception();
        LOGGER.warn(String.format("Operation ID: '%s' Exception: '%s'", executionAttributes.getAttributes(), exception.toString()));
    }

}
