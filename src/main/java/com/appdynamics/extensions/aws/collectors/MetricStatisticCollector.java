/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.collectors;

import com.appdynamics.extensions.aws.config.MetricsTimeRange;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.exceptions.AwsException;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.StatisticType;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.LongAdder;

import static com.appdynamics.extensions.aws.Constants.DEFAULT_END_TIME_IN_MINS_BEFORE_NOW;
import static com.appdynamics.extensions.aws.Constants.DEFAULT_METRIC_PERIOD_IN_SEC;
import static com.appdynamics.extensions.aws.Constants.DEFAULT_START_TIME_IN_MINS_BEFORE_NOW;
import static com.appdynamics.extensions.aws.validators.Validator.validateTimeRange;

/**
 * Retrieves statistics for the specified metric.
 * <p>
 * <p>Cloudwatch Limitation:
 * The maximum number of data points that can be queried is 50,850,
 * whereas the maximum number of data points returned from a single
 * GetMetricStatistics request is 1,440.
 * <p>
 * <p>
 *
 * @author Florencio Sarmiento
 */
public class MetricStatisticCollector implements Callable<MetricStatistic> {

    private static Logger LOGGER = ExtensionsLoggerFactory.getLogger(MetricStatisticCollector.class);

    private String accountName;

    private String region;

    private CloudWatchClient awsCloudWatch;

    private AWSMetric metric;

    private StatisticType statType;

    private int startTimeInMinsBeforeNow;

    private int endTimeInMinsBeforeNow;

    private LongAdder awsRequestsCounter;

    private String metricPrefix;

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
    }

    /**
     * Uses {@link CloudWatchClient} to retrieve metric datapoints.
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
                        metric.getMetric().namespace(), accountName, region,
                        metric.getIncludeMetric().getName(), metric.getMetric().dimensions()));
            }

            metricStatistic = new MetricStatistic();
            metricStatistic.setMetric(metric);
            metricStatistic.setMetricPrefix(metricPrefix);

            GetMetricStatisticsRequest request = createGetMetricStatisticsRequest();
            GetMetricStatisticsResponse result = awsCloudWatch.getMetricStatistics(request);
            awsRequestsCounter.increment();

            Datapoint latestDatapoint = getLatestDatapoint(result.datapoints());

            if (latestDatapoint != null) {
                Double value = getValue(latestDatapoint);
                metricStatistic.setValue(value);
                metricStatistic.setUnit(latestDatapoint.unitAsString());
            }

        } catch (Exception e) {
            throw new AwsException(String.format(
                    "Error getting MetricStatistic for Namespace [%s] "
                            + "Account [%s] Region [%s] Metric [%s] Dimensions [%s]",
                    metric.getMetric().namespace(), accountName, region,
                    metric.getIncludeMetric().getName(), metric.getMetric().dimensions()), e);
        }

        return metricStatistic;
    }

    private GetMetricStatisticsRequest createGetMetricStatisticsRequest() {
        GetMetricStatisticsRequest getMetricStatisticsRequest = GetMetricStatisticsRequest.builder()
                .startTime(DateTime.now(DateTimeZone.UTC).minusMinutes(startTimeInMinsBeforeNow).toDate().toInstant())
                .namespace(metric.getMetric().namespace())
                .dimensions(metric.getMetric().dimensions())
                .period(DEFAULT_METRIC_PERIOD_IN_SEC)
                .metricName(metric.getIncludeMetric().getName())
                .statistics(statType.asStatistic())
                .endTime(DateTime.now(DateTimeZone.UTC).minusMinutes(endTimeInMinsBeforeNow).toDate().toInstant())
                .build();

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
                    metric.getMetric().namespace(), accountName, region,
                    metric.getIncludeMetric().getName(), metric.getMetric().dimensions()));
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
            return datapoint != null ? Date.from(datapoint.timestamp()) : null;
        }
    }

    private Double getValue(Datapoint datapoint) {
        Double value = null;

        if (datapoint != null) {
            switch (statType) {
                case AVE:
                    value = datapoint.average();
                    break;
                case MAX:
                    value = datapoint.maximum();
                    break;
                case MIN:
                    value = datapoint.minimum();
                    break;
                case SUM:
                    value = datapoint.sum();
                    break;
                case SAMPLE_COUNT:
                    value = datapoint.sampleCount();
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

    /**
     * Builder class to maintain readability when
     * building {@link MetricStatisticCollector} due to its params size
     */
    public static class Builder {

        private String accountName;

        private String region;

        private CloudWatchClient awsCloudWatch;

        private AWSMetric metric;

        private StatisticType statType;

        private MetricsTimeRange metricsTimeRange;

        private LongAdder awsRequestsCounter;

        private String metricPrefix;

        public Builder withAccountName(String accountName) {
            this.accountName = accountName;
            return this;
        }

        public Builder withRegion(String region) {
            this.region = region;
            return this;
        }

        public Builder withAwsCloudWatch(
                CloudWatchClient awsCloudWatch) {
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
    }
}
