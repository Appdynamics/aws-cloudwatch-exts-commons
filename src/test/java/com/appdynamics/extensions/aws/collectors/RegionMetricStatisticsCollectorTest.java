/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.appdynamics.extensions.MonitorThreadPoolExecutor;
import com.appdynamics.extensions.aws.config.IncludeMetric;
import com.appdynamics.extensions.aws.config.MetricsTimeRange;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.exceptions.AwsException;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.aws.metric.StatisticType;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;
import com.appdynamics.extensions.aws.providers.RegionEndpointProvider;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.LongAdder;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RegionEndpointProvider.class,
        RegionMetricStatisticsCollector.class,
        ExecutorCompletionService.class,
        RateLimiter.class})
@PowerMockIgnore({"org.apache.*, javax.xml.*"})
public class RegionMetricStatisticsCollectorTest {

    private RegionMetricStatisticsCollector classUnderTest;

    @Mock
    private AmazonCloudWatchClient mockAwsCloudWatch;

    @Mock
    private AWSCredentials mockAWSCredentials;

    @Mock
    private ClientConfiguration mockAwsClientConfig;

    @Mock
    private MetricsProcessor mockMetricsProcessor;

    private LongAdder requestsCounter = new LongAdder();

    @Mock
    private RegionEndpointProvider mockRegionEndpointProvider;

    @Before
    public void setUp() throws Exception {
        mockStatic(RegionEndpointProvider.class);
        when(RegionEndpointProvider.getInstance()).thenReturn(mockRegionEndpointProvider);
        whenNew(AmazonCloudWatchClient.class).withArguments(mockAWSCredentials, mockAwsClientConfig)
                .thenReturn(mockAwsCloudWatch);
    }

    @Test(expected = AwsException.class)
    public void testInvalidRegionThrowsException() throws Exception {
        when(mockRegionEndpointProvider.getEndpoint(anyString())).thenReturn(null);

        classUnderTest = new RegionMetricStatisticsCollector.Builder()
                .withAmazonCloudWatchConfig(mockAWSCredentials, mockAwsClientConfig)
                .withMetricsProcessor(mockMetricsProcessor)
                .withRegion("invalid")
                .build();

        classUnderTest.call();
    }

    @Test
    public void testNoMetricsToProcessReturnsEmptyStats() throws Exception {

        when(mockMetricsProcessor.getNamespace()).thenReturn("testNamespace");
        when(mockRegionEndpointProvider.getEndpoint(anyString())).thenReturn("test-endpoint");
        when(mockMetricsProcessor.getMetrics(eq(mockAwsCloudWatch), anyString(), any(LongAdder.class))).thenReturn(new ArrayList<AWSMetric>());

        String testRegion = "testRegion";

        classUnderTest = new RegionMetricStatisticsCollector.Builder()
                .withAmazonCloudWatchConfig(mockAWSCredentials, mockAwsClientConfig)
                .withMetricsProcessor(mockMetricsProcessor)
                .withRegion(testRegion)
                .withAWSRequestCounter(requestsCounter)
                .build();

        RegionMetricStatistics result = classUnderTest.call();

        assertEquals(testRegion, result.getRegion());
        assertTrue(result.getMetricStatisticsList().isEmpty());
    }

    @Test
    public void testMetricsRetrievalIsSuccessful() throws Exception {
        when(mockRegionEndpointProvider.getEndpoint(anyString())).thenReturn("test-endpoint");

        List<AWSMetric> testMetrics = getTestMetrics();
        when(mockMetricsProcessor.getMetrics(eq(mockAwsCloudWatch), anyString(), any(LongAdder.class))).thenReturn(testMetrics);
        when(mockMetricsProcessor.getStatisticType(any(AWSMetric.class))).thenReturn(StatisticType.AVE);
        when(mockMetricsProcessor.getNamespace()).thenReturn("testNamespace");


        MetricStatisticCollector mockMetricStatsCollector1 = mock(MetricStatisticCollector.class);
        MetricStatistic metricStatistic1 = createTestMetricStatistics(testMetrics.get(0));
        when(mockMetricStatsCollector1.call()).thenReturn(metricStatistic1);

        MetricStatisticCollector mockMetricStatsCollector2 = mock(MetricStatisticCollector.class);
        MetricStatistic metricStatistic2 = createTestMetricStatistics(testMetrics.get(1));
        when(mockMetricStatsCollector2.call()).thenReturn(metricStatistic2);

        // simulate creation of metric stats collector
        MetricStatisticCollector.Builder mockBuilder = mock(MetricStatisticCollector.Builder.class);
        whenNew(MetricStatisticCollector.Builder.class).withNoArguments().thenReturn(mockBuilder);
        when(mockBuilder.withAccountName(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.withAwsCloudWatch(mockAwsCloudWatch)).thenReturn(mockBuilder);
        when(mockBuilder.withMetric(any(AWSMetric.class))).thenReturn(mockBuilder);
        when(mockBuilder.withMetricsTimeRange(any(MetricsTimeRange.class))).thenReturn(mockBuilder);
        when(mockBuilder.withRegion(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.withStatType(any(StatisticType.class))).thenReturn(mockBuilder);
        when(mockBuilder.withAWSRequestCounter(requestsCounter)).thenReturn(mockBuilder);
        when(mockBuilder.withPrefix(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockMetricStatsCollector1, mockMetricStatsCollector2);


        String testRegion = "testRegion";
        String testAccount = "testAccount";

        classUnderTest = new RegionMetricStatisticsCollector.Builder()
                .withAmazonCloudWatchConfig(mockAWSCredentials, mockAwsClientConfig)
                .withMetricsProcessor(mockMetricsProcessor)
                .withMetricsTimeRange(new MetricsTimeRange())
                .withAccountName(testAccount)
                .withRegion(testRegion)
                .withRateLimiter(RateLimiter.create(400))
                .withAWSRequestCounter(requestsCounter)
                .withPrefix("Custom Metrics|AWS|")
                .build();

        RegionMetricStatistics result = classUnderTest.call();

        assertEquals(testRegion, result.getRegion());
        assertEquals(testMetrics.size(), result.getMetricStatisticsList().size());
        assertEquals(metricStatistic1, result.getMetricStatisticsList().get(0));
        assertEquals(metricStatistic2, result.getMetricStatisticsList().get(1));
    }

    @Test
    public void testRateLimit() throws Exception {
        when(mockRegionEndpointProvider.getEndpoint(anyString())).thenReturn("test-endpoint");

        List<AWSMetric> testMetrics = getTestMetrics();
        List<AWSMetric> testAdditionalMetrics = getTestAdditionalMetrics();

        testMetrics.addAll(testAdditionalMetrics);

        when(mockMetricsProcessor.getMetrics(eq(mockAwsCloudWatch), anyString(), any(LongAdder.class))).thenReturn(testMetrics);
        when(mockMetricsProcessor.getStatisticType(any(AWSMetric.class))).thenReturn(StatisticType.AVE);
        when(mockMetricsProcessor.getNamespace()).thenReturn("testNamespace");


        MetricStatisticCollector mockMetricStatsCollector1 = mock(MetricStatisticCollector.class);
        MetricStatistic metricStatistic1 = createTestMetricStatistics(testMetrics.get(0));
        when(mockMetricStatsCollector1.call()).thenReturn(metricStatistic1);

        MetricStatisticCollector mockMetricStatsCollector2 = mock(MetricStatisticCollector.class);
        MetricStatistic metricStatistic2 = createTestMetricStatistics(testMetrics.get(1));
        when(mockMetricStatsCollector2.call()).thenReturn(metricStatistic2);

        MetricStatisticCollector mockMetricStatsCollector3 = mock(MetricStatisticCollector.class);
        MetricStatistic metricStatistic3 = createTestMetricStatistics(testMetrics.get(2));
        when(mockMetricStatsCollector3.call()).thenReturn(metricStatistic3);

        MetricStatisticCollector mockMetricStatsCollector4 = mock(MetricStatisticCollector.class);
        MetricStatistic metricStatistic4 = createTestMetricStatistics(testMetrics.get(3));
        when(mockMetricStatsCollector4.call()).thenReturn(metricStatistic4);

        // simulate creation of metric stats collector
        MetricStatisticCollector.Builder mockBuilder = mock(MetricStatisticCollector.Builder.class);
        whenNew(MetricStatisticCollector.Builder.class).withNoArguments().thenReturn(mockBuilder);
        when(mockBuilder.withAccountName(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.withAwsCloudWatch(mockAwsCloudWatch)).thenReturn(mockBuilder);
        when(mockBuilder.withMetric(any(AWSMetric.class))).thenReturn(mockBuilder);
        when(mockBuilder.withMetricsTimeRange(any(MetricsTimeRange.class))).thenReturn(mockBuilder);
        when(mockBuilder.withRegion(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.withStatType(any(StatisticType.class))).thenReturn(mockBuilder);
        when(mockBuilder.withAWSRequestCounter(requestsCounter)).thenReturn(mockBuilder);
        when(mockBuilder.withPrefix(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockMetricStatsCollector1, mockMetricStatsCollector2, mockMetricStatsCollector3, mockMetricStatsCollector4);


        String testRegion = "testRegion";
        String testAccount = "testAccount";


        RateLimiter rateLimiter = RateLimiter.create(1);

        classUnderTest = new RegionMetricStatisticsCollector.Builder()
                .withAmazonCloudWatchConfig(mockAWSCredentials, mockAwsClientConfig)
                .withMetricsProcessor(mockMetricsProcessor)
                .withMetricsTimeRange(new MetricsTimeRange())
                .withAccountName(testAccount)
                .withRegion(testRegion)
                .withRateLimiter(rateLimiter)
                .withAWSRequestCounter(requestsCounter)
                .withPrefix("Custom Metrics|AWS|")
                .build();

        classUnderTest.call();

        boolean canAcquire = rateLimiter.tryAcquire();
        assertFalse("Should not be able to acquire", canAcquire);

        System.out.println("Waiting for rate limit to pass");

        Thread.sleep(1000);

        canAcquire = rateLimiter.tryAcquire();
        assertTrue("Should be able to acquire", canAcquire);
    }

    private List<AWSMetric> getTestMetrics() {
        List<AWSMetric> testMetrics = Lists.newArrayList();

        for (int index = 0; index < 2; index++) {
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

    @Test
    public void testThreadPoolShutdown() throws Exception {
        when(mockRegionEndpointProvider.getEndpoint(anyString())).thenReturn("test-endpoint");

        List<AWSMetric> testMetrics = getTestMetrics();
        when(mockMetricsProcessor.getMetrics(eq(mockAwsCloudWatch), anyString(), any(LongAdder.class))).thenReturn(testMetrics);
        when(mockMetricsProcessor.getStatisticType(any(AWSMetric.class))).thenReturn(StatisticType.AVE);
        when(mockMetricsProcessor.getNamespace()).thenReturn("testNamespace");


        MetricStatisticCollector mockMetricStatsCollector1 = mock(MetricStatisticCollector.class);
        MetricStatistic metricStatistic1 = createTestMetricStatistics(testMetrics.get(0));
        when(mockMetricStatsCollector1.call()).thenReturn(metricStatistic1);

        MetricStatisticCollector mockMetricStatsCollector2 = mock(MetricStatisticCollector.class);
        MetricStatistic metricStatistic2 = createTestMetricStatistics(testMetrics.get(1));
        when(mockMetricStatsCollector2.call()).thenReturn(metricStatistic2);

        // simulate creation of metric stats collector
        MetricStatisticCollector.Builder mockBuilder = mock(MetricStatisticCollector.Builder.class);
        whenNew(MetricStatisticCollector.Builder.class).withNoArguments().thenReturn(mockBuilder);
        when(mockBuilder.withAccountName(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.withAwsCloudWatch(mockAwsCloudWatch)).thenReturn(mockBuilder);
        when(mockBuilder.withMetric(any(AWSMetric.class))).thenReturn(mockBuilder);
        when(mockBuilder.withMetricsTimeRange(any(MetricsTimeRange.class))).thenReturn(mockBuilder);
        when(mockBuilder.withRegion(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.withStatType(any(StatisticType.class))).thenReturn(mockBuilder);
        when(mockBuilder.withAWSRequestCounter(requestsCounter)).thenReturn(mockBuilder);
        when(mockBuilder.withPrefix(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockMetricStatsCollector1, mockMetricStatsCollector2);


        String testRegion = "testRegion";
        String testAccount = "testAccount";

        classUnderTest = new RegionMetricStatisticsCollector.Builder()
                .withAmazonCloudWatchConfig(mockAWSCredentials, mockAwsClientConfig)
                .withMetricsProcessor(mockMetricsProcessor)
                .withMetricsTimeRange(new MetricsTimeRange())
                .withAccountName(testAccount)
                .withRegion(testRegion)
                .withRateLimiter(RateLimiter.create(400))
                .withAWSRequestCounter(requestsCounter)
                .withPrefix("Custom Metrics|AWS|")
                .build();

        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(2);
        MonitorThreadPoolExecutor executorService = new MonitorThreadPoolExecutor(scheduledThreadPoolExecutor);

        MonitorThreadPoolExecutor executorServiceSpy = Mockito.spy(executorService);

        whenNew(MonitorThreadPoolExecutor.class).withArguments(any(ScheduledThreadPoolExecutor.class)).thenReturn(executorServiceSpy);

        RegionMetricStatistics result = classUnderTest.call();

        assertEquals(testRegion, result.getRegion());
        assertEquals(testMetrics.size(), result.getMetricStatisticsList().size());
        assertEquals(metricStatistic1, result.getMetricStatisticsList().get(0));
        assertEquals(metricStatistic2, result.getMetricStatisticsList().get(1));

        //Test if shutdown was called once on MonitorThreadPoolExecutor
        verify(executorServiceSpy, Mockito.times(1)).shutdown();
    }

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

    private MetricStatistic createTestMetricStatistics(AWSMetric metric) {
        MetricStatistic metricStatistic = new MetricStatistic();
        metricStatistic.setMetric(metric);
        metricStatistic.setValue(new Random().nextDouble());
        metricStatistic.setUnit("testUnit");
        return metricStatistic;
    }
}