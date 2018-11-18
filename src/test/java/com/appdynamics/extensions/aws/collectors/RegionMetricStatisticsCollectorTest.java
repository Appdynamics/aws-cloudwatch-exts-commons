/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.collectors;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.model.*;
import com.appdynamics.extensions.aws.config.IncludeMetric;
import com.appdynamics.extensions.aws.config.MetricsTimeRange;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.aws.metric.StatisticType;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;
import com.appdynamics.extensions.aws.providers.RegionEndpointProvider;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.atomic.LongAdder;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RegionEndpointProvider.class,
        RegionMetricStatisticsCollector.class,
        ExecutorCompletionService.class,
        RateLimiter.class})
@PowerMockIgnore({"org.apache.*, javax.xml.*"})
public class RegionMetricStatisticsCollectorTest {

    private RegionMetricStatisticsCollector classUnderTest;

    @Mock
    private AmazonCloudWatchAsync mockAwsCloudWatchAsync;

    @Mock
    private AWSCredentials mockAWSCredentials;

    @Mock
    private ClientConfiguration mockAwsClientConfig;

    @Mock
    private MetricsProcessor mockMetricsProcessor;

    private LongAdder requestsCounter = new LongAdder();

    @Mock
    private RegionMetricStatisticsCollector.Builder mockBuilder1;

    @Test
    public void testNoMetricsToProcessReturnsEmptyStats() throws Exception {

        when(mockMetricsProcessor.getNamespace()).thenReturn("testNamespace");

        when(mockMetricsProcessor.getMetrics(eq(mockAwsCloudWatchAsync), anyString(), any(LongAdder.class))).thenReturn(new ArrayList<Metric>());

        String testRegion = "testRegion";

        classUnderTest = new RegionMetricStatisticsCollector.Builder()
                .setAmazonCloudWatchAsync(mockAwsCloudWatchAsync)
                .withMetricsProcessor(mockMetricsProcessor)
                .withThreadTimeOut(5)
                .withRegion(testRegion)
                .withAWSRequestCounter(requestsCounter)
                .build();

        List<Datapoint> point = Lists.newArrayList();
        point.add(new Datapoint().withUnit("testunit").withAverage(1.0));
        GetMetricStatisticsResult result = mock(GetMetricStatisticsResult.class);
        result.setDatapoints(point);

        when(mockAwsCloudWatchAsync.getMetricStatistics(any(GetMetricStatisticsRequest.class))).thenReturn(new GetMetricStatisticsResult());
        RegionMetricStatistics result1 = classUnderTest.call();
        assertEquals(testRegion, result1.getRegion());
        assertTrue(result1.getMetricStatisticsList().isEmpty());
    }

    @Test
    public void testMetricsRetrievalIsSuccessful() throws Exception {

        List<AWSMetric> testMetrics = getTestMetrics();
        List<Metric> metrics = getMetrics();

        MetricStatisticCollector mockMetricStatsCollector1 = mock(MetricStatisticCollector.class);
        MetricStatistic metricStatistic1 = createTestMetricStatistics(testMetrics.get(0));
        when(mockMetricStatsCollector1.call()).thenReturn(metricStatistic1);

        when(mockMetricsProcessor.getMetrics(mockAwsCloudWatchAsync, eq(anyString()), any(LongAdder.class))).thenReturn(metrics);
        when(mockMetricsProcessor.filterUsingTags(anyList(),anyList(),anyString())).thenReturn(testMetrics);
        when(mockMetricsProcessor.getStatisticType(any(AWSMetric.class))).thenReturn(StatisticType.AVE);
        when(mockMetricsProcessor.getNamespace()).thenReturn("testNamespace");

        String testRegion = "us-east-1";
        String testAccount = "testAccount";

        when(mockBuilder1.withAmazonCloudWatchConfig(mockAWSCredentials,mockAwsClientConfig,"us-east-1")).thenReturn(mockBuilder1);

        classUnderTest = new RegionMetricStatisticsCollector.Builder()

                .setAmazonCloudWatchAsync(mockAwsCloudWatchAsync)
                .withTags(Lists.newArrayList())
                .withMetricsProcessor(mockMetricsProcessor)
                .withMetricsTimeRange(new MetricsTimeRange())
                .withThreadTimeOut(5)
                .withAccountName(testAccount)
                .withPeriodInSeconds(60)
                .withRegion("us-east-1")
                .withRateLimiter(RateLimiter.create(400))
                .withAWSRequestCounter(requestsCounter)
                .withPrefix("Custom Metrics|AWS|")
                .build();

        List<Datapoint> point = Lists.newArrayList();
        point.add(new Datapoint().withUnit("testunit").withAverage(1.0));
        GetMetricStatisticsResult result = mock(GetMetricStatisticsResult.class);
        result.setDatapoints(point);

        when(mockAwsCloudWatchAsync.getMetricStatistics(any(GetMetricStatisticsRequest.class))).thenReturn(new GetMetricStatisticsResult());
        RegionMetricStatistics result1 = classUnderTest.call();
        assertEquals(metrics.size(), result1.getMetricStatisticsList().size());
        assertEquals(metricStatistic1.getMetric().getMetric(), result1.getMetricStatisticsList().get(0).getMetric().getMetric());

    }

    private  List<Metric> getMetrics() {

        List<Metric> metric = Lists.newArrayList();
        for(int i = 0 ; i < 1; i++){
            List<Dimension> dimension  = Lists.newArrayList();
            dimension.add(new Dimension().withName("testDimesionName"+i).withValue("testDimensionValue"+i));
            metric.add(new Metric().withMetricName("testMetric" +i).withNamespace("testNamespace").withDimensions(dimension));

        }
        return metric;
    }

    @Test
    public void testRateLimit() throws Exception {


        List<AWSMetric> testMetrics = getTestMetrics();
        List<Metric> metrics = getMetrics();
        List<AWSMetric> testAdditionalMetrics = getTestAdditionalMetrics();
        testMetrics.addAll(testAdditionalMetrics);

        when(mockMetricsProcessor.getMetrics(eq(mockAwsCloudWatchAsync), anyString(), any(LongAdder.class))).thenReturn(metrics);
        when(mockMetricsProcessor.getStatisticType(any(AWSMetric.class))).thenReturn(StatisticType.AVE);
        when(mockMetricsProcessor.getNamespace()).thenReturn("testNamespace");
        when(mockMetricsProcessor.getMetrics(mockAwsCloudWatchAsync, eq(anyString()), any(LongAdder.class))).thenReturn(metrics);
        when(mockMetricsProcessor.filterUsingTags(anyList(),anyList(),anyString())).thenReturn(testMetrics);
        when(mockMetricsProcessor.getStatisticType(any(AWSMetric.class))).thenReturn(StatisticType.AVE);
        when(mockMetricsProcessor.getNamespace()).thenReturn("testNamespace");

        String testRegion = "us-east-1";
        String testAccount = "testAccount";

        when(mockBuilder1.withAmazonCloudWatchConfig(mockAWSCredentials,mockAwsClientConfig,"us-east-1")).
                thenReturn(mockBuilder1);

        List<Datapoint> point = Lists.newArrayList();
        point.add(new Datapoint().withUnit("testunit").withAverage(1.0));
        GetMetricStatisticsResult result = mock(GetMetricStatisticsResult.class);
        result.setDatapoints(point);

        when(mockAwsCloudWatchAsync.getMetricStatistics(any(GetMetricStatisticsRequest.class))).thenReturn(new GetMetricStatisticsResult());

        RateLimiter rateLimiter = RateLimiter.create(1);

        classUnderTest = new RegionMetricStatisticsCollector.Builder()
                .setAmazonCloudWatchAsync(mockAwsCloudWatchAsync)
                .withMetricsProcessor(mockMetricsProcessor)
                .withMetricsTimeRange(new MetricsTimeRange())
                .withAccountName(testAccount)
                .withThreadTimeOut(5)
                .withRegion(testRegion)
                .withRateLimiter(rateLimiter)
                .withAWSRequestCounter(requestsCounter)
                .withPrefix("Custom Metrics|AWS|")
                .build();

        classUnderTest.call();
        boolean canAcquire = rateLimiter.tryAcquire();
        assertFalse("Should not be able to acquire", canAcquire);
        System.out.println("Waiting for rate limit to pass");

        Thread.currentThread().sleep(1000);

        canAcquire = rateLimiter.tryAcquire();
        assertTrue("Should be able to acquire", canAcquire);
    }

    private List<AWSMetric> getTestMetrics() {
        List<AWSMetric> testMetrics = Lists.newArrayList();

        for (int index = 0; index < 1; index++) {
            IncludeMetric includeMetric = new IncludeMetric();
            includeMetric.setName("testMetric" + index);

            Metric metric = new Metric();
            metric.setNamespace("testNamespace");
            metric.setMetricName("testMetric" + index);

            Dimension dimension = new Dimension();
            dimension.setName("testDimesionName" + index);
            dimension.setValue("testDimesionValue" + index);

            metric.setDimensions(Lists.newArrayList(dimension));


            AWSMetric awsMetric = new AWSMetric();
            awsMetric.setIncludeMetric(includeMetric);
            awsMetric.setMetric(metric);

            testMetrics.add(awsMetric);
        }

        return testMetrics;
    }
//
    private List<AWSMetric> getTestAdditionalMetrics() {
        List<AWSMetric> testMetrics = Lists.newArrayList();

        for (int index = 0; index < 2; index++) {
            IncludeMetric includeMetric = new IncludeMetric();
            includeMetric.setName("testAddiMetric" + index);

            Metric metric = new Metric();
            metric.setNamespace("testNamespace");
            metric.setMetricName("testAddiMetric" + index);

            Dimension dimension = new Dimension();
            dimension.setName("testDimesionName" + index);
            dimension.setValue("testDimesionValue" + index);

            metric.setDimensions(Lists.newArrayList(dimension));


            AWSMetric awsMetric = new AWSMetric();
            awsMetric.setIncludeMetric(includeMetric);
            awsMetric.setMetric(metric);

            testMetrics.add(awsMetric);
        }

        return testMetrics;
    }
//
    private MetricStatistic createTestMetricStatistics(AWSMetric metric) {
        MetricStatistic metricStatistic = new MetricStatistic();
        metricStatistic.setMetric(metric);
        metricStatistic.setValue(new Random().nextDouble());
        metricStatistic.setUnit("testUnit");
        return metricStatistic;
    }
}