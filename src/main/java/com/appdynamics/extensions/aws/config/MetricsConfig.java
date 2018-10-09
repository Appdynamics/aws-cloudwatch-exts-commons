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

    private int getMetricStatisticsRateLimit = 400;

    private int maxErrorRetrySize;

    private int periodInSeconds;

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

    public int getPeriodInSeconds() { return periodInSeconds; }

    public void setPeriodInSeconds(int periodInSeconds) { this.periodInSeconds = periodInSeconds; }
}