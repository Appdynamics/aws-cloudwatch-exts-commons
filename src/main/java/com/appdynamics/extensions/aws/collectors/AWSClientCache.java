package com.appdynamics.extensions.aws.collectors;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

/**
 * @author Akshay Srivastava
 */
public class AWSClientCache {

    private static AWSClientCache instance;

    private final Cache<String, CloudWatchClient> cloudwatchClientCache;

    public AWSClientCache() {
        cloudwatchClientCache = CacheBuilder.newBuilder().build();
    }

    public void put(Object regionInfo, CloudWatchClient cloudwatchClient) {
        cloudwatchClientCache.put(String.valueOf(regionInfo), cloudwatchClient);
    }

    public CloudWatchClient get(Object regionInfo) {
        return cloudwatchClientCache.getIfPresent(String.valueOf(regionInfo));
    }

    public static AWSClientCache getInstance() {
        if (instance == null) {
            instance = new AWSClientCache();
        }

        return instance;
    }

}
