package com.appdynamics.extensions.aws.metric;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author Florencio Sarmiento
 *
 */
public class RegionMetricStatistics {

	private String region;

	private List<MetricStatistic> metricStatisticsList = new CopyOnWriteArrayList<MetricStatistic>();

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public List<MetricStatistic> getMetricStatisticsList() {
		return metricStatisticsList;
	}

	public void addMetricStatistic(MetricStatistic metricStatistics) {
		this.metricStatisticsList.add(metricStatistics);
	}
	
	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
