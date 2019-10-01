/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.appdynamics.extensions.executorservice.MonitorThreadPoolExecutor;
import com.appdynamics.extensions.aws.config.Account;
import com.appdynamics.extensions.aws.config.IncludeMetric;
import com.appdynamics.extensions.aws.config.MetricsTimeRange;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.exceptions.AwsException;
import com.appdynamics.extensions.aws.metric.AccountMetricStatistics;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.LongAdder;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AccountMetricStatisticsCollector.class,
        ExecutorCompletionService.class})
@PowerMockIgnore({"org.apache.*, javax.xml.*"})
public class AccountMetricStatisticsCollectorTest {

    private AccountMetricStatisticsCollector classUnderTest;

    @Mock
    private MetricsProcessor mockMetricsProcessor;

    private LongAdder requestCounter = new LongAdder();

    @Test(expected = AwsException.class)
    public void testIncompleteAccountThrowsException() throws Exception {
        classUnderTest = new AccountMetricStatisticsCollector.Builder()
                .withAccount(new Account())
                .withMetricsProcessor(mockMetricsProcessor)
                .withAWSRequestCounter(requestCounter)
                .build();

        classUnderTest.call();
    }

    @Test()
    public void testMetricsRetrievalIsSuccessful() throws Exception {
        Account testAccount = new Account();
        testAccount.setAwsAccessKey("testAccessKey");
        testAccount.setAwsSecretKey("testAwsSecretKey");

        when(mockMetricsProcessor.getNamespace()).thenReturn("testNamespace");


        testAccount.setDisplayAccountName("TestAccount");

        Set<String> testRegions = Sets.newHashSet("region1", "region2");
        testAccount.setRegions(testRegions);

        RegionMetricStatisticsCollector mockRegionStatsCollector1 = mock(RegionMetricStatisticsCollector.class);
        RegionMetricStatistics regionStats1 = createTestMetricStatistics("region1");
        when(mockRegionStatsCollector1.call()).thenReturn(regionStats1);

        RegionMetricStatisticsCollector mockRegionStatsCollector2 = mock(RegionMetricStatisticsCollector.class);
        RegionMetricStatistics regionStats2 = createTestMetricStatistics("region2");
        when(mockRegionStatsCollector2.call()).thenReturn(regionStats2);

        // simulate region stats collector creation
        RegionMetricStatisticsCollector.Builder mockBuilder = mock(RegionMetricStatisticsCollector.Builder.class);
        whenNew(RegionMetricStatisticsCollector.Builder.class).withNoArguments().thenReturn(mockBuilder);
        when(mockBuilder.withAccountName(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.withAmazonCloudWatchConfig(any(AWSCredentials.class), any(ClientConfiguration.class)))
                .thenReturn(mockBuilder);
        when(mockBuilder.withMetricsProcessor(any(MetricsProcessor.class))).thenReturn(mockBuilder);
        when(mockBuilder.withMetricsTimeRange(any(MetricsTimeRange.class))).thenReturn(mockBuilder);
        when(mockBuilder.withNoOfMetricThreadsPerRegion(anyInt())).thenReturn(mockBuilder);
        when(mockBuilder.withThreadTimeOut(anyInt())).thenReturn(mockBuilder);
        when(mockBuilder.withRegion(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.withRateLimiter(any(RateLimiter.class))).thenReturn(mockBuilder);
        when(mockBuilder.withAWSRequestCounter(any(LongAdder.class))).thenReturn(mockBuilder);
        when(mockBuilder.withPrefix(anyString())).thenReturn(mockBuilder);

        when(mockBuilder.build()).thenReturn(mockRegionStatsCollector1, mockRegionStatsCollector2);

        classUnderTest = new AccountMetricStatisticsCollector.Builder()
                .withAccount(testAccount)
                .withMetricsProcessor(mockMetricsProcessor)
                .withMaxErrorRetrySize(1)
                .withMetricsTimeRange(new MetricsTimeRange())
                .withNoOfMetricThreadsPerRegion(1)
                .withNoOfRegionThreadsPerAccount(2)
                .withRateLimiter(RateLimiter.create(400))
                .withAWSRequestCounter(requestCounter)
                .withThreadTimeOut(3000)
                .build();

        AccountMetricStatistics result = classUnderTest.call();
        assertEquals(testAccount.getDisplayAccountName(), result.getAccountName());
        assertEquals(regionStats1, result.getRegionMetricStatisticsList().get(0));
        assertEquals(regionStats2, result.getRegionMetricStatisticsList().get(1));
    }

    private RegionMetricStatistics createTestMetricStatistics(String region) {
        RegionMetricStatistics regionStats = new RegionMetricStatistics();
        regionStats.setRegion(region);

        Random random = new Random();

        for (int index = 0; index < random.nextInt(10); index++) {
            IncludeMetric metric = new IncludeMetric();
            metric.setName("testMetric" + index);

            AWSMetric awsMetric = new AWSMetric();
            awsMetric.setIncludeMetric(metric);

            MetricStatistic metricStatistic = new MetricStatistic();
            metricStatistic.setMetric(awsMetric);
            metricStatistic.setValue(new Random().nextDouble());
            metricStatistic.setUnit("testUnit");

            regionStats.addMetricStatistic(metricStatistic);
        }

        return regionStats;
    }

    @Test()
    public void testThreadPoolShutdown() throws Exception {
        Account testAccount = new Account();
        testAccount.setAwsAccessKey("testAccessKey");
        testAccount.setAwsSecretKey("testAwsSecretKey");

        when(mockMetricsProcessor.getNamespace()).thenReturn("testNamespace");


        testAccount.setDisplayAccountName("TestAccount");

        Set<String> testRegions = Sets.newHashSet("region1", "region2");
        testAccount.setRegions(testRegions);

        RegionMetricStatisticsCollector mockRegionStatsCollector1 = mock(RegionMetricStatisticsCollector.class);
        RegionMetricStatistics regionStats1 = createTestMetricStatistics("region1");
        when(mockRegionStatsCollector1.call()).thenReturn(regionStats1);

        RegionMetricStatisticsCollector mockRegionStatsCollector2 = mock(RegionMetricStatisticsCollector.class);
        RegionMetricStatistics regionStats2 = createTestMetricStatistics("region2");
        when(mockRegionStatsCollector2.call()).thenReturn(regionStats2);

        // simulate region stats collector creation
        RegionMetricStatisticsCollector.Builder mockBuilder = mock(RegionMetricStatisticsCollector.Builder.class);
        whenNew(RegionMetricStatisticsCollector.Builder.class).withNoArguments().thenReturn(mockBuilder);
        when(mockBuilder.withAccountName(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.withAmazonCloudWatchConfig(any(AWSCredentials.class), any(ClientConfiguration.class)))
                .thenReturn(mockBuilder);
        when(mockBuilder.withMetricsProcessor(any(MetricsProcessor.class))).thenReturn(mockBuilder);
        when(mockBuilder.withMetricsTimeRange(any(MetricsTimeRange.class))).thenReturn(mockBuilder);
        when(mockBuilder.withNoOfMetricThreadsPerRegion(anyInt())).thenReturn(mockBuilder);
        when(mockBuilder.withThreadTimeOut(anyInt())).thenReturn(mockBuilder);
        when(mockBuilder.withRegion(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.withRateLimiter(any(RateLimiter.class))).thenReturn(mockBuilder);
        when(mockBuilder.withAWSRequestCounter(any(LongAdder.class))).thenReturn(mockBuilder);
        when(mockBuilder.withPrefix(anyString())).thenReturn(mockBuilder);

        when(mockBuilder.build()).thenReturn(mockRegionStatsCollector1, mockRegionStatsCollector2);

        classUnderTest = new AccountMetricStatisticsCollector.Builder()
                .withAccount(testAccount)
                .withMetricsProcessor(mockMetricsProcessor)
                .withMaxErrorRetrySize(1)
                .withMetricsTimeRange(new MetricsTimeRange())
                .withNoOfMetricThreadsPerRegion(1)
                .withNoOfRegionThreadsPerAccount(2)
                .withRateLimiter(RateLimiter.create(400))
                .withAWSRequestCounter(requestCounter)
                .withThreadTimeOut(3000)
                .build();

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        MonitorThreadPoolExecutor executorService = new MonitorThreadPoolExecutor(threadPoolExecutor);

        MonitorThreadPoolExecutor executorServiceSpy = Mockito.spy(executorService);

        whenNew(MonitorThreadPoolExecutor.class).withArguments(any(ScheduledThreadPoolExecutor.class)).thenReturn(executorServiceSpy);

        AccountMetricStatistics result = classUnderTest.call();
        assertEquals(testAccount.getDisplayAccountName(), result.getAccountName());
        assertEquals(regionStats1, result.getRegionMetricStatisticsList().get(0));
        assertEquals(regionStats2, result.getRegionMetricStatisticsList().get(1));

        //Test if shutdown was called once on MonitorThreadPoolExecutor
        verify(executorServiceSpy, Mockito.times(1)).shutdown();
    }
}
