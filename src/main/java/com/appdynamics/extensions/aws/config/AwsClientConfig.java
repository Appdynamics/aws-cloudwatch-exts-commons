package com.appdynamics.extensions.aws.config;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;

public class AwsClientConfig {
    private final SdkHttpClient httpClient;
    private final ClientOverrideConfiguration overrideConfiguration;

    public AwsClientConfig(SdkHttpClient httpClient, ClientOverrideConfiguration overrideConfiguration) {
        this.httpClient = httpClient;
        this.overrideConfiguration = overrideConfiguration;
    }

    public SdkHttpClient getHttpClient() {
        return httpClient;
    }

    public ClientOverrideConfiguration getOverrideConfiguration() {
        return overrideConfiguration;
    }
}
