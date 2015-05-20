package com.appdynamics.extensions.aws.config;

import java.util.List;
import java.util.Set;

/**
 * @author Florencio Sarmiento
 *
 */
public class MetricsConfig {

	private List<MetricType> metricTypes;

	private Set<String> excludeMetrics;
	
	private MetricsTimeRange metricsTimeRange;
	
	private int maxErrorRetrySize;

	public List<MetricType> getMetricTypes() {
		return metricTypes;
	}

	public void setMetricTypes(List<MetricType> metricTypes) {
		this.metricTypes = metricTypes;
	}

	public Set<String> getExcludeMetrics() {
		return excludeMetrics;
	}

	public void setExcludeMetrics(Set<String> excludeMetrics) {
		this.excludeMetrics = excludeMetrics;
	}

	public MetricsTimeRange getMetricsTimeRange() {
		return metricsTimeRange;
	}

	public void setMetricsTimeRange(MetricsTimeRange metricsTimeRange) {
		this.metricsTimeRange = metricsTimeRange;
	}

	public int getMaxErrorRetrySize() {
		return maxErrorRetrySize;
	}

	public void setMaxErrorRetrySize(int maxErrorRetrySize) {
		this.maxErrorRetrySize = maxErrorRetrySize;
	}

}
