/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.collectors;

import com.appdynamics.extensions.aws.config.*;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.metric.AccountMetricStatistics;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.NamespaceMetricStatistics;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NamespaceMetricStatisticsCollector.class,
        ExecutorCompletionService.class})
@PowerMockIgnore({"org.apache.*, javax.xml.*","javax.net.ssl.*"})

public class NamespaceMetricStatisticsCollectorTest {

    private NamespaceMetricStatisticsCollector classUnderTest;

    @Mock
    private MetricsProcessor mockMetricsProcessor;

    @Mock
    private MetricsConfig mockMetricsConfig;

    @SuppressWarnings("unchecked")
    @Test
    public void testMetricsRetrievalIsSuccessful() throws Exception {
        List<Account> testAccounts = getTestAccounts();

        when(mockMetricsProcessor.getNamespace()).thenReturn("TestNamespace");

        AccountMetricStatisticsCollector mockAccountStatsCollector1 = mock(AccountMetricStatisticsCollector.class);
        AccountMetricStatistics accountStats1 = createTestAccountMetricStatistics(testAccounts.get(0).getDisplayAccountName());
        when(mockAccountStatsCollector1.call()).thenReturn(accountStats1);

        // simulate account stats collector creation
        AccountMetricStatisticsCollector.Builder mockBuilder = mock(AccountMetricStatisticsCollector.Builder.class);

        whenNew(AccountMetricStatisticsCollector.Builder.class).withNoArguments().thenReturn(mockBuilder);

        when(mockBuilder.withAccount(any(Account.class))).thenReturn(mockBuilder);
        when(mockBuilder.withMaxErrorRetrySize(anyInt())).thenReturn(mockBuilder);
        when(mockBuilder.withMetricsProcessor(any(MetricsProcessor.class))).thenReturn(mockBuilder);
        when(mockBuilder.withMetricsTimeRange(any(MetricsTimeRange.class))).thenReturn(mockBuilder);
        when(mockBuilder.withNoOfMetricThreadsPerRegion(anyInt())).thenReturn(mockBuilder);
        when(mockBuilder.withNoOfRegionThreadsPerAccount(anyInt())).thenReturn(mockBuilder);
        when(mockBuilder.withThreadTimeOut(anyInt())).thenReturn(mockBuilder);
        when(mockBuilder.withTags(anyList())).thenReturn(mockBuilder);
        when(mockBuilder.withPeriod(anyInt())).thenReturn(mockBuilder);
        when(mockBuilder.withThreadTimeOut(anyInt())).thenReturn(mockBuilder);
        when(mockBuilder.withCredentialsDecryptionConfig(any(CredentialsDecryptionConfig.class))).thenReturn(mockBuilder);
        when(mockBuilder.withProxyConfig(any(ProxyConfig.class))).thenReturn(mockBuilder);
        when(mockBuilder.withRateLimiter(any(RateLimiter.class))).thenReturn(mockBuilder);

        when(mockBuilder.withAWSRequestCounter(any(LongAdder.class))).thenReturn(mockBuilder);
        when(mockBuilder.withPrefix(anyString())).thenReturn(mockBuilder);

        when(mockBuilder.build()).thenReturn(mockAccountStatsCollector1);
        when(mockMetricsConfig.getGetMetricStatisticsRateLimit()).thenReturn(400);
        ArgumentCaptor<NamespaceMetricStatistics> argumentCaptor =
                ArgumentCaptor.forClass(NamespaceMetricStatistics.class);

        // since metricsprocessor converts NamespaceMetricStatistics to a map and we don't know the implementation
        // let's just capture it here for assertion
        List<com.appdynamics.extensions.metrics.Metric> mockMap = mock(List.class);
        when(mockMetricsProcessor.createMetricStatsMapForUpload(argumentCaptor.capture())).thenReturn(mockMap);

        ConcurrencyConfig mockConcurrencyConfig = mock(ConcurrencyConfig.class);
        when(mockConcurrencyConfig.getThreadTimeOut()).thenReturn(60);

        classUnderTest = new NamespaceMetricStatisticsCollector.Builder(testAccounts,
                mockConcurrencyConfig,
                mockMetricsConfig,
                mockMetricsProcessor, "Test|Prefix").withTags(Lists.newArrayList())
                .build();

        classUnderTest.call();

        NamespaceMetricStatistics result = argumentCaptor.getValue();

        verify(mockMetricsProcessor).createMetricStatsMapForUpload(isA(NamespaceMetricStatistics.class));
        assertEquals(accountStats1, result.getAccountMetricStatisticsList().get(0));
    }

    private List<Account> getTestAccounts() {
        List<Account> testAccounts = Lists.newArrayList();

        for (int index = 0; index < 2; index++) {
            Account testAccount = new Account();
            testAccount.setAwsAccessKey("testAccessKey" + index);
            testAccount.setAwsSecretKey("testAwsSecretKey" + index);
            testAccount.setDisplayAccountName("TestAccount" + index);

            Set<String> regions = new HashSet<>();
            regions.add("region1");
            testAccount.setRegions(regions);

            testAccounts.add(testAccount);
        }

        return testAccounts;
    }

    private AccountMetricStatistics createTestAccountMetricStatistics(String account) {
        AccountMetricStatistics accountStats = new AccountMetricStatistics();
        accountStats.setAccountName(account);

        Random random = new Random();

        for (int regionIndex = 0; regionIndex < random.nextInt(3); regionIndex++) {
            RegionMetricStatistics regionStats = new RegionMetricStatistics();
            regionStats.setRegion("region" + regionIndex);

            for (int metricIndex = 0; metricIndex < random.nextInt(10); metricIndex++) {
                IncludeMetric metric = new IncludeMetric();
                metric.setName("testMetric" + metricIndex);

                AWSMetric awsMetric = new AWSMetric();
                awsMetric.setIncludeMetric(metric);

                MetricStatistic metricStatistic = new MetricStatistic();
                metricStatistic.setMetric(awsMetric);
                metricStatistic.setValue(new Random().nextDouble());
                metricStatistic.setUnit("testUnit");

                regionStats.addMetricStatistic(metricStatistic);
            }
            accountStats.add(regionStats);
        }
        return accountStats;
    }

}
