package com.appdynamics.extensions.aws.metric.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import org.junit.Test;

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.appdynamics.extensions.aws.config.MetricType;
import com.appdynamics.extensions.aws.metric.AccountMetricStatistics;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.NamespaceMetricStatistics;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.aws.metric.StatisticType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class MetricsProcessorHelperTest {
	
	@Test
	public void testGetStatisticTypeWithRegex() {
		MetricType metricType = new MetricType();
		metricType.setMetricName("metricRequireSum\\d+");
		metricType.setStatType("sum");
		
		MetricType metricType2 = new MetricType();
		metricType2.setMetricName("metricRequireMax");
		metricType2.setStatType("max");
		
		List<MetricType> metricTypes = Lists.newArrayList(metricType, metricType2);
		
		Metric metric = new Metric();
		metric.setMetricName("metricRequireSum1");
		
		StatisticType result = MetricsProcessorHelper.getStatisticType(metric, metricTypes);
		assertEquals(StatisticType.SUM, result);
		
		metric = new Metric();
		metric.setMetricName("metricRequireMax");
		
		result = MetricsProcessorHelper.getStatisticType(metric, metricTypes);
		assertEquals(StatisticType.MAX, result);
	}
	
	@Test
	public void testGetStatisticDefaultsToAverage() {
		Metric metric = new Metric();
		metric.setMetricName("testMetric1");
		
		StatisticType result = MetricsProcessorHelper.getStatisticType(metric, new ArrayList<MetricType>());
		assertEquals(StatisticType.AVE, result);
	}
	
	@Test
	public void testFilterMetricsWithRegex() {
		String excludes = "metricToRemove|.*unwanted.*";
		Pattern excludesPattern = Pattern.compile(excludes, Pattern.CASE_INSENSITIVE);
		
		Metric metric1 = new Metric();
		metric1.setMetricName("metricToKeep");
		
		Metric metric2 = new Metric();
		metric2.setMetricName("metricToRemove");
		
		Metric metric3 = new Metric();
		metric3.setMetricName("metricUnwanted");
		
		List<Metric> testMetrics = Lists.newArrayList(metric1, metric2, metric3);
		
		List<Metric> result = MetricsProcessorHelper.filterMetrics(testMetrics, excludesPattern);
		
		assertEquals(1, result.size());
		assertEquals(metric1, result.get(0));
	}
	
	@Test
	public void testCreateMetricStatsMapForUploadWithNamespaceAsPrefix() {
		NamespaceMetricStatistics testNamespaceStats = createTestNamespaceMetricStatistics("testNamespace");
		Map<String, String> testDimensionDictionary = Maps.newHashMap();
		testDimensionDictionary.put("testDimesionName", "My Test Dimension");
		
		Map<String, Double> result = MetricsProcessorHelper.createMetricStatsMapForUpload(
				testNamespaceStats, testDimensionDictionary, true);
		
		for (int accountIndex=0; accountIndex<2; accountIndex++) {
			for (int regionIndex=0; regionIndex<2; regionIndex++) {
				for (int metricIndex=0; metricIndex <2; metricIndex++) {
					String expectedMetricName = String.format(
							"testNamespace|account%s|region%s|My Test Dimension|testDimesionValue|testMetric%s (testUnit)", 
							accountIndex, regionIndex, metricIndex);
					
					assertNotNull(result.get(expectedMetricName));
				}
			}
		}
	}
	
	@Test
	public void testCreateMetricStatsMapForUploadNotUsingNamespaceAsPrefixAndNullDimensionDictionary() {
		NamespaceMetricStatistics testNamespaceStats = createTestNamespaceMetricStatistics("testNamespace");
		Map<String, Double> result = MetricsProcessorHelper.createMetricStatsMapForUpload(testNamespaceStats, null, false);
		
		for (int accountIndex=0; accountIndex<2; accountIndex++) {
			for (int regionIndex=0; regionIndex<2; regionIndex++) {
				for (int metricIndex=0; metricIndex <2; metricIndex++) {
					String expectedMetricName = String.format(
							"account%s|region%s|testDimesionName|testDimesionValue|testMetric%s (testUnit)", 
							accountIndex, regionIndex, metricIndex);
					
					assertNotNull(result.get(expectedMetricName));
				}
			}
		}
	}
	
	private NamespaceMetricStatistics createTestNamespaceMetricStatistics(String namespace) {
		NamespaceMetricStatistics namespaceStats = new NamespaceMetricStatistics();
		namespaceStats.setNamespace(namespace);
		
		for (int accountIndex=0; accountIndex<2; accountIndex++) {
			AccountMetricStatistics accountStats = new AccountMetricStatistics();
			accountStats.setAccountName("account" + accountIndex);
			
			for (int regionIndex=0; regionIndex<2; regionIndex++) {
				RegionMetricStatistics regionStats = new RegionMetricStatistics();
				regionStats.setRegion("region" + regionIndex);
				
				for (int metricIndex=0; metricIndex <2; metricIndex++) {
					Metric metric = new Metric();
					metric.setMetricName("testMetric" + metricIndex);
					
					Dimension dimension = new Dimension();
					dimension.setName("testDimesionName");
					dimension.setValue("testDimesionValue");
					
					metric.setDimensions(Lists.newArrayList(dimension));
					
					MetricStatistic metricStatistic = new MetricStatistic();
					metricStatistic.setMetric(metric);
					metricStatistic.setValue(new Random().nextDouble());
					metricStatistic.setUnit("testUnit");
					
					regionStats.addMetricStatistic(metricStatistic);
				}
				
				accountStats.add(regionStats);
			}
			
			namespaceStats.add(accountStats);
		}

		return namespaceStats;
	}

}
