package com.appdynamics.extensions.aws.metric.processors;

import java.util.List;
import java.util.Map;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.appdynamics.extensions.aws.metric.NamespaceMetricStatistics;
import com.appdynamics.extensions.aws.metric.StatisticType;

/**
 * Interface to be implemented by Namespace specific MetricsProcessor
 * 
 * @author Florencio Sarmiento
 *
 */
public interface MetricsProcessor {
	
	/**
	 * Any metrics (names + dimensions) returned in this list 
	 * will be used for retrieving statistics, therefore
	 * any necessary filtering must be done within this method
	 * 
	 * @param awsCloudWatch AmazonCloudWatch
	 * @return list of metrics
	 */
	List<Metric> getMetrics(AmazonCloudWatch awsCloudWatch);

	/**
	 * Returns the statistic type of the specified metric
	 * 
	 * @param metric - Metric
	 * @return statisticType
	 */
	StatisticType getStatisticType(Metric metric);
	
	/**
	 * Converts the nested statistics within NamespaceMetricStatistics 
	 * into a Map object for uploading to Controller.
	 * 
	 * Map key is the metric name without the metric prefix, e.g.
	 * <b>MyTestAccount|us-east-1|mycachecluster|0001|CPUUtilization</b>
	 * 
	 * @param namespaceMetricStats NamespaceMetricStatistics
	 * @return Map of statistics
	 */
	Map<String, Double> createMetricStatsMapForUpload(NamespaceMetricStatistics namespaceMetricStats);
	
	/**
	 * @return the Namespace
	 */
	String getNamespace();
}
