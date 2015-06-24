package com.appdynamics.extensions.aws.collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorCompletionService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.appdynamics.extensions.aws.config.MetricsTimeRange;
import com.appdynamics.extensions.aws.exceptions.AwsException;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.aws.metric.StatisticType;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;
import com.appdynamics.extensions.aws.providers.RegionEndpointProvider;
import com.google.common.collect.Lists;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RegionEndpointProvider.class, 
	RegionMetricStatisticsCollector.class,
	ExecutorCompletionService.class})
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
	
	@Mock
	private RegionEndpointProvider mockRegionEndpointProvider;
	
	@Before
	public void setUp() throws Exception {
		mockStatic(RegionEndpointProvider.class);
		when(RegionEndpointProvider.getInstance()).thenReturn(mockRegionEndpointProvider);
		whenNew(AmazonCloudWatchClient.class).withArguments(mockAWSCredentials, mockAwsClientConfig)
			.thenReturn(mockAwsCloudWatch);
	}
	
	@Test(expected=AwsException.class)
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
		when(mockRegionEndpointProvider.getEndpoint(anyString())).thenReturn("test-endpoint");
		when(mockMetricsProcessor.getMetrics(mockAwsCloudWatch)).thenReturn(new ArrayList<Metric>());
		
		String testRegion = "testRegion";
		
		classUnderTest = new RegionMetricStatisticsCollector.Builder()
			.withAmazonCloudWatchConfig(mockAWSCredentials, mockAwsClientConfig)
			.withMetricsProcessor(mockMetricsProcessor)
			.withRegion(testRegion)
			.build();
		
		RegionMetricStatistics result = classUnderTest.call();
		
		assertEquals(testRegion, result.getRegion());
		assertTrue(result.getMetricStatisticsList().isEmpty());
	}
	
	@Test
	public void testMetricsRetrievalIsSuccessful() throws Exception {
		when(mockRegionEndpointProvider.getEndpoint(anyString())).thenReturn("test-endpoint");
		
		List<Metric> testMetrics = getTestMetrics();
		when(mockMetricsProcessor.getMetrics(mockAwsCloudWatch)).thenReturn(testMetrics);
		
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
		when(mockBuilder.withMetric(any(Metric.class))).thenReturn(mockBuilder);
		when(mockBuilder.withMetricsTimeRange(any(MetricsTimeRange.class))).thenReturn(mockBuilder);
		when(mockBuilder.withRegion(anyString())).thenReturn(mockBuilder);
		when(mockBuilder.withStatType(any(StatisticType.class))).thenReturn(mockBuilder);
		when(mockBuilder.build()).thenReturn(mockMetricStatsCollector1, mockMetricStatsCollector2);
		
		String testRegion = "testRegion";
		
		classUnderTest = new RegionMetricStatisticsCollector.Builder()
			.withAmazonCloudWatchConfig(mockAWSCredentials, mockAwsClientConfig)
			.withMetricsProcessor(mockMetricsProcessor)
			.withMetricsTimeRange(new MetricsTimeRange())
			.withRegion(testRegion)
			.build();
		
		RegionMetricStatistics result = classUnderTest.call();
		
		assertEquals(testRegion, result.getRegion());
		assertEquals(testMetrics.size(), result.getMetricStatisticsList().size());
		assertEquals(metricStatistic1, result.getMetricStatisticsList().get(0));
		assertEquals(metricStatistic2, result.getMetricStatisticsList().get(1));
	}
	
	private List<Metric> getTestMetrics() {
		List<Metric> testMetrics = Lists.newArrayList();
		
		for (int index=0; index<2; index++) {
			Metric metric = new Metric();
			metric.setMetricName("testMetric" + index);
			metric.setNamespace("testNamespace");
			testMetrics.add(metric);
		}
		
		return testMetrics;
	}
	
	private MetricStatistic createTestMetricStatistics(Metric metric) {
		MetricStatistic metricStatistic = new MetricStatistic();
		metricStatistic.setMetric(metric);
		metricStatistic.setValue(new Random().nextDouble());
		metricStatistic.setUnit("testUnit");
		return metricStatistic;
	}
}
