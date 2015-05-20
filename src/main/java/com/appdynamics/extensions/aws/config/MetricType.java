package com.appdynamics.extensions.aws.config;

/**
 * @author Florencio Sarmiento
 *
 */
public class MetricType {
	
	private String metricName;
	
	private String statType;

	public String getMetricName() {
		return metricName;
	}

	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	public String getStatType() {
		return statType;
	}

	public void setStatType(String statType) {
		this.statType = statType;
	}

}
