/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.util;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.appdynamics.extensions.Constants;
import com.appdynamics.extensions.aws.config.Account;
import com.appdynamics.extensions.aws.config.CredentialsDecryptionConfig;
import com.appdynamics.extensions.aws.config.ProxyConfig;
import com.appdynamics.extensions.util.CryptoUtils;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;

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

    public static AWSCredentials createAWSCredentials(Account account,
                                                      CredentialsDecryptionConfig credentialsDecryptionConfig) {
        String awsAccessKey = account.getAwsAccessKey();
        String awsSecretKey = account.getAwsSecretKey();

        if (credentialsDecryptionConfig != null &&
                credentialsDecryptionConfig.isDecryptionEnabled()) {
            String encryptionKey = credentialsDecryptionConfig.getEncryptionKey();
            awsAccessKey = getDecryptedPassword(awsAccessKey, encryptionKey);
            awsSecretKey = getDecryptedPassword(awsSecretKey, encryptionKey);
        }

        AWSCredentials awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
        return awsCredentials;
    }

    private static String getDecryptedPassword(String encryptedPassword, String encryptionKey) {
        Map<String, String> cryptoMap = Maps.newHashMap();
        cryptoMap.put(Constants.ENCRYPTED_PASSWORD, encryptedPassword);
        cryptoMap.put(Constants.ENCRYPTION_KEY, encryptionKey);
        return CryptoUtils.getPassword(cryptoMap);
    }

    public static ClientConfiguration createAWSClientConfiguration(int maxErrorRetrySize,
                                                                   ProxyConfig proxyConfig) {
        ClientConfiguration awsClientConfig = new ClientConfiguration();
        awsClientConfig.setMaxErrorRetry(maxErrorRetrySize);

        if (proxyConfig != null && StringUtils.isNotBlank(proxyConfig.getHost()) &&
                proxyConfig.getPort() != null) {
            awsClientConfig.setProxyHost(proxyConfig.getHost());
            awsClientConfig.setProxyPort(proxyConfig.getPort());
            awsClientConfig.setProxyUsername(proxyConfig.getUsername());
            awsClientConfig.setProxyPassword(proxyConfig.getPassword());
        }

        return awsClientConfig;
    }
}
