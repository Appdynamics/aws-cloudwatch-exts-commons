/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.metric.processors;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.appdynamics.extensions.aws.config.Tags;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.metric.NamespaceMetricStatistics;
import com.appdynamics.extensions.aws.metric.StatisticType;
import com.appdynamics.extensions.metrics.Metric;

import java.util.List;
import java.util.concurrent.atomic.LongAdder;

/**
 * Interface to be implemented by Namespace specific MetricsProcessor
 *
 * @author Florencio Sarmiento
 */
public interface MetricsProcessor {

    /**
     * Any metrics (names + dimensions) returned in this list
     * will be used for retrieving statistics, therefore
     * any necessary filtering must be done within this method
     *
     * @param awsCloudWatch      AmazonCloudWatch
     * @param awsRequestsCounter a requests counter which counts the number of requests
     * @return list of metrics
     */
    List<AWSMetric> getMetrics(AmazonCloudWatch awsCloudWatch, String accountName, LongAdder awsRequestsCounter);

    /**
     * Returns the statistic type of the specified metric
     *
     * @param metric - Metric
     * @return statisticType
     */
    StatisticType getStatisticType(AWSMetric metric);

    /**
     * Converts the nested statistics within NamespaceMetricStatistics
     * into a Map object for uploading to Controller.
     * <p>
     * Map key is the metric name without the metric prefix, e.g.
     * <b>MyTestAccount|us-east-1|mycachecluster|0001|CPUUtilization</b>
     *
     * @param namespaceMetricStats NamespaceMetricStatistics
     * @return Map of statistics
     */
    List<Metric> createMetricStatsMapForUpload(NamespaceMetricStatistics namespaceMetricStats);

    /**
     * @return the Namespace
     */
    String getNamespace();

    List<AWSMetric> filterUsingTags(List<AWSMetric> metrics, List<Tags> tags, String region);
}
