/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.providers;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class RegionEndpointProviderTest {

    private RegionEndpointProvider classUnderTest = RegionEndpointProvider.getInstance();

    @Test
    public void testLoadFromMap() {

        Map<String, String> regions = new HashMap<>();

        regions.put("region1", "endpoint1");
        regions.put("region2", "endpoint2");
        regions.put("region3", "endpoint3");

        classUnderTest.initialise(regions);

        String[] expectedRegions = {"region1",
                "region2",
                "region3"};

        for (String region : expectedRegions) {
            assertNotNull(classUnderTest.getEndpoint(region));
        }
    }
}
