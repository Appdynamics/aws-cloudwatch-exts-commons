package com.appdynamics.extensions.aws.collectors;

import static com.appdynamics.extensions.aws.Constants.DEFAULT_NO_OF_THREADS;
import static com.appdynamics.extensions.aws.Constants.DEFAULT_THREAD_TIMEOUT;
import static com.appdynamics.extensions.aws.validators.Validator.validateRegion;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.appdynamics.extensions.aws.config.MetricsTimeRange;
import com.appdynamics.extensions.aws.exceptions.AwsException;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;
import com.appdynamics.extensions.aws.providers.RegionEndpointProvider;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Collects statistics (of specified metrics) for specified region.
 *
 * @author Florencio Sarmiento
 */
public class RegionMetricStatisticsCollector implements Callable<RegionMetricStatistics> {

    private static Logger LOGGER = Logger.getLogger("com.singularity.extensions.aws.RegionMetricStatisticsCollector");

    private String accountName;

    private String region;

    private MetricsProcessor metricsProcessor;

    private int noOfMetricThreadsPerRegion;

    private MetricsTimeRange metricsTimeRange;

    private AmazonCloudWatch awsCloudWatch;

    private RegionMetricStatisticsCollector(Builder builder) {

        this.accountName = builder.accountName;
        this.region = builder.region;
        this.awsCloudWatch = builder.awsCloudWatch;
        this.metricsTimeRange = builder.metricsTimeRange;
        this.metricsProcessor = builder.metricsProcessor;

        setNoOfMetricThreadsPerRegion(builder.noOfMetricThreadsPerRegion);
    }

    /**
     * Uses {@link MetricsProcessor} to retrieve metric names
     * then hands off individual metric statistics retrieval to
     * {@link MetricStatisticCollector}
     * <p>
     * Returns the accumulated metrics statistics for specified region
     */
    public RegionMetricStatistics call() throws Exception {
        RegionMetricStatistics regionMetricStats = null;
        ExecutorService threadPool = null;

        try {
            RegionEndpointProvider regionEndpointProvider =
                    RegionEndpointProvider.getInstance();

            validateRegion(region, regionEndpointProvider);

            LOGGER.info(String.format(
                    "Collecting RegionMetricStatistics for Namespace [%s] Account [%s] Region [%s]",
                    metricsProcessor.getNamespace(), accountName, region));

            this.awsCloudWatch.setEndpoint(regionEndpointProvider.getEndpoint(region));
            List<Metric> metrics = metricsProcessor.getMetrics(awsCloudWatch, accountName);

            regionMetricStats = new RegionMetricStatistics();
            regionMetricStats.setRegion(region);

            if (metrics != null && !metrics.isEmpty()) {
                threadPool = Executors.newFixedThreadPool(noOfMetricThreadsPerRegion);
                CompletionService<MetricStatistic> tasks = createConcurrentMetricTasks(
                        threadPool, metrics);
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
            if (threadPool != null && !threadPool.isShutdown()) {
                threadPool.shutdown();
            }
        }

        return regionMetricStats;
    }

    private CompletionService<MetricStatistic> createConcurrentMetricTasks(ExecutorService threadPool,
                                                                           List<Metric> metrics) {

        CompletionService<MetricStatistic> metricTasks =
                new ExecutorCompletionService<MetricStatistic>(threadPool);

        for (Metric metric : metrics) {
            MetricStatisticCollector metricTask =
                    new MetricStatisticCollector.Builder()
                            .withAccountName(accountName)
                            .withRegion(region)
                            .withAwsCloudWatch(awsCloudWatch)
                            .withMetric(metric)
                            .withMetricsTimeRange(metricsTimeRange)
                            .withStatType(metricsProcessor.getStatisticType(metric))
                            .build();

            metricTasks.submit(metricTask);
        }

        return metricTasks;
    }

    private void collectMetrics(CompletionService<MetricStatistic> parallelTasks,
                                int taskSize, RegionMetricStatistics regionMetricStatistics) {

        for (int index = 0; index < taskSize; index++) {

            try {
                MetricStatistic metricStatistics = parallelTasks.take().get(DEFAULT_THREAD_TIMEOUT, TimeUnit.SECONDS);
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

        private MetricsTimeRange metricsTimeRange;

        private AmazonCloudWatch awsCloudWatch;

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
                this.awsCloudWatch = new AmazonCloudWatchClient(new InstanceProfileCredentialsProvider(), awsClientConfig);
            } else {
                this.awsCloudWatch = new AmazonCloudWatchClient(awsCredentials, awsClientConfig);
            }
            return this;
        }

        public Builder withMetricsTimeRange(MetricsTimeRange metricsTimeRange) {
            this.metricsTimeRange = metricsTimeRange;
            return this;
        }

        public RegionMetricStatisticsCollector build() {
            return new RegionMetricStatisticsCollector(this);
        }
    }
}
