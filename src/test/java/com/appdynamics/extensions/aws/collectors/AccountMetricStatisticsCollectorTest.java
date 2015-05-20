package com.appdynamics.extensions.aws.collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.appdynamics.extensions.aws.config.Account;
import com.appdynamics.extensions.aws.config.MetricsTimeRange;
import com.appdynamics.extensions.aws.exceptions.AwsException;
import com.appdynamics.extensions.aws.metric.AccountMetricStatistics;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;
import com.google.common.collect.Sets;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AccountMetricStatisticsCollector.class,
	ExecutorCompletionService.class})
@PowerMockIgnore({"org.apache.*, javax.xml.*"})
public class AccountMetricStatisticsCollectorTest {
	
	private AccountMetricStatisticsCollector classUnderTest;
	
	@Mock
	private MetricsProcessor mockMetricsProcessor;
	
	@Test(expected=AwsException.class)
	public void testIncompleteAccountThrowsException() throws Exception {
		classUnderTest = new AccountMetricStatisticsCollector.Builder()
							.withAccount(new Account())
							.withMetricsProcessor(mockMetricsProcessor)
							.build();
		
		classUnderTest.call();
	}

	@Test()
	public void testMetricsRetrievalIsSuccessful() throws Exception {
		Account testAccount = new Account();
		testAccount.setAwsAccessKey("testAccessKey");
		testAccount.setAwsSecretKey("testAwsSecretKey");
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
		when(mockBuilder.withRegion(anyString())).thenReturn(mockBuilder);
		when(mockBuilder.build()).thenReturn(mockRegionStatsCollector1, mockRegionStatsCollector2);
		
		classUnderTest = new AccountMetricStatisticsCollector.Builder()
							.withAccount(testAccount)
							.withMetricsProcessor(mockMetricsProcessor)
							.withMaxErrorRetrySize(1)
							.withMetricsTimeRange(new MetricsTimeRange())
							.withNoOfMetricThreadsPerRegion(1)
							.withNoOfRegionThreadsPerAccount(2)
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
		
		for (int index=0; index < random.nextInt(10); index++) {
			Metric metric = new Metric();
			metric.setMetricName("testMetric" + index);
			
			MetricStatistic metricStatistic = new MetricStatistic();
			metricStatistic.setMetric(metric);
			metricStatistic.setValue(new Random().nextDouble());
			metricStatistic.setUnit("testUnit");
			
			regionStats.addMetricStatistic(metricStatistic);
		}

		return regionStats;
	}
}
