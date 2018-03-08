/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.appdynamics.extensions.aws.config.IncludeMetric;
import com.appdynamics.extensions.aws.config.MetricsTimeRange;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.exceptions.AwsException;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.StatisticType;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.LongAdder;

@RunWith(MockitoJUnitRunner.class)
public class MetricStatisticsCollectorTest {

    private MetricStatisticCollector classUnderTest;

    @Mock
    private AWSMetric mockAWSMetric;

    @Mock
    private IncludeMetric mockIncludeMetric;

    @Mock
    private Metric mockMetric;

    @Mock
    private AmazonCloudWatch mockAwsCloudWatch;

    @Mock
    private GetMetricStatisticsResult mockGetMetricStatsResult;

    private LongAdder requestCounter = new LongAdder();

    @Before
    public void setup() {
        when(mockAWSMetric.getIncludeMetric()).thenReturn(mockIncludeMetric);
        when(mockAWSMetric.getMetric()).thenReturn(mockMetric);
    }

    @Test(expected = AwsException.class)
    public void testInvalidTimeRangeThrowsException() throws Exception {
        MetricsTimeRange invalidTimeRange = new MetricsTimeRange();
        invalidTimeRange.setEndTimeInMinsBeforeNow(10);
        invalidTimeRange.setStartTimeInMinsBeforeNow(5);

        classUnderTest = new MetricStatisticCollector.Builder()
                .withMetricsTimeRange(invalidTimeRange)
                .withMetric(mockAWSMetric)
                .withAWSRequestCounter(requestCounter)
                .build();

        classUnderTest.call();
    }

    @Test(expected = AwsException.class)
    public void testAwsCloudwatchThrowsException() throws Exception {
        when(mockAwsCloudWatch.getMetricStatistics(any(GetMetricStatisticsRequest.class)))
                .thenThrow(new AmazonServiceException("test exception"));

        classUnderTest = new MetricStatisticCollector.Builder()
                .withMetricsTimeRange(new MetricsTimeRange())
                .withMetric(mockAWSMetric)
                .withAwsCloudWatch(mockAwsCloudWatch)
                .withAWSRequestCounter(requestCounter)
                .build();

        classUnderTest.call();
    }

    @Test
    public void testLatestDatapointIsUsed() throws Exception {
        Datapoint latestDatapoint = createTestDatapoint(
                DateTime.now().toDate());

        Datapoint fiveMinsAgoDatapoint = createTestDatapoint(
                DateTime.now().minusMinutes(5).toDate());

        Datapoint tenMinsAgoDatapoint = createTestDatapoint(
                DateTime.now().minusMinutes(10).toDate());

        List<Datapoint> testDatapoints = Lists.newArrayList(latestDatapoint,
                fiveMinsAgoDatapoint, tenMinsAgoDatapoint);

        when(mockAwsCloudWatch.getMetricStatistics(any(GetMetricStatisticsRequest.class)))
                .thenReturn(mockGetMetricStatsResult);

        when(mockGetMetricStatsResult.getDatapoints()).thenReturn(testDatapoints);

        classUnderTest = new MetricStatisticCollector.Builder()
                .withMetricsTimeRange(new MetricsTimeRange())
                .withMetric(mockAWSMetric)
                .withAwsCloudWatch(mockAwsCloudWatch)
                .withStatType(StatisticType.SUM)
                .withAWSRequestCounter(requestCounter)
                .build();

        MetricStatistic result = classUnderTest.call();

        assertEquals(latestDatapoint.getSum(), result.getValue());
        assertEquals(latestDatapoint.getUnit(), result.getUnit());
    }

    @Test
    public void testNullDatapoint() throws Exception {
        List<Datapoint> testDatapoints = Lists.newArrayList(null, null);

        when(mockAwsCloudWatch.getMetricStatistics(any(GetMetricStatisticsRequest.class)))
                .thenReturn(mockGetMetricStatsResult);

        when(mockGetMetricStatsResult.getDatapoints()).thenReturn(testDatapoints);

        classUnderTest = new MetricStatisticCollector.Builder()
                .withMetricsTimeRange(new MetricsTimeRange())
                .withMetric(mockAWSMetric)
                .withAwsCloudWatch(mockAwsCloudWatch)
                .withStatType(StatisticType.SUM)
                .withAWSRequestCounter(requestCounter)
                .build();

        MetricStatistic result = classUnderTest.call();

        assertNull(result.getValue());
        assertNull(result.getUnit());
    }

    private Datapoint createTestDatapoint(Date timestamp) {
        Random random = new Random();

        Datapoint datapoint = new Datapoint();
        datapoint.setAverage(random.nextDouble());
        datapoint.setMaximum(random.nextDouble());
        datapoint.setMinimum(random.nextDouble());
        datapoint.setSampleCount(random.nextDouble());
        datapoint.setSum(random.nextDouble());
        datapoint.setTimestamp(timestamp);
        datapoint.setUnit(StandardUnit.Bits);
        return datapoint;
    }

}
