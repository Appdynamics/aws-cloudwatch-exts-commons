/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.collectors;

import com.appdynamics.extensions.aws.config.*;
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
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AccountMetricStatisticsCollector.class,
        ExecutorCompletionService.class})
@PowerMockIgnore({"org.apache.*, javax.xml.*", "javax.net.ssl.*" })

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
        Set<String> testRegions = Sets.newHashSet("us-west-1", "us-west-2");
        testAccount.setRegions(testRegions);

//        RegionMetricStatisticsCollector mockRegionStatsCollector1 = mock(RegionMetricStatisticsCollector.class);
//        RegionMetricStatistics regionStats1 = createTestMetricStatistics("region1");
//        when(mockRegionStatsCollector1.call()).thenReturn(regionStats1);

//        RegionMetricStatisticsCollector mockRegionStatsCollector2 = mock(RegionMetricStatisticsCollector.class);
//        RegionMetricStatistics regionStats2 = createTestMetricStatistics("region2");
//        when(mockRegionStatsCollector2.call()).thenReturn(regionStats2);



        classUnderTest = new AccountMetricStatisticsCollector.Builder()
                .withAccount(testAccount)
                .withMetricsProcessor(mockMetricsProcessor)
                .withMaxErrorRetrySize(1)
                .withMetricsTimeRange(new MetricsTimeRange())
                .withNoOfMetricThreadsPerRegion(1)
                .withNoOfRegionThreadsPerAccount(2)
                .withProxyConfig(null)
                .withPeriod(60)
                .withThreadTimeOut(10)
                .withRateLimiter(RateLimiter.create(400))
                .withAWSRequestCounter(requestCounter)
                .withPrefix("CustomMetrics| AWS").withTags(new ArrayList<Tags>())
                .build();

//
//
//        whenNew(RegionMetricStatisticsCollector.Builder.class).withNoArguments().thenReturn(mockBuilder);
//        when(mockBuilder.withAccountName(anyString())).thenReturn(mockBuilder);
//        when(mockBuilder.withMetricsProcessor(any(MetricsProcessor.class))).thenReturn(mockBuilder);
//        when(mockBuilder.withMetricsTimeRange(any(MetricsTimeRange.class))).thenReturn(mockBuilder);
//        when(mockBuilder.withAmazonCloudWatchConfig())
//        when(mockBuilder.withNoOfMetricThreadsPerRegion(anyInt())).thenReturn(mockBuilder);
//        when(mockBuilder.withPeriodInSeconds(anyInt())).thenReturn(mockBuilder);
//        when(mockBuilder.withThreadTimeOut(anyInt())).thenReturn(mockBuilder);
//        when(mockBuilder.withRegion(anyString())).thenReturn(mockBuilder);
//        when(mockBuilder.withRateLimiter(any(RateLimiter.class))).thenReturn(mockBuilder);
//        when(mockBuilder.withAWSRequestCounter(any(LongAdder.class))).thenReturn(mockBuilder);
//        when(mockBuilder.withPrefix(anyString())).thenReturn(mockBuilder);
//        when(mockBuilder.build()).thenReturn(mock(RegionMetricStatisticsCollector.class));
//        when(AWSUtil.createAWSCredentials(testAccount,null)).thenReturn(mock(AWSCredentials.class));
//


        AccountMetricStatistics result = classUnderTest.call();

        assertEquals(testAccount.getDisplayAccountName(), result.getAccountName());
//        assertEquals(regionStats1, result.getRegionMetricStatisticsList().get(0));
//        assertEquals(regionStats2, result.getRegionMetricStatisticsList().get(1));
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
}
