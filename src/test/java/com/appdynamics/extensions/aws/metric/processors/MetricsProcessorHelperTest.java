/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.metric.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.appdynamics.extensions.aws.config.IncludeMetric;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.metric.AccountMetricStatistics;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.NamespaceMetricStatistics;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.aws.metric.StatisticType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

}
