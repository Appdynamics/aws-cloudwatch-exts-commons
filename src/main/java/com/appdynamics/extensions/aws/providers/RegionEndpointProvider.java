/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.providers;

import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Florencio Sarmiento
 */
public class RegionEndpointProvider {

    private static Logger LOGGER = Logger.getLogger("com.singularity.extensions.aws.RegionEndpointProvider");

    private final Map<String, String> regionEndpoints = new ConcurrentHashMap<String, String>();

    private static RegionEndpointProvider instance;

    private RegionEndpointProvider() {
    }

    public static RegionEndpointProvider getInstance() {
        if (instance == null) {
            instance = new RegionEndpointProvider();
        }

        return instance;
    }

    @SuppressWarnings("unchecked")
    public void initialise(Map<String, String> regionEndpoints) {

        if (regionEndpoints != null) {
            synchronized (this.regionEndpoints) {
                this.regionEndpoints.clear();
                this.regionEndpoints.putAll(regionEndpoints);
            }

            LOGGER.info("Region endpoints successfully initialised!");
        } else {
            LOGGER.error("Region endpoints not provided in configuration");
            throw new IllegalArgumentException("Region endpoints not provided in configuration");
        }
    }

    public String getEndpoint(String region) {
        return regionEndpoints.get(region);
    }

}
