/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.aws.config.Configuration;
import com.appdynamics.extensions.aws.providers.RegionEndpointProvider;
import com.appdynamics.extensions.metrics.Metric;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * @param <T> Configuration class
 * @author Florencio Sarmiento
 */
public abstract class AWSCloudwatchMonitor<T> extends ABaseMonitor {

    private Class<T> clazz;

    public AWSCloudwatchMonitor(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    protected void doRun(TasksExecutionServiceProvider serviceProvider) {
        getLogger().info("Starting AWS Cloudwatch Monitoring task");

        try {

            Map<String, ?> configYml = configuration.getConfigYml();
            T config = parseConfig(configYml);

            initialize(config);
            
            List<Metric> statsForUpload = getStatsForUpload(config);

            serviceProvider.getMetricWriteHelper().transformAndPrintMetrics(statsForUpload);

        } catch (Exception ex) {
            getLogger().error("Unfortunately an issue has occurred: ", ex);
        }
    }

    protected void initialiseRegionServiceProviders(T config) {
        Configuration thisConfig = (Configuration) config;

        RegionEndpointProvider regionEndpointProvider = RegionEndpointProvider.getInstance();
        regionEndpointProvider.initialise(thisConfig.getRegionEndPoints());
    }

    private T parseConfig(Map<String, ?> configYml) {
        Gson gson = new Gson();
        JsonElement jsonElement = gson.toJsonTree(configYml);
        T config = gson.fromJson(jsonElement, this.clazz);
        return config;
    }

    protected void initialize(T config) {
        initialiseRegionServiceProviders(config);
    }

    protected abstract List<Metric> getStatsForUpload(T config);

    protected abstract Logger getLogger();
}
