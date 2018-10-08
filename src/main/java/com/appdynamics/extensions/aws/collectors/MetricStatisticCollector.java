/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.collectors;

import static com.appdynamics.extensions.aws.Constants.DEFAULT_END_TIME_IN_MINS_BEFORE_NOW;
import static com.appdynamics.extensions.aws.Constants.DEFAULT_METRIC_PERIOD_IN_SEC;
import static com.appdynamics.extensions.aws.Constants.DEFAULT_START_TIME_IN_MINS_BEFORE_NOW;
import static com.appdynamics.extensions.aws.validators.Validator.validateTimeRange;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.appdynamics.extensions.aws.config.MetricsTimeRange;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.exceptions.AwsException;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.StatisticType;
import com.appdynamics.extensions.aws.config.Period;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.LongAdder;

/**
 * Retrieves statistics for the specified metric.
 * <p>
 * <p>Cloudwatch Limitation:
 * The maximum number of data points that can be queried is 50,850,
 * whereas the maximum number of data points returned from a single
 * GetMetricStatistics request is 1,440.
 * <p>
 * <p>see {@link http://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricStatistics.html}
 *
 * @author Florencio Sarmiento
 */
public class MetricStatisticCollector implements Callable<MetricStatistic> {

    private static Logger LOGGER = Logger.getLogger(MetricStatisticCollector.class);

    private String accountName;

    private String region;

    private AmazonCloudWatch awsCloudWatch;

    private AWSMetric metric;

    private StatisticType statType;

    private int startTimeInMinsBeforeNow;

    private int endTimeInMinsBeforeNow;

    private LongAdder awsRequestsCounter;

    private String metricPrefix;

    private int periodInSeconds;

    private MetricStatisticCollector(Builder builder) {

        this.accountName = builder.accountName;
        this.region = builder.region;
        this.awsCloudWatch = builder.awsCloudWatch;
        this.metric = builder.metric;
        this.statType = builder.statType;
        this.awsRequestsCounter = builder.awsRequestsCounter;
        this.metricPrefix = builder.metricPrefix;

        //Check if time ranges are specified locally for a metric. If not use the global time ranges.
        MetricsTimeRange metricsTimeRangeLocal = metric.getIncludeMetric().getMetricsTimeRange();

        int startTimeInMinsBeforeNow = builder.metricsTimeRange.getStartTimeInMinsBeforeNow();
        if (metricsTimeRangeLocal != null) {
            startTimeInMinsBeforeNow = metricsTimeRangeLocal.getStartTimeInMinsBeforeNow();
        }

        int endTimeInMinsBeforeNow = builder.metricsTimeRange.getEndTimeInMinsBeforeNow();
        if (metricsTimeRangeLocal != null) {
            endTimeInMinsBeforeNow = metricsTimeRangeLocal.getEndTimeInMinsBeforeNow();
        }

        setStartTimeInMinsBeforeNow(startTimeInMinsBeforeNow);
        setEndTimeInMinsBeforeNow(endTimeInMinsBeforeNow);

        Period periodInSecondsLocal = metric.getIncludeMetric().getPeriodInSeconds();
        int periodInSeconds = builder.periodInSeconds.getPeriodInSeconds();

        if(periodInSecondsLocal != null){
            periodInSeconds = periodInSecondsLocal.getPeriodInSeconds();
        }

        setPeriodInSeconds(periodInSeconds);

    }

    /**
     * Uses {@link AmazonCloudWatch} to retrieve metric datapoints.
     * <p>
     * Returns statistic based from the latest datapoint
     * and the statistic type specified.
     */
    public MetricStatistic call() throws Exception {
        MetricStatistic metricStatistic = null;

        try {
            validateTimeRange(startTimeInMinsBeforeNow, endTimeInMinsBeforeNow);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("Collecting MetricStatistic for Namespace [%s] "
                                + "Account [%s] Region [%s] Metric [%s] Dimensions [%s]",
                        metric.getMetric().getNamespace(), accountName, region,
                        metric.getIncludeMetric().getName(), metric.getMetric().getDimensions()));
            }

            metricStatistic = new MetricStatistic();
            metricStatistic.setMetric(metric);
            metricStatistic.setMetricPrefix(metricPrefix);

            GetMetricStatisticsRequest request = createGetMetricStatisticsRequest();
            GetMetricStatisticsResult result = awsCloudWatch.getMetricStatistics(request);
            awsRequestsCounter.increment();

            Datapoint latestDatapoint = getLatestDatapoint(result.getDatapoints());

            if (latestDatapoint != null) {
                Double value = getValue(latestDatapoint);
                metricStatistic.setValue(value);
                metricStatistic.setUnit(latestDatapoint.getUnit());
            }

        } catch (Exception e) {
            throw new AwsException(String.format(
                    "Error getting MetricStatistic for Namespace [%s] "
                            + "Account [%s] Region [%s] Metric [%s] Dimensions [%s]",
                    metric.getMetric().getNamespace(), accountName, region,
                    metric.getIncludeMetric().getName(), metric.getMetric().getDimensions()), e);
        }

        return metricStatistic;
    }

    private GetMetricStatisticsRequest createGetMetricStatisticsRequest() {
        GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                .withStartTime(DateTime.now(DateTimeZone.UTC)
                        .minusMinutes(startTimeInMinsBeforeNow).toDate())
                .withNamespace(metric.getMetric().getNamespace())
                .withDimensions(metric.getMetric().getDimensions())
                .withPeriod(periodInSeconds)
                .withMetricName(metric.getIncludeMetric().getName())
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

        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("No statistics retrieved for Namespace [%s] "
                            + "Account [%s] Region [%s] Metric [%s] Dimensions [%s]",
                    metric.getMetric().getNamespace(), accountName, region,
                    metric.getIncludeMetric().getName(), metric.getMetric().getDimensions()));
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

    private void setPeriodInSeconds (int periodInSeconds){
        this.periodInSeconds = periodInSeconds < 0 ? DEFAULT_METRIC_PERIOD_IN_SEC : periodInSeconds;
    }

    /**
     * Builder class to maintain readability when
     * building {@link MetricStatisticCollector} due to its params size
     */
    public static class Builder {

        private String accountName;

        private String region;

        private AmazonCloudWatch awsCloudWatch;

        private AWSMetric metric;

        private StatisticType statType;

        private MetricsTimeRange metricsTimeRange;

        private LongAdder awsRequestsCounter;

        private String metricPrefix;

        private Period periodInSeconds;

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

        public Builder withMetric(AWSMetric metric) {
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

        public Builder withAWSRequestCounter(LongAdder awsRequestsCounter) {
            this.awsRequestsCounter = awsRequestsCounter;
            return this;
        }

        public MetricStatisticCollector build() {
            return new MetricStatisticCollector(this);
        }

        public Builder withPrefix(String metricPrefix) {
            this.metricPrefix = metricPrefix;
            return this;
        }

        public Builder withPeriod(Period periodInSeconds) {
            this.periodInSeconds = periodInSeconds;
            return this;
        }

    }
}
