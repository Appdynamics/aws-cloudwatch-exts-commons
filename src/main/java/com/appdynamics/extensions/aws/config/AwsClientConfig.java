package com.appdynamics.extensions.aws.config;

import com.appdynamics.extensions.aws.interceptors.LoggingInterceptor;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;

import java.util.Collections;
import java.util.List;

public class AwsClientConfig {
    private final SdkHttpClient httpClient;
    private final ClientOverrideConfiguration overrideConfiguration;

    public AwsClientConfig(SdkHttpClient httpClient, ClientOverrideConfiguration overrideConfiguration) {
        this.httpClient = httpClient;
        this.overrideConfiguration = overrideConfiguration.toBuilder().addExecutionInterceptor(new LoggingInterceptor()).build();
    }

    public SdkHttpClient getHttpClient() {
        return httpClient;
    }

    public ClientOverrideConfiguration getOverrideConfiguration() {
        return overrideConfiguration;
    }
}
