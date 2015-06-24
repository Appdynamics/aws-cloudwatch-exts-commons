package com.appdynamics.extensions.aws;

import static com.appdynamics.extensions.aws.Constants.*;
import static com.appdynamics.extensions.aws.util.AWSUtil.convertToLong;
import static com.appdynamics.extensions.aws.util.AWSUtil.resolvePath;
import static com.appdynamics.extensions.yml.YmlReader.readFromFile;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.appdynamics.extensions.aws.providers.RegionEndpointProvider;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

/**
 * @author Florencio Sarmiento
 *
 * @param <T> Configuration class
 */
public abstract class AWSCloudwatchMonitor<T> extends AManagedMonitor {

	private Class<T> clazz;

	public AWSCloudwatchMonitor(Class<T> clazz) {
		this.clazz = clazz;
	}

	public TaskOutput execute(Map<String, String> args,
			TaskExecutionContext paramTaskExecutionContext)
			throws TaskExecutionException {
		getLogger().info("Starting AWS Cloudwatch Monitoring task");

		if (getLogger().isDebugEnabled()) {
			getLogger().debug(String.format("Args received were: %s", args));
		}

		if (args != null) {
			try {
				String configFilename = resolvePath(args.get(CONFIG_ARG));
				T config = readFromFile(configFilename, clazz);
				
				initialiseServiceProviders(config, args);
				
				Map<String, Double> statsForUpload = getStatsForUpload(config);
				uploadStats(statsForUpload, config);
				
				return new TaskOutput("AWS Cloudwatch Monitoring task successfully completed");

			} catch (Exception ex) {
				getLogger().error("Unfortunately an issue has occurred: ", ex);
			}
		}

		throw new TaskExecutionException(
				"AWS Cloudwatch Monitoring task completed with failures.");
	}
	
	protected void initialiseServiceProviders(T config, Map<String, String> paramArgs) {
		RegionEndpointProvider regionEndpointProvider = 
				RegionEndpointProvider.getInstance();
		regionEndpointProvider.initialise(paramArgs.get(CONFIG_REGION_ENDPOINTS_ARG));		
	}

	private void uploadStats(Map<String, Double> statsForUpload, T config) {
		String metricPrefix = doGetMetricPrefix(config);
		
		for (Map.Entry<String, Double> stat : statsForUpload.entrySet()) {
			printCollectiveObservedCurrent(metricPrefix + stat.getKey(), stat.getValue());
		}
	}

	private void printCollectiveObservedCurrent(String metricName,
			Double metricValue) {
		printMetric(metricName, metricValue,
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
				MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
	}

	private void printMetric(String metricName, Double metricValue,
			String aggregation, String timeRollup, String cluster) {
		MetricWriter metricWriter = getMetricWriter(metricName, aggregation,
				timeRollup, cluster);

		Long valueToReport = convertToLong(metricValue);

		if (getLogger().isDebugEnabled()) {
			getLogger().debug(String
					.format("Sending [%s/%s/%s] metric = %s = %s => %s",
							aggregation, timeRollup, cluster, metricName,
							metricValue, valueToReport));
		}

		metricWriter.printMetric(valueToReport.toString());
	}
	
	private String doGetMetricPrefix(T config) {
		String metricPrefix = getMetricPrefix(config);
		
		if (StringUtils.isNotBlank(metricPrefix) && 
				!metricPrefix.endsWith(METRIC_PATH_SEPARATOR)) {
			metricPrefix = metricPrefix + METRIC_PATH_SEPARATOR;
		}
		
		return metricPrefix;
	}
	
	protected abstract Map<String, Double> getStatsForUpload(T config);
	
	protected abstract String getMetricPrefix(T config);

	protected abstract Logger getLogger();

}
