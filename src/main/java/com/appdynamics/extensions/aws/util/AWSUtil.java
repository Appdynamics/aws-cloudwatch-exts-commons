/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.util;

import com.appdynamics.extensions.aws.config.AwsClientConfig;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import com.appdynamics.extensions.Constants;
import com.appdynamics.extensions.aws.config.Account;
import com.appdynamics.extensions.aws.config.CredentialsDecryptionConfig;
import com.appdynamics.extensions.aws.config.ProxyConfig;
import com.appdynamics.extensions.util.CryptoUtils;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;

import java.net.URI;
import java.util.Map;

/**
 * @author Florencio Sarmiento
 */
public class AWSUtil {

    public static Long convertToLong(Double metricValue) {
        Long value = null;

        if (metricValue == null || metricValue < 0) {
            value = 0L;

        } else {
            value = Math.round(metricValue);
        }

        return value;
    }

    public static StaticCredentialsProvider createAWSCredentials(Account account,
                                                                 CredentialsDecryptionConfig credentialsDecryptionConfig) {
        String awsAccessKey = account.getAwsAccessKey();
        String awsSecretKey = account.getAwsSecretKey();
        String awsSessionToken = account.getAwsSessionToken();

        if (credentialsDecryptionConfig != null &&
                credentialsDecryptionConfig.isDecryptionEnabled()) {
            String encryptionKey = credentialsDecryptionConfig.getEncryptionKey();
            awsAccessKey = getDecryptedPassword(awsAccessKey, encryptionKey);
            awsSecretKey = getDecryptedPassword(awsSecretKey, encryptionKey);
            if( awsSessionToken != null && !"".equals(awsSessionToken) )
                awsSessionToken = getDecryptedPassword(awsSessionToken, encryptionKey);
        }

        if( StringUtils.isNotEmpty(awsAccessKey) && StringUtils.isNotEmpty(awsSecretKey) && StringUtils.isNotEmpty(awsSessionToken) ) {
            AwsSessionCredentials awsSessionCredentials = AwsSessionCredentials.create(awsAccessKey, awsSecretKey, awsSessionToken);
            return StaticCredentialsProvider.create(awsSessionCredentials);
        } // else fall through and send back the basic credentials
        AwsBasicCredentials credentials = AwsBasicCredentials.create(awsAccessKey, awsSecretKey);
        return StaticCredentialsProvider.create(credentials);
    }

    private static String getDecryptedPassword(String encryptedPassword, String encryptionKey) {
        Map<String, String> cryptoMap = Maps.newHashMap();
        cryptoMap.put(Constants.ENCRYPTED_PASSWORD, encryptedPassword);
        cryptoMap.put(Constants.ENCRYPTION_KEY, encryptionKey);
        return CryptoUtils.getPassword(cryptoMap);
    }

    public static AwsClientConfig createAwsClientConfiguration(int maxErrorRetrySize, ProxyConfig proxyConfig) {
        // Configure the Apache HTTP client builder
        ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder();

        if (proxyConfig != null && StringUtils.isNotBlank(proxyConfig.getHost()) && proxyConfig.getPort() != null) {
            // Construct the proxy endpoint URI
            URI proxyEndpoint = URI.create("http://" + proxyConfig.getHost() + ":" + proxyConfig.getPort());
            ProxyConfiguration.Builder proxyBuilder = ProxyConfiguration.builder().endpoint(proxyEndpoint);

            if (StringUtils.isNotBlank(proxyConfig.getUsername())) {
                proxyBuilder.username(proxyConfig.getUsername());
            }
            if (StringUtils.isNotBlank(proxyConfig.getPassword())) {
                proxyBuilder.password(proxyConfig.getPassword());
            }

            httpClientBuilder.proxyConfiguration(proxyBuilder.build());
        }

        SdkHttpClient httpClient = httpClientBuilder.build();

        // Configure client override settings (e.g., retry policy)
        ClientOverrideConfiguration overrideConfiguration = ClientOverrideConfiguration.builder()
                .retryPolicy(RetryPolicy.builder().numRetries(maxErrorRetrySize).build())
                .build();

        return new AwsClientConfig(httpClient, overrideConfiguration);
    }
}
