package com.appdynamics.extensions.aws.collectors;

import static com.appdynamics.extensions.aws.Constants.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.appdynamics.extensions.aws.config.MetricsTimeRange;
import com.appdynamics.extensions.aws.exceptions.AwsException;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.StatisticType;

/**
 * Retrieves statistics for the specified metric.
 * 
 * <p>Cloudwatch Limitation: 
 * The maximum number of data points that can be queried is 50,850, 
 * whereas the maximum number of data points returned from a single 
 * GetMetricStatistics request is 1,440.
 * 
 * <p>see {@link http://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricStatistics.html} 
 * 
 * @author Florencio Sarmiento
 *
 */
public class MetricStatisticCollector implements Callable<MetricStatistic> {
	
	private static Logger LOGGER = Logger.getLogger("com.singularity.extensions.aws.MetricStatisticCollector");
	
	private String accountName;
	
	private String region;
	
	private AmazonCloudWatch awsCloudWatch;
	
	private Metric metric;

	private StatisticType statType;
	
	private int startTimeInMinsBeforeNow;
	
	private int endTimeInMinsBeforeNow;
	
	private MetricStatisticCollector(String accountName,
			String region,
			AmazonCloudWatch awsCloudWatch, 
			Metric metric,
			StatisticType statType, 
			MetricsTimeRange metricsTimeRange) {
		
		this.accountName = accountName;
		this.region = region;
		this.awsCloudWatch = awsCloudWatch;
		this.metric = metric;
		this.statType = statType;
		
		setStartTimeInMinsBeforeNow(metricsTimeRange.getStartTimeInMinsBeforeNow());
		setEndTimeInMinsBeforeNow(metricsTimeRange.getEndTimeInMinsBeforeNow());
	}

	/**
	 * Uses {@link AmazonCloudWatch} to retrieve metric datapoints.
	 * 
	 * Returns statistic based from the latest datapoint 
	 * and the statistic type specified. 
	 */
	public MetricStatistic call() throws Exception {
		MetricStatistic metricStatistic = null;
		
		try{
			validateStartAndEndTime();
			
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(String.format("Collecting MetricStatistic for Namespace [%s] "
						+ "Account [%s] Region [%s] Metric [%s] Dimensions [%s]",
						metric.getNamespace(), accountName, region, 
						metric.getMetricName(), metric.getDimensions()));
			}
			
			metricStatistic = new MetricStatistic();
			metricStatistic.setMetric(metric);
			
            GetMetricStatisticsRequest request = createGetMetricStatisticsRequest();
            GetMetricStatisticsResult result = awsCloudWatch.getMetricStatistics(request);
            
            Datapoint latestDatapoint = getLatestDatapoint(result.getDatapoints());
            
            if (latestDatapoint != null) {
            	Double value = getValue(latestDatapoint);
            	metricStatistic.setValue(value);
            	metricStatistic.setUnit(latestDatapoint.getUnit());
            }
            
        } catch (Exception e){
        	throw new AwsException(String.format(
        			"Error getting MetricStatistic for Namespace [%s] "
        			+ "Account [%s] Region [%s] Metric [%s] Dimensions [%s]", 
        			metric.getNamespace(), accountName, region, 
					metric.getMetricName(), metric.getDimensions()), e);
        }
		
		return metricStatistic;
	}
	
	private GetMetricStatisticsRequest createGetMetricStatisticsRequest() {
		GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
				.withStartTime(DateTime.now(DateTimeZone.UTC)
						.minusMinutes(startTimeInMinsBeforeNow).toDate())
				.withNamespace(metric.getNamespace())
				.withDimensions(metric.getDimensions())
				.withPeriod(DEFAULT_METRIC_PERIOD_IN_SEC)
				.withMetricName(metric.getMetricName())
				.withStatistics(statType.getTypeName())
				.withEndTime(DateTime.now(DateTimeZone.UTC)
						.minusMinutes(endTimeInMinsBeforeNow).toDate());
		
		return getMetricStatisticsRequest;
	}
	
    private Datapoint getLatestDatapoint(List<Datapoint> datapoints) {
    	Datapoint datapoint = null;
    	
    	if (datapoints != null && !datapoints.isEmpty()) {
    		if (datapoints.size() > 1) {
    			Collections.sort(datapoints, new DatapointComparator());
    		}
    		
    		datapoint = datapoints.get(0);
    		
    	} else if (LOGGER.isDebugEnabled()){
    		LOGGER.debug(String.format("No statistics retrieved for Namespace [%s] "
    				+ "Account [%s] Region [%s] Metric [%s] Dimensions [%s]",
    				metric.getNamespace(), accountName, region, 
					metric.getMetricName(), metric.getDimensions()));
    	}
    	
    	return datapoint;
    }
    
    /**
     * Descending order comparator for Datapoint
     * Null value is always considered last
     */
    private class DatapointComparator implements Comparator<Datapoint> {

		public int compare(Datapoint datapoint1, Datapoint datapoint2) {
			
			if (getTimestamp(datapoint1) == null && getTimestamp(datapoint2) == null) {
				return 0;
				
			} else if (getTimestamp(datapoint1) == null && getTimestamp(datapoint2) != null) {
				return 1;
				
			} else if (getTimestamp(datapoint1) != null && getTimestamp(datapoint2) == null) {
				return -1;
				
			} else {
				return -1 * getTimestamp(datapoint1).compareTo(getTimestamp(datapoint2));
			}
			
		}
		
		private Date getTimestamp(Datapoint datapoint) {
			return datapoint != null ? datapoint.getTimestamp() : null;
		}
    }
    
    private Double getValue(Datapoint datapoint) {
    	Double value = null;
    	
    	if (datapoint != null) {
			switch (statType) {
			case AVE:
				value = datapoint.getAverage();
				break;
			case MAX:
				value = datapoint.getMaximum();
				break;
			case MIN:
				value = datapoint.getMinimum();
				break;
			case SUM:
				value = datapoint.getSum();
				break;
			case SAMPLE_COUNT:
				value = datapoint.getSampleCount();
				break;
			}
    	}
    	
    	return value;
    }
    
    private void setStartTimeInMinsBeforeNow(int startTimeInMinsBeforeNow) {
    	this.startTimeInMinsBeforeNow = startTimeInMinsBeforeNow < 0 ? 
    			DEFAULT_START_TIME_IN_MINS_BEFORE_NOW : startTimeInMinsBeforeNow;
    }
    
    private void setEndTimeInMinsBeforeNow(int endTimeInMinsBeforeNow) {
    	this.endTimeInMinsBeforeNow = endTimeInMinsBeforeNow < 0 ? 
    			DEFAULT_END_TIME_IN_MINS_BEFORE_NOW : endTimeInMinsBeforeNow;
    }
    
    private void validateStartAndEndTime() {
    	if (endTimeInMinsBeforeNow > startTimeInMinsBeforeNow) {
    		throw new IllegalArgumentException(String.format(
    				"endTimeInMinsBeforeNow [%s] must not be greater than startTimeInMinsBeforeNow [%s]",
    				endTimeInMinsBeforeNow, startTimeInMinsBeforeNow));
    	}
    }
    
    /**
     * Builder class to maintain readability when 
     * building {@link MetricStatisticCollector} due to its params size
     */
    public static class Builder {
    	
    	private String accountName;
    	
    	private String region;
    	
    	private AmazonCloudWatch awsCloudWatch;
    	
    	private Metric metric;

    	private StatisticType statType;
    	
    	private MetricsTimeRange metricsTimeRange;
    	
    	public Builder withAccountName(String accountName) {
    		this.accountName = accountName;
    		return this;
    	}
    	
    	public Builder withRegion(String region) {
    		this.region = region;
    		return this;
    	}
    	
    	public Builder withAwsCloudWatch(AmazonCloudWatch awsCloudWatch) {
    		this.awsCloudWatch = awsCloudWatch;
    		return this;
    	}
    	
    	public Builder withMetric(Metric metric) {
    		this.metric = metric;
    		return this;
    	}
    	
    	public Builder withStatType(StatisticType statType) {
    		this.statType = statType;
    		return this;
    	}
    	
    	public Builder withMetricsTimeRange(MetricsTimeRange metricsTimeRange) {
    		this.metricsTimeRange = metricsTimeRange;
    		return this;
    	}
    	
    	public MetricStatisticCollector build() {
    		return new MetricStatisticCollector(
    				accountName,
    				region,
    				awsCloudWatch, 
    				metric, 
    				statType, 
    				metricsTimeRange);
    	}
    }
}
