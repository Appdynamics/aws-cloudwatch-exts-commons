/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.collectors;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.appdynamics.extensions.aws.config.Account;
import com.appdynamics.extensions.aws.config.IncludeMetric;
import com.appdynamics.extensions.aws.config.MetricsTimeRange;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.exceptions.AwsException;
import com.appdynamics.extensions.aws.metric.AccountMetricStatistics;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.powermock.api.mockito.PowerMockito.*;

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
        when(mockBuilder.withAmazonCloudWatchConfig(any(AWSCredentials.class), any(ClientConfiguration.class),anyString()))
                .thenReturn(mockBuilder);
        when(mockBuilder.withTags(anyList())).thenReturn(mockBuilder);
        when(mockBuilder.withPeriodInSeconds(anyInt())).thenReturn(mockBuilder);
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
                .withPeriod(60)
                .withThreadTimeOut(10)
                .withTags(Lists.newArrayList())
                .withRateLimiter(RateLimiter.create(400))
                .withAWSRequestCounter(requestCounter)
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

    private  List<Metric> getMetrics() {

        List<Metric> metric = Lists.newArrayList();
        for(int i = 0 ; i < 1; i++){
            List<Dimension> dimension  = Lists.newArrayList();
            dimension.add(new Dimension().withName("testDimesionName"+i).withValue("testDimensionValue"+i));
            metric.add(new Metric().withMetricName("testMetric" +i).withNamespace("testNamespace").withDimensions(dimension));

        }
        return metric;
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
}
