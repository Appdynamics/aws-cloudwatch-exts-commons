package com.appdynamics.extensions.aws.collectors;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.appdynamics.extensions.aws.providers.RegionEndpointProvider;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * @author Akshay Srivastava
 */
public class AWSClientCache {

    private static AWSClientCache instance;

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

    public static AWSClientCache getInstance() {
        if (instance == null) {
            instance = new AWSClientCache();
        }

        return instance;
    }

}
