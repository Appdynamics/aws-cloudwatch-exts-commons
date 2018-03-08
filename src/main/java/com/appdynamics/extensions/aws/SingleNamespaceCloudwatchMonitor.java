/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws;

import com.appdynamics.extensions.aws.collectors.NamespaceMetricStatisticsCollector;
import com.appdynamics.extensions.metrics.Metric;

import java.util.List;

/**
 * @param <T> Configuration class
 * @author Florencio Sarmiento
 */
public abstract class SingleNamespaceCloudwatchMonitor<T> extends AWSCloudwatchMonitor<T> {

    public SingleNamespaceCloudwatchMonitor(Class<T> clazz) {
        super(clazz);
    }

    /*
     * Running NamespaceMetricsCollector on the same thread
     * since we're only dealing with a single namespace
     */
    @Override
    protected List<Metric> getStatsForUpload(T config) {
        return getNamespaceMetricsCollector(config).call();
    }

    protected abstract NamespaceMetricStatisticsCollector getNamespaceMetricsCollector(T config);
}