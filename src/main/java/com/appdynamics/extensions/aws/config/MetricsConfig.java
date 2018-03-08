/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.config;

import java.util.List;

/**
 * @author Florencio Sarmiento
 */
public class MetricsConfig {

    private List<IncludeMetric> includeMetrics;

    private MetricsTimeRange metricsTimeRange;

    private int metricsCollectionInterval;

    private int metricsCacheExpiryInterval;

    private int getMetricStatisticsRateLimit = 400;

    private int maxErrorRetrySize;

    public List<IncludeMetric> getIncludeMetrics() {
        return includeMetrics;
    }

    public void setIncludeMetrics(List<IncludeMetric> includeMetrics) {
        this.includeMetrics = includeMetrics;
    }

    public MetricsTimeRange getMetricsTimeRange() {
        return metricsTimeRange;
    }

    public void setMetricsTimeRange(MetricsTimeRange metricsTimeRange) {
        this.metricsTimeRange = metricsTimeRange;
    }

    public int getMetricsCollectionInterval() {
        return metricsCollectionInterval;
    }

    public void setMetricsCollectionInterval(int metricsCollectionInterval) {
        this.metricsCollectionInterval = metricsCollectionInterval;
    }

    public int getMetricsCacheExpiryInterval() {
        return metricsCacheExpiryInterval;
    }

    public void setMetricsCacheExpiryInterval(int metricsCacheExpiryInterval) {
        this.metricsCacheExpiryInterval = metricsCacheExpiryInterval;
    }

    public int getGetMetricStatisticsRateLimit() {
        return getMetricStatisticsRateLimit;
    }

    public void setGetMetricStatisticsRateLimit(int getMetricStatisticsRateLimit) {
        this.getMetricStatisticsRateLimit = getMetricStatisticsRateLimit;
    }

    public int getMaxErrorRetrySize() {
        return maxErrorRetrySize;
    }

    public void setMaxErrorRetrySize(int maxErrorRetrySize) {
        this.maxErrorRetrySize = maxErrorRetrySize;
    }

}
