/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.metric.processors;

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.amazonaws.services.resourcegroupstaggingapi.AWSResourceGroupsTaggingAPI;
import com.amazonaws.services.resourcegroupstaggingapi.AWSResourceGroupsTaggingAPIClientBuilder;
import com.amazonaws.services.resourcegroupstaggingapi.model.GetResourcesRequest;
import com.amazonaws.services.resourcegroupstaggingapi.model.GetResourcesResult;
import com.amazonaws.services.resourcegroupstaggingapi.model.ResourceTagMapping;
import com.amazonaws.services.resourcegroupstaggingapi.model.Tag;
import com.appdynamics.extensions.aws.config.IncludeMetric;
import com.appdynamics.extensions.aws.config.Tags;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.metric.*;
import com.appdynamics.extensions.aws.predicate.MultiDimensionPredicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GetResourcesRequest.class,AWSResourceGroupsTaggingAPIClientBuilder.class, AWSResourceGroupsTaggingAPI.class })
@PowerMockIgnore({"org.apache.*, javax.xml.*", "javax.net.ssl.*" })
public class MetricsProcessorHelperTest {

    @Test
    public void testGetStatisticTypeWithRegex() {
        IncludeMetric includeMetric = new IncludeMetric();
        includeMetric.setName("metricRequireSum\\d+");
        includeMetric.setStatType("sum");

        IncludeMetric includeMetric2 = new IncludeMetric();
        includeMetric2.setName("metricRequireMax");
        includeMetric2.setStatType("max");

        List<IncludeMetric> metrics = Lists.newArrayList(includeMetric, includeMetric2);

        IncludeMetric metric = new IncludeMetric();
        metric.setName("metricRequireSum1");

        StatisticType result = MetricsProcessorHelper.getStatisticType(metric, metrics);
        assertEquals(StatisticType.SUM, result);

        metric = new IncludeMetric();
        metric.setName("metricRequireMax");

        result = MetricsProcessorHelper.getStatisticType(metric, metrics);
        assertEquals(StatisticType.MAX, result);
    }

    @Test
    public void testGetStatisticDefaultsToAverage() {
        IncludeMetric metric = new IncludeMetric();
        metric.setName("testMetric1");

        StatisticType result = MetricsProcessorHelper.getStatisticType(metric, new ArrayList<IncludeMetric>());
        assertEquals(StatisticType.AVE, result);
    }

    @Test
    public void testFilterMetricsWithInclude() {

        List<IncludeMetric> includeMetrics = new ArrayList<>();

        IncludeMetric includeMetric = new IncludeMetric();
        includeMetric.setName("metricToKeep");

        includeMetrics.add(includeMetric);

        Metric metric1 = new Metric();
        metric1.setMetricName("metricToKeep");

        Metric metric2 = new Metric();
        metric2.setMetricName("metricToRemove");

        Metric metric3 = new Metric();
        metric3.setMetricName("metricUnwanted");

        List<Metric> testMetrics = Lists.newArrayList(metric1, metric2, metric3);

        List<AWSMetric> result = MetricsProcessorHelper.filterMetrics(testMetrics, includeMetrics);

        assertEquals(1, result.size());
        assertEquals(includeMetric, result.get(0).getIncludeMetric());
        assertEquals(metric1, result.get(0).getMetric());
    }

    @Test
    public void testCreateMetricStatsMapForUploadWithNamespaceAsPrefix() {
        NamespaceMetricStatistics testNamespaceStats = createTestNamespaceMetricStatistics("testNamespace");
        Map<String, String> testDimensionDictionary = Maps.newHashMap();
        testDimensionDictionary.put("testDimesionName", "My Test Dimension");

        List<com.appdynamics.extensions.metrics.Metric> result = MetricsProcessorHelper.createMetricStatsMapForUpload(
                testNamespaceStats, testDimensionDictionary, true);

        int index = 0;
        for (int accountIndex = 0; accountIndex < 2; accountIndex++) {
            for (int regionIndex = 0; regionIndex < 2; regionIndex++) {
                for (int metricIndex = 0; metricIndex < 2; metricIndex++) {
                    String expectedMetricName = String.format(
                            "Custom Metrics|AWS|testNamespace|account%s|region%s|My Test Dimension|testDimesionValue|testMetric%s",
                            accountIndex, regionIndex, metricIndex);

                    com.appdynamics.extensions.metrics.Metric metric = result.get(index++);
                    assertNotNull(metric);
                    assertEquals(expectedMetricName, metric.getMetricPath());
                }
            }
        }
    }

    @Test
    public void testCreateMetricStatsMapForUploadNotUsingNamespaceAsPrefixAndNullDimensionDictionary() {
        NamespaceMetricStatistics testNamespaceStats = createTestNamespaceMetricStatistics("testNamespace");
        List<com.appdynamics.extensions.metrics.Metric> result = MetricsProcessorHelper.createMetricStatsMapForUpload(testNamespaceStats, null, false);

        int index = 0;
        for (int accountIndex = 0; accountIndex < 2; accountIndex++) {
            for (int regionIndex = 0; regionIndex < 2; regionIndex++) {
                for (int metricIndex = 0; metricIndex < 2; metricIndex++) {
                    String expectedMetricName = String.format(
                            "Custom Metrics|AWS|account%s|region%s|testDimesionName|testDimesionValue|testMetric%s",
                            accountIndex, regionIndex, metricIndex);

                    com.appdynamics.extensions.metrics.Metric metric = result.get(index++);
                    assertNotNull(metric);
                    assertEquals(expectedMetricName, metric.getMetricPath());
                }
            }
        }
    }

    private NamespaceMetricStatistics createTestNamespaceMetricStatistics(String namespace) {
        NamespaceMetricStatistics namespaceStats = new NamespaceMetricStatistics();
        namespaceStats.setNamespace(namespace);

        for (int accountIndex = 0; accountIndex < 2; accountIndex++) {
            AccountMetricStatistics accountStats = new AccountMetricStatistics();
            accountStats.setAccountName("account" + accountIndex);

            for (int regionIndex = 0; regionIndex < 2; regionIndex++) {
                RegionMetricStatistics regionStats = new RegionMetricStatistics();
                regionStats.setRegion("region" + regionIndex);

                for (int metricIndex = 0; metricIndex < 2; metricIndex++) {
                    IncludeMetric includeMetric = new IncludeMetric();
                    includeMetric.setName("testMetric" + metricIndex);

                    Dimension dimension = new Dimension();
                    dimension.setName("testDimesionName");
                    dimension.setValue("testDimesionValue");

                    Metric metric = new Metric();
                    metric.setDimensions(Lists.newArrayList(dimension));

                    AWSMetric awsMetric = new AWSMetric();
                    awsMetric.setIncludeMetric(includeMetric);
                    awsMetric.setMetric(metric);

                    MetricStatistic metricStatistic = new MetricStatistic();
                    metricStatistic.setMetric(awsMetric);
                    metricStatistic.setValue(new Random().nextDouble());
                    metricStatistic.setUnit("testUnit");
                    metricStatistic.setMetricPrefix("Custom Metrics|AWS|");

                    regionStats.addMetricStatistic(metricStatistic);
                }

                accountStats.add(regionStats);
            }

            namespaceStats.add(accountStats);
        }

        return namespaceStats;
    }


    @Test
    public void filterUsingTagsTest() throws Exception {

        List<Dimension> dimensionsForListMetric = Lists.newArrayList();
        Dimension dimension1 = new Dimension();
        dimension1.setName("dimensionName1");
        dimension1.setValue("dimension1");

        Dimension dimension2 = new Dimension();
        dimension2.setName("dimensionName2");
        dimension2.setValue("dimension2");

        dimensionsForListMetric.add(dimension1);
        dimensionsForListMetric.add(dimension2);

        List<Metric> listMetricsResult = Lists.newArrayList();
        Metric awsMetric1 = new Metric()
                .withMetricName("testMetric1")
                .withNamespace("AWS/Namespace")
                .withDimensions(dimensionsForListMetric);
        listMetricsResult.add(awsMetric1);

        List<Tags> tags = Lists.newArrayList();
        Tags tag = new Tags();
        tag.setTagName("testTagKey");
        Set tagValue = Sets.newHashSet();
        tagValue.add("testTagValue");
        tag.setTagValue(tagValue);
        tags.add(tag);

        String dimensionUsedForFiltering = "dimensionName1";
        String resourceType = "redshift:cluster";
        String region = "us-west-1";


        List<com.appdynamics.extensions.aws.config.Dimension> dimensionsFromConfig =
                Lists.newArrayList();
        com.appdynamics.extensions.aws.config.Dimension configDimension1 = new com.appdynamics.extensions.aws.config.Dimension();
        configDimension1.setName("dimensionName1");
        Set dimensionValues1 = Sets.newHashSet();
        dimensionValues1.add("dimension1");
        configDimension1.setValues(dimensionValues1);
        dimensionsFromConfig.add(configDimension1);


        com.appdynamics.extensions.aws.config.Dimension configDimension2 = new com.appdynamics.extensions.aws.config.Dimension();
        configDimension2.setName("dimensionName2");
        Set dimensionValues2 = Sets.newHashSet();
        dimensionValues2.add("dimension2");
        configDimension2.setValues(dimensionValues2);
        dimensionsFromConfig.add(configDimension2);

        MultiDimensionPredicate dimensionPredicate = new MultiDimensionPredicate(dimensionsFromConfig);
        List<IncludeMetric> includeMetrics = Lists.newArrayList();
        IncludeMetric includeMetric = new IncludeMetric();
        includeMetric.setName("testMetric1");
        includeMetrics.add(includeMetric);

        Tag awsTag = new Tag().withKey("testTagKey").withValue("testTagValue");
        GetResourcesResult getResourcesResult = mock(GetResourcesResult.class);
        List<ResourceTagMapping> resourceTagMappingList = Lists.newArrayList();
        ResourceTagMapping resourceTagMapping = new ResourceTagMapping()
                .withResourceARN("arn:aws:redshift:us-west-1:12345672:cluster:dimension1")
                .withTags(awsTag);
        resourceTagMappingList.add(resourceTagMapping);
        getResourcesResult.setResourceTagMappingList(resourceTagMappingList);
        when(getResourcesResult.getResourceTagMappingList()).thenReturn(resourceTagMappingList);

        AWSResourceGroupsTaggingAPI taggingAPIClient = mock(AWSResourceGroupsTaggingAPI.class);
        AWSResourceGroupsTaggingAPIClientBuilder builder = mock(AWSResourceGroupsTaggingAPIClientBuilder.class);
        whenNew(AWSResourceGroupsTaggingAPIClientBuilder.class).withNoArguments().thenReturn(builder);
        when(builder.withRegion(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(taggingAPIClient);
        when(taggingAPIClient.getResources(any(GetResourcesRequest.class))).thenReturn(getResourcesResult);

        List<AWSMetric> result= MetricsProcessorHelper.filterUsingTags(listMetricsResult,tags,dimensionUsedForFiltering,resourceType,region,dimensionPredicate,includeMetrics);

        assertNotNull(result);

    }

}
