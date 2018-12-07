/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.metric.processors;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.resourcegroupstaggingapi.AWSResourceGroupsTaggingAPI;
import com.amazonaws.services.resourcegroupstaggingapi.AWSResourceGroupsTaggingAPIClientBuilder;
import com.amazonaws.services.resourcegroupstaggingapi.model.*;
import com.appdynamics.extensions.aws.config.IncludeMetric;
import com.appdynamics.extensions.aws.config.Tags;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.metric.*;
import com.appdynamics.extensions.aws.predicate.MultiDimensionPredicate;
import com.appdynamics.extensions.aws.predicate.TagsPredicate;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.appdynamics.extensions.aws.Constants.METRIC_PATH_SEPARATOR;


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
                                                     LongAdder awsRequestsCounter, String namespace, List<IncludeMetric> includeMetrics) {
        List<Metric> metrics = getMetrics(awsCloudWatch, awsRequestsCounter, namespace);
        return filterMetrics(metrics, includeMetrics);
    }

    public static List<Metric> getFilteredMetrics(AmazonCloudWatch awsCloudWatch,
                                                     LongAdder awsRequestsCounter, String namespace) {
       return getMetrics(awsCloudWatch, awsRequestsCounter, namespace);
    }


    public static List<Metric> getMetrics(AmazonCloudWatch awsCloudWatch,
                                          LongAdder awsRequestsCounter, String namespace, String... dimensionNames) {
        List<DimensionFilter> dimensions = new ArrayList<DimensionFilter>();

        for (String dimensionName : dimensionNames) {
            DimensionFilter dimension = new DimensionFilter();
            dimension.withName(dimensionName);
            dimensions.add(dimension);
        }

        return getMetrics(awsCloudWatch, awsRequestsCounter, namespace);
    }

    public static List<Metric> getMetrics(AmazonCloudWatch awsCloudWatch,
                                          LongAdder awsRequestsCounter, String namespace) {
        ListMetricsRequest request = new ListMetricsRequest();

        request.withNamespace(namespace);
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

    public static Set<String> filterUsingTags(List<Tags> tags,
                                              String resourceType,
                                              String region, int position
                                                ) {
        List<TagFilter> tagFilters;
        Set<String> resources = Sets.newHashSet();
        TagsPredicate tagPredicate = new TagsPredicate(tags);

        if(tags == null || tags.isEmpty()){ //tag filtering disabled
            LOGGER.debug("No tags specified in config.yml");
        }
        else {
            tagFilters = createTagFilters(tags);
            if (!tagFilters.isEmpty()){
                GetResourcesResult result = getResources(resourceType, region, tagFilters);
                if (result.getResourceTagMappingList().size() > 0) {
                    resources = filterResourcesUsingTags(tagPredicate, result, position);
                }
                else {
                    LOGGER.warn("Received empty list of tags from AWS");
                }
            }
            else {
                LOGGER.warn("Tag names in config.yml are empty");
            }
        }
        return resources;
    }

    public static List<AWSMetric> listMetricsFromFilteredResources (List<Metric> listMetricsResult,
                                                                  String dimensionUsedForFiltering,
                                                                  Set<String> resources,
                                                                 MultiDimensionPredicate dimensionPredicate,
                                                                 List<IncludeMetric> includeMetrics
                                                                  ) {
        List<Metric> filteredList = Lists.newArrayList();
        List<Metric> tagFilteredMetricList = Lists.newArrayList();
        for (String resource : resources) {
            Dimension dimension = new Dimension().withName(dimensionUsedForFiltering).withValue(resource);
            tagFilteredMetricList.add(new Metric().withDimensions(dimension));
        }
        for (Metric metric : tagFilteredMetricList) {
            for (Metric awsMetric : listMetricsResult) {
                if ((awsMetric.getDimensions()).containsAll(metric.getDimensions())) {
                    filteredList.add(awsMetric);
                }
            }
        }


        if(!filteredList.isEmpty()) {
            listMetricsResult = Lists.newArrayList(Collections2.filter(filteredList, dimensionPredicate));
        }
        else { //tagging disabled
            listMetricsResult = Lists.newArrayList(Collections2.filter(listMetricsResult, dimensionPredicate));
        }
        return filterMetrics(listMetricsResult, includeMetrics);
    }

    private static Set<String> filterResourcesUsingTags (Predicate<Tag> tagPredicate, GetResourcesResult result, int position) {
        List<ResourceTagMapping> resourceTagMappingList = result.getResourceTagMappingList();
        Set<String> resources = Sets.newHashSet();
        for (ResourceTagMapping arn : resourceTagMappingList) {
            List<Tag> tag = arn.getTags();
            Collection<Tag> tagsFromArnAfterFilter = Collections2.filter(tag, tagPredicate);
            if (tagsFromArnAfterFilter.size() > 0) {
                resources.add(extractResourceNameFromARN(arn.getResourceARN(), position));
            }
        }
        return resources;
    }

    private static GetResourcesResult getResources (String resourceType, String region, List<TagFilter> tagFilters) {
        GetResourcesRequest request = new GetResourcesRequest().withTagFilters(tagFilters)
                .withResourceTypeFilters(resourceType);
        AWSResourceGroupsTaggingAPI taggingAPIClient = AWSResourceGroupsTaggingAPIClientBuilder
                                                        .standard().withRegion(region).build();

        GetResourcesResult result =  taggingAPIClient.getResources(request);
        return result;
    }

    private static List<TagFilter> createTagFilters (List<Tags> tags) {
        List<TagFilter> tagFilters = Lists.newArrayList();
        for (Tags tag : tags) {
            if (StringUtils.isNotEmpty(tag.getTagName())) {
                tagFilters.add(new TagFilter().withKey(tag.getTagName()));
            }
        }
        return tagFilters;
    }

    private static String extractResourceNameFromARN(String resourceARN, int position) {

        resourceARN = resourceARN.split(":")[position];
        String resource = resourceARN.contains("/") ? resourceARN.substring(resourceARN.lastIndexOf("/") + 1,
        resourceARN.length()) : resourceARN.substring(resourceARN.lastIndexOf(":") + 1,
        resourceARN.length()) ;
        LOGGER.debug("Extracting Resource Name from ARN:"+resource);
        return resource;
    }
}
