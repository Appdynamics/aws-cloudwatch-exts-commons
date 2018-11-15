/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.collectors;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClientBuilder;
import com.appdynamics.extensions.MonitorExecutorService;
import com.appdynamics.extensions.MonitorThreadPoolExecutor;
import com.appdynamics.extensions.aws.config.MetricsTimeRange;
import com.appdynamics.extensions.aws.config.Tags;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.exceptions.AwsException;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

import static com.appdynamics.extensions.aws.Constants.DEFAULT_NO_OF_THREADS;

/**
 * Collects statistics (of specified metrics) for specified region.
 *
 * @author Florencio Sarmiento
 */
public class RegionMetricStatisticsCollector implements Callable<RegionMetricStatistics> {

    private static Logger LOGGER = Logger.getLogger(RegionMetricStatisticsCollector.class);

    private String accountName;

    private String region;

    private MetricsProcessor metricsProcessor;

    private int noOfMetricThreadsPerRegion;

    private int threadTimeOut;

    private MetricsTimeRange metricsTimeRange;

    private AmazonCloudWatch awsCloudWatch;

    private RateLimiter rateLimiter;

    private LongAdder awsRequestsCounter;

    private String metricPrefix;

    private int periodInSeconds;

    private List<Tags> tags;

    private RegionMetricStatisticsCollector(Builder builder) {

        this.accountName = builder.accountName;
        this.region = builder.region;
        this.awsCloudWatch = builder.amazonCloudWatchClient;
        this.metricsTimeRange = builder.metricsTimeRange;
        this.metricsProcessor = builder.metricsProcessor;
        this.rateLimiter = builder.rateLimiter;
        this.awsRequestsCounter = builder.awsRequestsCounter;
        this.metricPrefix = builder.metricPrefix;
        this.threadTimeOut = builder.threadTimeOut;
        this.periodInSeconds = builder.periodInSeconds;
        this.tags = builder.tags;

        setNoOfMetricThreadsPerRegion(builder.noOfMetricThreadsPerRegion);
    }

    /**
     * Uses {@link MetricsProcessor} to retrieve metric names
     * then hands off individual metric statistics retrieval to
     * {@link MetricStatisticCollector}
     * <p>
     * Returns the accumulated metrics statistics for specified region
     */
    public RegionMetricStatistics call() {
        RegionMetricStatistics regionMetricStats = null;

        MonitorExecutorService executorService = null;

        try {
            //RegionEndpointProvider regionEndpointProvider =
//                    RegionEndpointProvider.getInstance();

            //validateRegion(region, regionEndpointProvider);
           LOGGER.info(String.format(
                    "Collecting RegionMetricStatistics for Namespace [%s] Account [%s] Region [%s]",
                    metricsProcessor.getNamespace(), accountName, region));

//            this.awsCloudWatch.setEndpoint(regionEndpointProvider.getEndpoint(region));


            List<AWSMetric> metrics = metricsProcessor.getMetrics(awsCloudWatch, accountName, awsRequestsCounter); //--> list-metrics call

            metrics = metricsProcessor.filterUsingTags(metrics, tags, region);

            regionMetricStats = new RegionMetricStatistics();
            regionMetricStats.setRegion(region);

            if (metrics != null && !metrics.isEmpty()) {


                executorService = new MonitorThreadPoolExecutor(new ScheduledThreadPoolExecutor(noOfMetricThreadsPerRegion));
                List<FutureTask<MetricStatistic>> tasks = createConcurrentMetricTasks(
                        executorService, metrics);
                collectMetrics(tasks, metrics.size(), regionMetricStats);

            } else {
                LOGGER.info(String.format(
                        "No metric names available to process for Namespace [%s] Account [%s] Region [%s]",
                        metricsProcessor.getNamespace(), accountName, region));
            }

        } catch (Exception e) {
            throw new AwsException(String.format(
                    "Error getting RegionMetricStatistics for Namespace [%s] Account [%s] Region [%s]",
                    metricsProcessor.getNamespace(), accountName, region), e);

        } finally {
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
        }

        return regionMetricStats;
    }

    private List<FutureTask<MetricStatistic>> createConcurrentMetricTasks(MonitorExecutorService executorService,
                                                                          List<AWSMetric> metrics) {

        List<FutureTask<MetricStatistic>> futureTasks = Lists.newArrayList();

        long startTime = DateTime.now().getSecondOfMinute();
        for (AWSMetric metric : metrics) {

            //Limit the number of requests per second. Limit can be configured using getMetricStatisticsRateLimit config
            rateLimiter.acquire();

            MetricStatisticCollector metricTask =
                    new MetricStatisticCollector.Builder()
                            .withAccountName(accountName)
                            .withRegion(region)
                            .withAwsCloudWatch(awsCloudWatch)
                            .withMetric(metric)
                            .withMetricsTimeRange(metricsTimeRange)
                            .withPeriod(periodInSeconds)
                            .withStatType(metricsProcessor.getStatisticType(metric))
                            .withAWSRequestCounter(awsRequestsCounter)
                            .withPrefix(metricPrefix)
                            .withTags(tags)
                            .build();

            FutureTask<MetricStatistic> accountTaskExecutor = new FutureTask<MetricStatistic>(metricTask);

            executorService.submit("RegionMetricStatisticsCollector", accountTaskExecutor);
            futureTasks.add(accountTaskExecutor);
        }
        long elapsedTimeSeconds = DateTime.now().getSecondOfMinute() - startTime;

        LOGGER.debug("Get metric statistics took " + elapsedTimeSeconds);

        return futureTasks;
    }

    private void collectMetrics(List<FutureTask<MetricStatistic>> parallelTasks,
                                int taskSize, RegionMetricStatistics regionMetricStatistics) {

        for (FutureTask<MetricStatistic> task : parallelTasks) {

            try {
                MetricStatistic metricStatistics = task.get(threadTimeOut, TimeUnit.SECONDS);
                regionMetricStatistics.addMetricStatistic(metricStatistics);

            } catch (InterruptedException e) {
                LOGGER.error("Task interrupted. ", e);
            } catch (ExecutionException e) {
                LOGGER.error("Task execution failed. ", e);
            } catch (TimeoutException e) {
                LOGGER.error("Task timed out. ", e);
            }
        }
    }

    private void setNoOfMetricThreadsPerRegion(int noOfMetricThreadsPerRegion) {
        this.noOfMetricThreadsPerRegion = noOfMetricThreadsPerRegion > 0 ?
                noOfMetricThreadsPerRegion : DEFAULT_NO_OF_THREADS;
    }

    /**
     * Builder class to maintain readability when
     * building {@link RegionMetricStatisticsCollector} due to its params size
     */
    public static class Builder {

        private String accountName;

        private String region;

        private MetricsProcessor metricsProcessor;

        private int noOfMetricThreadsPerRegion;

        private int threadTimeOut;

        private MetricsTimeRange metricsTimeRange;

       // private AmazonCloudWatch awsCloudWatch;
       // private AmazonCloudWatchClientBuilder amazonCloudWatchClientBuilder =  AmazonCloudWatchClientBuilder.standard();

        private AmazonCloudWatchAsync amazonCloudWatchClient;

        private RateLimiter rateLimiter;

        private LongAdder awsRequestsCounter;

        private String metricPrefix;

        private int periodInSeconds;

        private List<Tags> tags;

        public Builder withAccountName(String accountName) {
            this.accountName = accountName;
            return this;
        }

        public Builder withRegion(String region) {
            this.region = region;
            return this;
        }

        public Builder withMetricsProcessor(MetricsProcessor metricsProcessor) {
            this.metricsProcessor = metricsProcessor;
            return this;
        }

        public Builder withNoOfMetricThreadsPerRegion(int noOfMetricThreadsPerRegion) {
            this.noOfMetricThreadsPerRegion = noOfMetricThreadsPerRegion;
            return this;
        }

        public Builder withAmazonCloudWatchConfig(AWSCredentials awsCredentials,
                                                  ClientConfiguration awsClientConfig) {
            if (awsCredentials == null) {
                LOGGER.info("Credentials not provided trying to use instance profile");
               // this.awsCloudWatch = new AmazonCloudWatchClient(new InstanceProfileCredentialsProvider(), awsClientConfig);
                this.amazonCloudWatchClient =  AmazonCloudWatchAsyncClientBuilder.standard()
                                                .withCredentials(new InstanceProfileCredentialsProvider(true).getInstance())
                                                .withClientConfiguration(awsClientConfig)
                                                .withRegion(this.region)
                                                .build();
            } else {
               // this.awsCloudWatch = new AmazonCloudWatchClient(awsCredentials, awsClientConfig);
                LOGGER.debug("Creating client for "+this.region);
                this.amazonCloudWatchClient = AmazonCloudWatchAsyncClientBuilder.standard()
                                                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                                                .withExecutorFactory(() -> Executors.newFixedThreadPool(10))
                                                .withRegion(this.region)
                                                .withClientConfiguration(awsClientConfig)
                                                .build();
            }
            return this;
        }

        public Builder withMetricsTimeRange(MetricsTimeRange metricsTimeRange) {
            this.metricsTimeRange = metricsTimeRange;
            return this;
        }

        public Builder withRateLimiter(RateLimiter rateLimiter) {
            this.rateLimiter = rateLimiter;
            return this;
        }

        public Builder withAWSRequestCounter(LongAdder awsRequestsCounter) {
            this.awsRequestsCounter = awsRequestsCounter;
            return this;
        }

        public RegionMetricStatisticsCollector build() {
            return new RegionMetricStatisticsCollector(this);
        }

        public Builder withPrefix(String metricPrefix) {
            this.metricPrefix = metricPrefix;
            return this;
        }

        public Builder withThreadTimeOut(int threadTimeOut) {
            this.threadTimeOut = threadTimeOut;
            return this;
        }

        public Builder withPeriodInSeconds(int periodInSeconds) {
            this.periodInSeconds = periodInSeconds;
            return this;
        }

        public Builder withTags(List<Tags> tags){
            this.tags = tags;
            return  this;
        }
    }
}
