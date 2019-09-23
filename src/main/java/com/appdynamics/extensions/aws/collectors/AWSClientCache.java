package com.appdynamics.extensions.aws.collectors;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

/**
 * @author Akshay Srivastava
 */
public class AWSClientCache {

    private final Cache<String, AmazonCloudWatch> cloudwatchClientCache;

    public AWSClientCache() {
        cloudwatchClientCache = CacheBuilder.newBuilder().build();
    }

    public void put(String regionInfo, AmazonCloudWatch cloudwatchClient) {
        cloudwatchClientCache.put(regionInfo, cloudwatchClient);
    }

    public AmazonCloudWatch get(String regionInfo) {
        return cloudwatchClientCache.getIfPresent(regionInfo);
    }

    static class Builder {

        AWSClientCache build() {
            AWSClientCache cache = new AWSClientCache();
            return cache;
        }

    }

}
