/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.collectors;

import static com.appdynamics.extensions.aws.Constants.DEFAULT_MAX_ERROR_RETRY;
import static com.appdynamics.extensions.aws.Constants.DEFAULT_NO_OF_THREADS;
import static com.appdynamics.extensions.aws.util.AWSUtil.createAWSClientConfiguration;
import static com.appdynamics.extensions.aws.util.AWSUtil.createAWSCredentials;
import static com.appdynamics.extensions.aws.validators.Validator.validateAccount;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.appdynamics.extensions.MonitorExecutorService;
import com.appdynamics.extensions.MonitorThreadPoolExecutor;
import com.appdynamics.extensions.aws.config.*;
import com.appdynamics.extensions.aws.exceptions.AwsException;
import com.appdynamics.extensions.aws.metric.AccountMetricStatistics;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;

/**
 * Collects statistics (of specified regions) for specified account
 *
 * @author Florencio Sarmiento
 */
public class AccountMetricStatisticsCollector implements Callable<AccountMetricStatistics> {

    private static Logger LOGGER = Logger.getLogger(AccountMetricStatisticsCollector.class);

    private Account account;

    private int noOfRegionThreadsPerAccount;

    private int noOfMetricThreadsPerRegion;

    private int threadTimeOut;

    private MetricsTimeRange metricsTimeRange;

    private MetricsProcessor metricsProcessor;

    private int maxErrorRetrySize;

    private CredentialsDecryptionConfig credentialsDecryptionConfig;

    private ProxyConfig proxyConfig;

    private RateLimiter rateLimiter;
    private LongAdder awsRequestsCounter;

    private String metricPrefix;

    private DashboardConfig dashboardConfig;
    private ControllerInformation controllerInformation;


    private AccountMetricStatisticsCollector(Builder builder) {
        this.account = builder.account;
        this.noOfMetricThreadsPerRegion = builder.noOfMetricThreadsPerRegion;
        this.metricsTimeRange = builder.metricsTimeRange;
        this.metricsProcessor = builder.metricsProcessor;
        this.credentialsDecryptionConfig = builder.credentialsDecryptionConfig;
        this.proxyConfig = builder.proxyConfig;
        this.rateLimiter = builder.rateLimiter;
        this.awsRequestsCounter = builder.awsRequestsCounter;
        this.metricPrefix = builder.metricPrefix;
        this.threadTimeOut = builder.threadTimeOut;
        this.dashboardConfig = dashboardConfig;
        this.controllerInformation = controllerInformation;


        setNoOfRegionThreadsPerAccount(builder.noOfRegionThreadsPerAccount);
        setMaxErrorRetrySize(builder.maxErrorRetrySize);
    }

    /**
     * Loops through each region for specified account and hands
     * off region metrics retrieval to {@link RegionMetricStatisticsCollector}
     * <p>
     * Returns the accumulated metrics statistics for specified account
     */
    public AccountMetricStatistics call() {
        AccountMetricStatistics accountStats = null;

        MonitorExecutorService executorService = null;

        try {
            validateAccount(account);

            LOGGER.info(String.format(
                    "Collecting AccountMetricStatistics for Namespace [%s] Account [%s]",
                    metricsProcessor.getNamespace(), account.getDisplayAccountName()));

            accountStats = new AccountMetricStatistics();
            accountStats.setAccountName(account.getDisplayAccountName());

            AWSCredentials awsCredentials = null;
            if (StringUtils.isNotEmpty(account.getAwsAccessKey()) && StringUtils.isNotEmpty(account.getAwsSecretKey())) {
                awsCredentials = createAWSCredentials(account, credentialsDecryptionConfig);
            }

            ClientConfiguration awsClientConfig = createAWSClientConfiguration(maxErrorRetrySize, proxyConfig);

            executorService = new MonitorThreadPoolExecutor(new ScheduledThreadPoolExecutor(noOfRegionThreadsPerAccount));


            List<FutureTask<RegionMetricStatistics>> tasks = createConcurrentRegionTasks(
                    executorService, account.getRegions(), awsCredentials, awsClientConfig);
            collectMetrics(tasks, account.getRegions().size(), accountStats);

        } catch (Exception e) {
            throw new AwsException(
                    String.format(
                            "Error getting AccountMetricStatistics for Namespace [%s] Account [%s]",
                            metricsProcessor.getNamespace(),
                            account.getDisplayAccountName()), e);

        } finally {
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
        }


        return accountStats;
    }

    private List<FutureTask<RegionMetricStatistics>> createConcurrentRegionTasks(
            MonitorExecutorService executorService,
            Set<String> regions,
            AWSCredentials awsCredentials,
            ClientConfiguration awsClientConfig) {

        List<FutureTask<RegionMetricStatistics>> futureTasks = Lists.newArrayList();

        for (String region : regions) {
            RegionMetricStatisticsCollector regionTask =
                    new RegionMetricStatisticsCollector.Builder()
                            .withAccountName(account.getDisplayAccountName())
                            .withAmazonCloudWatchConfig(awsCredentials, awsClientConfig)
                            .withMetricsProcessor(metricsProcessor)
                            .withMetricsTimeRange(metricsTimeRange)
                            .withNoOfMetricThreadsPerRegion(noOfMetricThreadsPerRegion)
                            .withThreadTimeOut(threadTimeOut)
                            .withRegion(region)
                            .withRateLimiter(rateLimiter)
                            .withAWSRequestCounter(awsRequestsCounter)
                            .withPrefix(metricPrefix)
                            .build();

            FutureTask<RegionMetricStatistics> regionTaskExecutor = new FutureTask<RegionMetricStatistics>(regionTask);

            executorService.submit("AccountMetricStatisticsCollector", regionTaskExecutor);
            futureTasks.add(regionTaskExecutor);
        }

        return futureTasks;
    }

    private void collectMetrics(List<FutureTask<RegionMetricStatistics>> parallelTasks,
                                int taskSize, AccountMetricStatistics accountMetricStatistics) {

        for (FutureTask<RegionMetricStatistics> task : parallelTasks) {
            try {
                RegionMetricStatistics regionStats = task.get(threadTimeOut, TimeUnit.SECONDS);
                accountMetricStatistics.add(regionStats);

            } catch (InterruptedException e) {
                LOGGER.error("Task interrupted. ", e);
            } catch (ExecutionException e) {
                LOGGER.error("Task execution failed. ", e);
            } catch (TimeoutException e) {
                LOGGER.error("Task timed out. ", e);
            }
        }

    }

    private void setMaxErrorRetrySize(int maxErrorRetrySize) {
        this.maxErrorRetrySize = maxErrorRetrySize < DEFAULT_MAX_ERROR_RETRY ?
                DEFAULT_MAX_ERROR_RETRY : maxErrorRetrySize;
    }

    private void setNoOfRegionThreadsPerAccount(int noOfRegionThreadsPerAccount) {
        this.noOfRegionThreadsPerAccount = noOfRegionThreadsPerAccount > 0 ?
                noOfRegionThreadsPerAccount : DEFAULT_NO_OF_THREADS;
    }

    /**
     * Builder class to maintain readability when
     * building {@link AccountMetricStatisticsCollector} due to its params size
     */
    public static class Builder {

        private Account account;
        private int noOfRegionThreadsPerAccount;
        private int noOfMetricThreadsPerRegion;
        private int threadTimeOut;
        private MetricsTimeRange metricsTimeRange;
        private MetricsProcessor metricsProcessor;
        private int maxErrorRetrySize;
        private CredentialsDecryptionConfig credentialsDecryptionConfig;
        private ProxyConfig proxyConfig;
        private RateLimiter rateLimiter;
        private LongAdder awsRequestsCounter;
        private String metricPrefix;
        private DashboardConfig dashboardConfig;
        private ControllerInformation controllerInfo;

        public Builder withAccount(Account account) {
            this.account = account;
            return this;
        }

        public Builder withNoOfRegionThreadsPerAccount(int noOfRegionThreadsPerAccount) {
            this.noOfRegionThreadsPerAccount = noOfRegionThreadsPerAccount;
            return this;
        }

        public Builder withNoOfMetricThreadsPerRegion(int noOfMetricThreadsPerRegion) {
            this.noOfMetricThreadsPerRegion = noOfMetricThreadsPerRegion;
            return this;
        }

        public Builder withMetricsTimeRange(MetricsTimeRange metricsTimeRange) {
            this.metricsTimeRange = metricsTimeRange;
            return this;
        }

        public Builder withMetricsProcessor(MetricsProcessor metricsProcessor) {
            this.metricsProcessor = metricsProcessor;
            return this;
        }

        public Builder withMaxErrorRetrySize(int maxErrorRetrySize) {
            this.maxErrorRetrySize = maxErrorRetrySize;
            return this;
        }

        public Builder withCredentialsDecryptionConfig(CredentialsDecryptionConfig credentialsDecryptionConfig) {
            this.credentialsDecryptionConfig = credentialsDecryptionConfig;
            return this;
        }

        public Builder withProxyConfig(ProxyConfig proxyConfig) {
            this.proxyConfig = proxyConfig;
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

        public AccountMetricStatisticsCollector build() {
            return new AccountMetricStatisticsCollector(this);
        }

        public Builder withPrefix(String metricPrefix) {
            this.metricPrefix = metricPrefix;
            return this;
        }

        public Builder withThreadTimeOut(int threadTimeOut) {
            this.threadTimeOut = threadTimeOut;
            return this;
        }

        Builder withDashboardConfig(DashboardConfig dashboardConfig) {
            this.dashboardConfig = dashboardConfig;
            return this;
        }
        Builder withControllerInformation(ControllerInformation controllerInformation) {
            this.controllerInfo = controllerInformation;
            return this;
        }


    }

}
