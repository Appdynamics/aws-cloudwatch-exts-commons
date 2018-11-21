/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.collectors;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.model.*;
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
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(MockitoJUnitRunner.class)
@PrepareForTest({LoggerFactory.class, MetricStatisticCollector.class})
public class MetricStatisticsCollectorTest {

    private MetricStatisticCollector classUnderTest;

    @Mock
    private AWSMetric mockAWSMetric;

    @Mock
    private IncludeMetric mockIncludeMetric;

    @Mock
    private Metric mockMetric;

    @Mock
    private AmazonCloudWatchAsync mockAwsCloudWatchAsync;

    @Mock
    private GetMetricStatisticsResult mockGetMetricStatsResult;

    @Mock
    private MetricsTimeRange mockMetricsTimeRange;

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
                .withPeriod(60)
                .withMetric(mockAWSMetric)
                .withAWSRequestCounter(requestCounter)
                .build();

        classUnderTest.call();
    }

    @Test(expected = AwsException.class)
    public void testAwsCloudwatchThrowsException() throws Exception {
        when(mockAwsCloudWatchAsync.getMetricStatistics(any(GetMetricStatisticsRequest.class)))
                .thenThrow(new AmazonServiceException("test exception"));

        classUnderTest = new MetricStatisticCollector.Builder()
                .withMetricsTimeRange(new MetricsTimeRange())
                .withPeriod(60)
                .withMetric(mockAWSMetric)
                .withAwsCloudWatch(mockAwsCloudWatchAsync)
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

        when(mockAwsCloudWatchAsync.getMetricStatistics(any(GetMetricStatisticsRequest.class)))
                .thenReturn(mockGetMetricStatsResult);

        when(mockGetMetricStatsResult.getDatapoints()).thenReturn(testDatapoints);

        classUnderTest = new MetricStatisticCollector.Builder()
                .withMetricsTimeRange(new MetricsTimeRange())
                .withPeriod(60)
                .withMetric(mockAWSMetric)
                .withAwsCloudWatch(mockAwsCloudWatchAsync)
                .withStatType(StatisticType.SUM)
                .withAWSRequestCounter(requestCounter)
                .build();

        MetricStatistic result = classUnderTest.call();

        assertEquals(latestDatapoint.getSum(), result.getValue());
        assertEquals(latestDatapoint.getUnit(), result.getUnit());
    }

    @Test
    public void whenDataPointsExceed1440ThenReturnNullMetric() throws Exception{
        MetricsTimeRange mockMetricsTimeRange = new MetricsTimeRange();
        mockMetricsTimeRange.setStartTimeInMinsBeforeNow(2880);
        mockMetricsTimeRange.setEndTimeInMinsBeforeNow(0);

        classUnderTest = new MetricStatisticCollector.Builder()
                .withMetricsTimeRange(mockMetricsTimeRange)
                .withPeriod(60)
                .withMetric(mockAWSMetric)
                .withAwsCloudWatch(mockAwsCloudWatchAsync)
                .withStatType(StatisticType.AVE)
                .withAWSRequestCounter(requestCounter)
                .build();

        MetricStatistic result = classUnderTest.call();
        assertNull(result.getValue());

    }

    @Test
    public void testNullDatapoint() throws Exception {
        List<Datapoint> testDatapoints = Lists.newArrayList(null, null);

        when(mockAwsCloudWatchAsync.getMetricStatistics(any(GetMetricStatisticsRequest.class)))
                .thenReturn(mockGetMetricStatsResult);

        when(mockGetMetricStatsResult.getDatapoints()).thenReturn(testDatapoints);

        classUnderTest = new MetricStatisticCollector.Builder()
                .withMetricsTimeRange(new MetricsTimeRange())
                .withPeriod(60)
                .withMetric(mockAWSMetric)
                .withAwsCloudWatch(mockAwsCloudWatchAsync)
                .withStatType(StatisticType.SUM)
                .withAWSRequestCounter(requestCounter)
                .build();

        MetricStatistic result = classUnderTest.call();

        assertNull(result.getValue());
        assertNull(result.getUnit());
    }

    @Test
    public void testIndividualMetricTimeRanges() throws NoSuchFieldException, IllegalAccessException {
        MetricsTimeRange timeRange = new MetricsTimeRange();
        timeRange.setEndTimeInMinsBeforeNow(0);
        timeRange.setStartTimeInMinsBeforeNow(5);

        int startTime = 15;
        int endTime = 10;
        when(mockAWSMetric.getIncludeMetric()).thenReturn(mockIncludeMetric);
        when(mockIncludeMetric.getMetricsTimeRange()).thenReturn(mockMetricsTimeRange);
        when(mockMetricsTimeRange.getStartTimeInMinsBeforeNow()).thenReturn(startTime);
        when(mockMetricsTimeRange.getEndTimeInMinsBeforeNow()).thenReturn(endTime);


        classUnderTest = new MetricStatisticCollector.Builder()
                .withMetricsTimeRange(timeRange)
                .withPeriod(60)
                .withMetric(mockAWSMetric)
                .withAWSRequestCounter(requestCounter)
                .build();


        Field startTimeInMinsBeforeNow = getField(classUnderTest.getClass(), "startTimeInMinsBeforeNow");
        int startTimeInMinsBeforeNowValue = (Integer) startTimeInMinsBeforeNow.get(classUnderTest);
        assertEquals(startTime, startTimeInMinsBeforeNowValue);


        Field endTimeInMinsBeforeNow = getField(classUnderTest.getClass(), "endTimeInMinsBeforeNow");
        int endTimeInMinsBeforeNowValue = (Integer) endTimeInMinsBeforeNow.get(classUnderTest);
        assertEquals(endTime, endTimeInMinsBeforeNowValue);
    }

    private Field getField(Class<?> thisClass, String name) throws NoSuchFieldException {
        Field field = thisClass.getDeclaredField(name);
        field.setAccessible(true);

        return field;
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
