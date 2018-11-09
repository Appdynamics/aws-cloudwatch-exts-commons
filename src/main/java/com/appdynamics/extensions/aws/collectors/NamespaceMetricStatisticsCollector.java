/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.collectors;

import com.appdynamics.extensions.MonitorExecutorService;
import com.appdynamics.extensions.MonitorThreadPoolExecutor;
import com.appdynamics.extensions.aws.config.*;
import com.appdynamics.extensions.aws.exceptions.AwsException;
import com.appdynamics.extensions.aws.metric.AccountMetricStatistics;
import com.appdynamics.extensions.aws.metric.NamespaceMetricStatistics;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

import static com.appdynamics.extensions.aws.Constants.DEFAULT_NO_OF_THREADS;
import static com.appdynamics.extensions.aws.validators.Validator.validateNamespace;

/**
 * Collects statistics (of all specified accounts) for specified namespace
 *
 * @author Florencio Sarmiento
 */
public class NamespaceMetricStatisticsCollector implements Callable<List<Metric>> {

    private static Logger LOGGER = Logger.getLogger(NamespaceMetricStatisticsCollector.class);

    private List<Account> accounts;

    private MetricsConfig metricsConfig;

    private ConcurrencyConfig concurrencyConfig;

    private MetricsProcessor metricsProcessor;

    private CredentialsDecryptionConfig credentialsDecryptionConfig;

    private ProxyConfig proxyConfig;

    private LongAdder awsRequestsCounter = new LongAdder();

    private String metricPrefix;

    private List<Tag> tags;

    private NamespaceMetricStatisticsCollector(Builder builder) {
        this.accounts = builder.accounts;
        this.concurrencyConfig = builder.concurrencyConfig;
        this.metricsConfig = builder.metricsConfig;
        this.metricsProcessor = builder.metricsProcessor;
        this.credentialsDecryptionConfig = builder.credentialsDecryptionConfig;
        this.proxyConfig = builder.proxyConfig;
        this.metricPrefix = builder.metricPrefix;
        this.tags = builder.tags;
    }

    /**
     * Loops through each account for specified namespace and hands
     * off account metrics retrieval to {@link AccountMetricStatisticsCollector}
     * <p>
     * Uses {@link MetricsProcessor} to convert all stats retrieved
     * into a {@link Map<String, Double>} format
     * <p>
     * Returns the accumulated metrics statistics for specified namespace
     */
    public List<Metric> call() {
        LOGGER.info(String.format("Collecting statistics for Namespace [%s]",
                metricsProcessor.getNamespace()));

        if (accounts != null && !accounts.isEmpty()) {
            ScheduledExecutorService threadPool = null;

            try {
                validateNamespace(metricsProcessor.getNamespace());

                threadPool = Executors.newScheduledThreadPool(getNoOfAccountThreads());

                MonitorExecutorService executorService = new MonitorThreadPoolExecutor(new ScheduledThreadPoolExecutor(getNoOfAccountThreads()));


                List<FutureTask<AccountMetricStatistics>> tasks =
                        createConcurrentAccountTasks(executorService);

                NamespaceMetricStatistics namespaceMetrics = new NamespaceMetricStatistics();
                namespaceMetrics.setNamespace(metricsProcessor.getNamespace());

                collectMetrics(tasks, namespaceMetrics);
                List<Metric> metricStatsForUpload = metricsProcessor.createMetricStatsMapForUpload(namespaceMetrics);
                String total_requests = "AWS API Calls";
                Metric metric = new Metric(total_requests, Double.toString(awsRequestsCounter.doubleValue()), metricPrefix + total_requests,
                        MetricWriter.METRIC_AGGREGATION_TYPE_SUM, MetricWriter.METRIC_TIME_ROLLUP_TYPE_SUM, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
                metricStatsForUpload.add(metric);

                return metricStatsForUpload;

            } catch (Exception e) {
                throw new AwsException(
                        String.format(
                                "Error getting NamespaceMetricStatistics for Namespace [%s]",
                                metricsProcessor.getNamespace()), e);

            } finally {
                if (threadPool != null && !threadPool.isShutdown()) {
                    threadPool.shutdown();
                }
            }

        } else {
            LOGGER.info(String.format("No accounts to process for Namespace [%s]",
                    metricsProcessor.getNamespace()));
        }

        return Lists.newArrayList();
    }

    private List<FutureTask<AccountMetricStatistics>> createConcurrentAccountTasks(
            MonitorExecutorService executorService) {

        List<FutureTask<AccountMetricStatistics>> futureTasks = Lists.newArrayList();

        for (Account account : accounts) {
            AccountMetricStatisticsCollector accountTask =
                    new AccountMetricStatisticsCollector.Builder()
                            .withAccount(account)
                            .withMaxErrorRetrySize(metricsConfig.getMaxErrorRetrySize())
                            .withMetricsProcessor(metricsProcessor)
                            .withMetricsTimeRange(metricsConfig.getMetricsTimeRange())
                            .withPeriod(metricsConfig.getPeriodInSeconds())
                            .withNoOfMetricThreadsPerRegion(concurrencyConfig.getNoOfMetricThreadsPerRegion())
                            .withNoOfRegionThreadsPerAccount(concurrencyConfig.getNoOfRegionThreadsPerAccount())
                            .withThreadTimeOut(concurrencyConfig.getThreadTimeOut())
                            .withCredentialsDecryptionConfig(credentialsDecryptionConfig)
                            .withProxyConfig(proxyConfig)
                            .withRateLimiter(RateLimiter.create(metricsConfig.getGetMetricStatisticsRateLimit()))
                            .withAWSRequestCounter(awsRequestsCounter)
                            .withPrefix(metricPrefix)
                            .withTags(tags)
                            .build();

            FutureTask<AccountMetricStatistics> accountTaskExecutor = new FutureTask<AccountMetricStatistics>(accountTask);
            executorService.submit("NamespaceMetricStatisticsCollector", accountTaskExecutor);
            futureTasks.add(accountTaskExecutor);
        }

        return futureTasks;
    }

    private void collectMetrics(List<FutureTask<AccountMetricStatistics>> parallelTasks, NamespaceMetricStatistics namespaceMetricStatistics) {

        for (FutureTask<AccountMetricStatistics> task : parallelTasks) {
            try {
                AccountMetricStatistics accountStats = task.get(concurrencyConfig.getThreadTimeOut(), TimeUnit.SECONDS);
                namespaceMetricStatistics.add(accountStats);

            } catch (InterruptedException e) {
                LOGGER.error("Task interrupted. ", e);
            } catch (ExecutionException e) {
                LOGGER.error("Task execution failed. ", e);
            } catch (TimeoutException e) {
                LOGGER.error("Task timed out. ", e);
            }
        }
    }

    private int getNoOfAccountThreads() {
        int noOfAccountThreads = concurrencyConfig.getNoOfAccountThreads();
        return noOfAccountThreads > 0 ? noOfAccountThreads : DEFAULT_NO_OF_THREADS;
    }

    /**
     * Builder class to maintain readability when
     * building {@link NamespaceMetricStatisticsCollector} due to its params size
     */
    public static class Builder {

        private List<Account> accounts;
        private MetricsConfig metricsConfig;
        private ConcurrencyConfig concurrencyConfig;
        private MetricsProcessor metricsProcessor;
        private CredentialsDecryptionConfig credentialsDecryptionConfig;
        private ProxyConfig proxyConfig;
        private String metricPrefix;
        private List<Tag> tags;

        public Builder(List<Account> accounts,
                       ConcurrencyConfig concurrencyConfig,
                       MetricsConfig metricsConfig,
                       MetricsProcessor metricsProcessor, String metricPrefix) {
            this.accounts = accounts;
            this.concurrencyConfig = concurrencyConfig;
            this.metricsConfig = metricsConfig;
            this.metricsProcessor = metricsProcessor;
            this.metricPrefix = metricPrefix;

        }

        public Builder withCredentialsDecryptionConfig(CredentialsDecryptionConfig credentialsDecryptionConfig) {
            this.credentialsDecryptionConfig = credentialsDecryptionConfig;
            return this;
        }

        public Builder withProxyConfig(ProxyConfig proxyConfig) {
            this.proxyConfig = proxyConfig;
            return this;
        }

        public Builder withTags(List<Tag> tags){
            this.tags = tags;
            return this;
        }

        public NamespaceMetricStatisticsCollector build() {
            return new NamespaceMetricStatisticsCollector(this);
        }
    }
}
