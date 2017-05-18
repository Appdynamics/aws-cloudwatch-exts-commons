package com.appdynamics.extensions.aws.metric.processors;

import static com.appdynamics.extensions.aws.Constants.METRIC_PATH_SEPARATOR;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.DimensionFilter;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.appdynamics.extensions.aws.config.MetricType;
import com.appdynamics.extensions.aws.metric.AccountMetricStatistics;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.NamespaceMetricStatistics;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.aws.metric.StatisticType;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides default behaviour and other utility methods
 * that can used by concrete class of {@link MetricsProcessor}
 *
 * @author Florencio Sarmiento
 */
public class MetricsProcessorHelper {

    public static List<Metric> getFilteredMetrics(AmazonCloudWatch awsCloudWatch,
                                                  String namespace, Pattern excludeMetrics, String... dimensionNames) {
        List<Metric> metrics = getMetrics(awsCloudWatch, namespace, dimensionNames);
        return filterMetrics(metrics, excludeMetrics);
    }

    public static List<Metric> getFilteredMetrics(AmazonCloudWatch awsCloudWatch,
                                                  String namespace, Pattern excludeMetrics, List<DimensionFilter> dimensions) {
        List<Metric> metrics = getMetrics(awsCloudWatch, namespace, dimensions);
        return filterMetrics(metrics, excludeMetrics);
    }

    public static List<Metric> getFilteredMetrics(AmazonCloudWatch awsCloudWatch,
                                                  String namespace, Pattern excludeMetrics, List<DimensionFilter> dimensions, Predicate<Metric> metricFilter) {
        List<Metric> metrics = getMetrics(awsCloudWatch, namespace, dimensions);

        metrics = Lists.newArrayList(Collections2.filter(metrics, metricFilter));

        return filterMetrics(metrics, excludeMetrics);
    }


    public static List<Metric> getMetrics(AmazonCloudWatch awsCloudWatch,
                                          String namespace, String... dimensionNames) {
        List<DimensionFilter> dimensions = new ArrayList<DimensionFilter>();

        for (String dimensionName : dimensionNames) {
            DimensionFilter dimension = new DimensionFilter();
            dimension.withName(dimensionName);
            dimensions.add(dimension);
        }

        return getMetrics(awsCloudWatch, namespace, dimensions);
    }

    public static List<Metric> getMetrics(AmazonCloudWatch awsCloudWatch,
                                          String namespace, List<DimensionFilter> dimensions) {
        ListMetricsRequest request = new ListMetricsRequest();

        request.withNamespace(namespace);
        request.withDimensions(dimensions);
        ListMetricsResult listMetricsResult = awsCloudWatch.listMetrics(request);
        List<Metric> metrics = listMetricsResult.getMetrics();

        // Retrieves all the metrics if metricList > 500
        while (listMetricsResult.getNextToken() != null) {
            request.setNextToken(listMetricsResult.getNextToken());
            listMetricsResult = awsCloudWatch.listMetrics(request);
            metrics.addAll(listMetricsResult.getMetrics());
        }

        return metrics;
    }

    public static StatisticType getStatisticType(Metric metric, List<MetricType> metricTypes) {
        if (metricTypes != null && !metricTypes.isEmpty() && metric != null) {
            for (MetricType metricType : metricTypes) {
                Pattern pattern = Pattern.compile(metricType.getMetricName(),
                        Pattern.CASE_INSENSITIVE);

                if (isMatch(metric.getMetricName(), pattern)) {
                    return StatisticType.fromString(metricType.getStatType());
                }
            }
        }

        return StatisticType.AVE;
    }

    public static List<Metric> filterMetrics(List<Metric> metrics, Pattern excludeMetrics) {
        if (metrics != null && !metrics.isEmpty() && excludeMetrics != null) {
            List<Metric> filteredMetrics = new ArrayList<Metric>();

            for (Metric metric : metrics) {
                if (!isMatch(metric.getMetricName(), excludeMetrics)) {
                    filteredMetrics.add(metric);
                }
            }

            return filteredMetrics;
        }

        return metrics;
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

    public static Map<String, Double> createMetricStatsMapForUpload(NamespaceMetricStatistics namespaceMetricStats,
                                                                    Map<String, String> dimesionNameForMetricPathDictionary,
                                                                    boolean useNamespaceAsPrefix) {
        Map<String, Double> statsMap = new HashMap<String, Double>();

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
                    statsMap);
        }

        return statsMap;
    }

    private static void processAccountMetricStatisticsList(String namespacePrefix,
                                                           List<AccountMetricStatistics> accountStatsList,
                                                           Map<String, String> dimesionNameForMetricPathDictionary,
                                                           Map<String, Double> statsMap) {
        for (AccountMetricStatistics accountStats : accountStatsList) {
            // e.g. MyTestAccount|
            String accountPrefix = buildMetricName(namespacePrefix, accountStats.getAccountName(), true);
            processRegionMetricStatisticsList(accountPrefix, accountStats.getRegionMetricStatisticsList(),
                    dimesionNameForMetricPathDictionary, statsMap);
        }
    }

    private static void processRegionMetricStatisticsList(String accountPrefix,
                                                          List<RegionMetricStatistics> regionStatsList,
                                                          Map<String, String> dimesionNameForMetricPathDictionary,
                                                          Map<String, Double> statsMap) {
        for (RegionMetricStatistics regionStats : regionStatsList) {
            // e.g. MyTestAccount|us-east-1|
            String regionPrefix = buildMetricName(accountPrefix, regionStats.getRegion(), true);
            processMetricStatisticsList(regionPrefix, regionStats.getMetricStatisticsList(),
                    dimesionNameForMetricPathDictionary, statsMap);
        }
    }

    private static void processMetricStatisticsList(String regionPrefix,
                                                    List<MetricStatistic> metricStatsList,
                                                    Map<String, String> dimesionNameForMetricPathDictionary,
                                                    Map<String, Double> statsMap) {

        for (MetricStatistic metricStats : metricStatsList) {
            String partialMetricPath = regionPrefix;

            for (Dimension dimension : metricStats.getMetric().getDimensions()) {
                String dimesionNameForMetricPath = getDimesionNameForMetricPath(dimension.getName(),
                        dimesionNameForMetricPathDictionary);

                // e.g. MyTestAccount|us-east-1|Cache Cluster|
                partialMetricPath = buildMetricName(partialMetricPath, dimesionNameForMetricPath, true);

                // e.g. MyTestAccount|us-east-1|Cache Cluster|mycachecluster
                partialMetricPath = buildMetricName(partialMetricPath, dimension.getValue(), true);
            }

            String awsMetricName = metricStats.getMetric().getMetricName();

			/*
             *  Commented out for now as unit isn't available when there's no datapoint,
			 *  so could result in duplicate metric registration when eventually it becomes available, 
			 *  i.e. with and without unit
			 */
            //if (StringUtils.isNotBlank(metricStats.getUnit())) {
            //	awsMetricName = String.format("%s (%s)", awsMetricName, metricStats.getUnit());
            //}

            // e.g. MyTestAccount|us-east-1|Cache Cluster|mycachecluster|Cache Node|0001|CPUUtilization
            String fullMetricPath = buildMetricName(partialMetricPath, awsMetricName, false);
            statsMap.put(fullMetricPath, metricStats.getValue());
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
