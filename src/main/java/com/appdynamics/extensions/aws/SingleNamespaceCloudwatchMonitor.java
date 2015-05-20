package com.appdynamics.extensions.aws;

import java.util.Map;

import com.appdynamics.extensions.aws.collectors.NamespaceMetricStatisticsCollector;

/**
 * @author Florencio Sarmiento
 *
 * @param <T> Configuration class
 */
public abstract class SingleNamespaceCloudwatchMonitor<T> extends AWSCloudwatchMonitor<T> {

	public SingleNamespaceCloudwatchMonitor(Class<T> clazz) {
		super(clazz);
	}

	/*
	 * Running NamespaceMetricsCollector on the same thread 
	 * since we're only dealing with a single namespace
	 */
	@Override
	protected Map<String, Double> getStatsForUpload(T config) {
		return getNamespaceMetricsCollector(config).call();
	}

	protected abstract NamespaceMetricStatisticsCollector getNamespaceMetricsCollector(T config);

}
