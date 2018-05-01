/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.metric.processors;

import static com.appdynamics.extensions.aws.Constants.METRIC_PATH_SEPARATOR;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.DimensionFilter;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.appdynamics.extensions.aws.config.IncludeMetric;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.metric.AccountMetricStatistics;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.NamespaceMetricStatistics;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.aws.metric.StatisticType;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides default behaviour and other utility methods
 * that can used by concrete class of {@link MetricsProcessor}
 *
 * @author Florencio Sarmiento
 */
public class MetricsProcessorHelper {

    private static Logger LOGGER = Logger.getLogger(MetricsProcessorHelper.class);


    public static List<AWSMetric> getFilteredMetrics(AmazonCloudWatch awsCloudWatch,
                                                     LongAdder awsRequestsCounter, String namespace, List<IncludeMetric> includeMetrics, String... dimensionNames) {
        List<Metric> metrics = getMetrics(awsCloudWatch, awsRequestsCounter, namespace, dimensionNames);
        return filterMetrics(metrics, includeMetrics);
    }

    public static List<AWSMetric> getFilteredMetrics(AmazonCloudWatch awsCloudWatch,
                                                     LongAdder awsRequestsCounter, String namespace, List<IncludeMetric> includeMetrics, List<DimensionFilter> dimensions) {
        List<Metric> metrics = getMetrics(awsCloudWatch, awsRequestsCounter, namespace, dimensions);
        return filterMetrics(metrics, includeMetrics);
    }

    public static List<AWSMetric> getFilteredMetrics(AmazonCloudWatch awsCloudWatch,
                                                     LongAdder awsRequestsCounter, String namespace, List<IncludeMetric> includeMetrics, List<DimensionFilter> dimensions, Predicate<Metric> metricFilter) {
        List<Metric> metrics = getMetrics(awsCloudWatch, awsRequestsCounter, namespace, dimensions);

        metrics = Lists.newArrayList(Collections2.filter(metrics, metricFilter));

        return filterMetrics(metrics, includeMetrics);
    }


    public static List<Metric> getMetrics(AmazonCloudWatch awsCloudWatch,
                                          LongAdder awsRequestsCounter, String namespace, String... dimensionNames) {
        List<DimensionFilter> dimensions = new ArrayList<DimensionFilter>();

        for (String dimensionName : dimensionNames) {
            DimensionFilter dimension = new DimensionFilter();
            dimension.withName(dimensionName);
            dimensions.add(dimension);
        }

        return getMetrics(awsCloudWatch, awsRequestsCounter, namespace, dimensions);
    }

    public static List<Metric> getMetrics(AmazonCloudWatch awsCloudWatch,
                                          LongAdder awsRequestsCounter, String namespace, List<DimensionFilter> dimensions) {
        ListMetricsRequest request = new ListMetricsRequest();

        request.withNamespace(namespace);
        request.withDimensions(dimensions);
        ListMetricsResult listMetricsResult = awsCloudWatch.listMetrics(request);
        awsRequestsCounter.increment();
        List<Metric> metrics = listMetricsResult.getMetrics();

        // Retrieves all the metrics if metricList > 500
        while (listMetricsResult.getNextToken() != null) {
            request.setNextToken(listMetricsResult.getNextToken());
            listMetricsResult = awsCloudWatch.listMetrics(request);
            awsRequestsCounter.increment();
            metrics.addAll(listMetricsResult.getMetrics());
        }

        return metrics;
    }

    public static StatisticType getStatisticType(IncludeMetric metric, List<IncludeMetric> metrics) {
        if (metrics != null && !metrics.isEmpty() && metric != null) {
            for (IncludeMetric includeMetric : metrics) {
                Pattern pattern = Pattern.compile(includeMetric.getName(),
                        Pattern.CASE_INSENSITIVE);

                if (isMatch(metric.getName(), pattern)) {
                    return StatisticType.fromString(includeMetric.getStatType());
                }
            }
        }

        return StatisticType.AVE;
    }

    public static List<AWSMetric> filterMetrics(List<Metric> metrics, List<IncludeMetric> includeMetrics) {

        List<AWSMetric> awsMetrics = new ArrayList<>();
        if (metrics != null && !metrics.isEmpty() && includeMetrics != null) {

            for (Metric metric : metrics) {
                for (IncludeMetric includeMetric : includeMetrics) {
                    if (includeMetric.getName().equals(metric.getMetricName())) {
                        AWSMetric awsMetric = new AWSMetric();
                        awsMetric.setIncludeMetric(includeMetric);
                        awsMetric.setMetric(metric);
                        awsMetrics.add(awsMetric);
                        continue;
                    }
                }
            }
        }
        return awsMetrics;
    }

    public static Pattern createPattern(Set<String> rawPatterns) {
        Pattern pattern = null;

        if (rawPatterns != null && !rawPatterns.isEmpty()) {
            StringBuilder rawPatternsStringBuilder = new StringBuilder();
            int index = 0;

            for (String rawPattern : rawPatterns) {
                if (index > 0) {
                    rawPatternsStringBuilder.append("|");
                }

                rawPatternsStringBuilder.append(rawPattern);
                index++;
            }

            pattern = Pattern.compile(rawPatternsStringBuilder.toString(),
                    Pattern.CASE_INSENSITIVE);
        }

        return pattern;
    }

    public static final boolean isMatch(String name, Pattern pattern) {
        if (name != null && pattern != null) {
            Matcher matcher = pattern.matcher(name);

            if (matcher.matches()) {
                return true;
            }
        }

        return false;
    }

    public static List<com.appdynamics.extensions.metrics.Metric> createMetricStatsMapForUpload(NamespaceMetricStatistics namespaceMetricStats,
                                                                                                Map<String, String> dimesionNameForMetricPathDictionary,
                                                                                                boolean useNamespaceAsPrefix) {
        List<com.appdynamics.extensions.metrics.Metric> stats = new ArrayList<>();

        if (namespaceMetricStats != null) {
            String namespacePrefix = null;

            if (useNamespaceAsPrefix) {
                namespacePrefix = buildMetricName(namespaceMetricStats.getNamespace(), "", true);

            } else {
                namespacePrefix = "";
            }

            processAccountMetricStatisticsList(namespacePrefix,
                    namespaceMetricStats.getAccountMetricStatisticsList(),
                    dimesionNameForMetricPathDictionary,
                    stats);
        }

        return stats;
    }

    private static void processAccountMetricStatisticsList(String namespacePrefix,
                                                           List<AccountMetricStatistics> accountStatsList,
                                                           Map<String, String> dimesionNameForMetricPathDictionary,
                                                           List<com.appdynamics.extensions.metrics.Metric> stats) {
        for (AccountMetricStatistics accountStats : accountStatsList) {
            // e.g. MyTestAccount|
            String accountPrefix = buildMetricName(namespacePrefix, accountStats.getAccountName(), true);
            processRegionMetricStatisticsList(accountPrefix, accountStats.getRegionMetricStatisticsList(),
                    dimesionNameForMetricPathDictionary, stats);
        }
    }

    private static void processRegionMetricStatisticsList(String accountPrefix,
                                                          List<RegionMetricStatistics> regionStatsList,
                                                          Map<String, String> dimesionNameForMetricPathDictionary,
                                                          List<com.appdynamics.extensions.metrics.Metric> stats) {
        for (RegionMetricStatistics regionStats : regionStatsList) {
            // e.g. MyTestAccount|us-east-1|
            String regionPrefix = buildMetricName(accountPrefix, regionStats.getRegion(), true);
            processMetricStatisticsList(regionPrefix, regionStats.getMetricStatisticsList(),
                    dimesionNameForMetricPathDictionary, stats);
        }
    }

    private static void processMetricStatisticsList(String regionPrefix,
                                                    List<MetricStatistic> metricStatsList,
                                                    Map<String, String> dimensionNameForMetricPathDictionary,
                                                    List<com.appdynamics.extensions.metrics.Metric> stats) {

        for (MetricStatistic metricStats : metricStatsList) {
            String partialMetricPath = regionPrefix;

            for (Dimension dimension : metricStats.getMetric().getMetric().getDimensions()) {
                String dimensionNameForMetricPath = getDimesionNameForMetricPath(dimension.getName(),
                        dimensionNameForMetricPathDictionary);

                // e.g. MyTestAccount|us-east-1|Cache Cluster|
                partialMetricPath = buildMetricName(partialMetricPath, dimensionNameForMetricPath, true);

                // e.g. MyTestAccount|us-east-1|Cache Cluster|mycachecluster
                partialMetricPath = buildMetricName(partialMetricPath, dimension.getValue(), true);
            }

            String awsMetricName = metricStats.getMetric().getIncludeMetric().getName();
            String metricPathWithoutPrefix = buildMetricName(partialMetricPath, awsMetricName, false);
            String fullMetricPath = buildMetricName(metricStats.getMetricPrefix(), metricPathWithoutPrefix, false);


            if (metricStats.getValue() != null) {

                /*
                 *  Commented out for now as unit isn't available when there's no datapoint,
                 *  so could result in duplicate metric registration when eventually it becomes available,
                 *  i.e. with and without unit
                 */
                //if (StringUtils.isNotBlank(metricStats.getUnit())) {
                //	awsMetricName = String.format("%s (%s)", awsMetricName, metricStats.getUnit());
                //}

                // e.g. MyTestAccount|us-east-1|Cache Cluster|mycachecluster|Cache Node|0001|CPUUtilization
                //String fullMetricPath = buildMetricName(partialMetricPath, awsMetricName, false);

                Map<String, Object> metricProperties = new HashMap<>();
                IncludeMetric metricWithConfig = metricStats.getMetric().getIncludeMetric();
                metricProperties.put("alias", metricWithConfig.getAlias());
                metricProperties.put("multiplier", metricWithConfig.getMultiplier());
                metricProperties.put("aggregationType", metricWithConfig.getAggregationType());
                metricProperties.put("timeRollUpType", metricWithConfig.getTimeRollUpType());
                metricProperties.put("clusterRollUpType", metricWithConfig.getClusterRollUpType());
                metricProperties.put("delta", metricWithConfig.isDelta());

                com.appdynamics.extensions.metrics.Metric metric = new com.appdynamics.extensions.metrics.Metric(awsMetricName, Double.toString(metricStats.getValue()),
                        fullMetricPath, metricProperties);
                stats.add(metric);
            } else {
                LOGGER.debug(String.format("Ignoring metric [ %s ] which has value null", fullMetricPath));
            }
        }
    }

    private static String getDimesionNameForMetricPath(String dimensionName, Map<String, String> dictionary) {
        String metricPathName = null;

        if (dictionary != null) {
            metricPathName = dictionary.get(dimensionName);
        }

        return StringUtils.isNotBlank(metricPathName) ? metricPathName : dimensionName;
    }

    private static String buildMetricName(String metricPrefix, String toAppend, boolean appendMetricSeparator) {
        if (appendMetricSeparator) {
            return String.format("%s%s%s", metricPrefix, toAppend, METRIC_PATH_SEPARATOR);
        }

        return String.format("%s%s", metricPrefix, toAppend);
    }

}
