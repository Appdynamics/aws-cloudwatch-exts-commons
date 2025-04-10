/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.collectors;

import com.appdynamics.extensions.aws.config.AwsClientConfig;
import com.appdynamics.extensions.aws.config.MetricsTimeRange;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.exceptions.AwsException;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;
import com.appdynamics.extensions.aws.providers.RegionEndpointProvider;
import com.appdynamics.extensions.executorservice.MonitorExecutorService;
import com.appdynamics.extensions.executorservice.MonitorThreadPoolExecutor;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import java.net.URI;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClientBuilder;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;

import static com.appdynamics.extensions.aws.Constants.DEFAULT_NO_OF_THREADS;
import static com.appdynamics.extensions.aws.validators.Validator.validateRegion;

/**
 * Collects statistics (of specified metrics) for specified region.
 *
 * @author Florencio Sarmiento
 */
public class RegionMetricStatisticsCollector implements Callable<RegionMetricStatistics> {

    private static Logger LOGGER = ExtensionsLoggerFactory.getLogger(RegionMetricStatisticsCollector.class);

    private String accountName;

    private String region;

    private MetricsProcessor metricsProcessor;

    private int noOfMetricThreadsPerRegion;

    private int threadTimeOut;

    private MetricsTimeRange metricsTimeRange;

    private CloudWatchClient awsCloudWatch;

    private RateLimiter rateLimiter;

    private LongAdder awsRequestsCounter;

    private String metricPrefix;

    private RegionMetricStatisticsCollector(Builder builder) {

        this.accountName = builder.accountName;
        this.region = builder.region;
        this.awsCloudWatch = builder.awsCloudWatch;
        this.metricsTimeRange = builder.metricsTimeRange;
        this.metricsProcessor = builder.metricsProcessor;
        this.rateLimiter = builder.rateLimiter;
        this.awsRequestsCounter = builder.awsRequestsCounter;
        this.metricPrefix = builder.metricPrefix;
        this.threadTimeOut = builder.threadTimeOut;

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
            RegionEndpointProvider regionEndpointProvider =
                    RegionEndpointProvider.getInstance();

            validateRegion(region, regionEndpointProvider);

            LOGGER.info(String.format(
                    "Collecting RegionMetricStatistics for Namespace [%s] Account [%s] Region [%s]",
                    metricsProcessor.getNamespace(), accountName, region));

            List<AWSMetric> metrics = metricsProcessor.getMetrics(awsCloudWatch, accountName, awsRequestsCounter);

            regionMetricStats = new RegionMetricStatistics();
            regionMetricStats.setRegion(region);

            if (metrics != null && !metrics.isEmpty()) {

                executorService = new MonitorThreadPoolExecutor((ThreadPoolExecutor) Executors.newFixedThreadPool(noOfMetricThreadsPerRegion));


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

        long startTime = System.currentTimeMillis();
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
                            .withStatType(metricsProcessor.getStatisticType(metric))
                            .withAWSRequestCounter(awsRequestsCounter)
                            .withPrefix(metricPrefix)
                            .build();

            FutureTask<MetricStatistic> accountTaskExecutor = new FutureTask<MetricStatistic>(metricTask);

            executorService.submit("RegionMetricStatisticsCollector", accountTaskExecutor);
            futureTasks.add(accountTaskExecutor);
        }
        long elapsedTime = System.currentTimeMillis() - startTime;

        LOGGER.debug("Get metric statistics took " + elapsedTime +"(ms)");

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

        private CloudWatchClient awsCloudWatch;

        private RateLimiter rateLimiter;

        private LongAdder awsRequestsCounter;

        private String metricPrefix;

        private AWSClientCache awsClientCache = AWSClientCache.getInstance();

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

        public Builder withAmazonCloudWatchConfig(AwsCredentialsProvider awsCredentials, AwsClientConfig awsClientConfig) {
            // Derive the endpoint URI from your endpoint provider.
            String endpointUrl = RegionEndpointProvider.getInstance().getEndpoint(region);
            URI endpointUri = URI.create("https://" + endpointUrl);
            LOGGER.debug(String.format("Endpoint URI: %s",endpointUri));

            // Retrieve a CloudWatchClient from the cache, keyed by the endpoint URI.
            CloudWatchClient cloudWatchClient = awsClientCache.get(endpointUri.toString());
            if (cloudWatchClient == null) {
                LOGGER.info("CloudWatch client not found in cache; creating a new client and adding it to cache.");

                CloudWatchClientBuilder clientBuilder = CloudWatchClient.builder()
                        .endpointOverride(endpointUri)
                        .region(Region.of(region))
                        .httpClient(awsClientConfig.getHttpClient())
                        .overrideConfiguration(awsClientConfig.getOverrideConfiguration());

                if (awsCredentials == null) {
                    clientBuilder.credentialsProvider(InstanceProfileCredentialsProvider.create());
                    LOGGER.info("No credentials provided; using instance profile credentials.");
                } else {
                    clientBuilder.credentialsProvider(awsCredentials);
                    LOGGER.info("Credentials provided; using supplied AWS credentials provider.");
                }

                cloudWatchClient = clientBuilder.build();
                awsClientCache.put(endpointUri, cloudWatchClient);
            } else {
                cloudWatchClient = awsClientCache.get(endpointUri);
            }
            this.awsCloudWatch = cloudWatchClient;
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
    }
}
