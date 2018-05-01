/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws;

import static com.appdynamics.extensions.aws.Constants.DEFAULT_NO_OF_THREADS;
import static com.appdynamics.extensions.aws.Constants.DEFAULT_THREAD_TIMEOUT;

import com.appdynamics.extensions.aws.collectors.NamespaceMetricStatisticsCollector;
import com.appdynamics.extensions.metrics.Metric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @param <T> Configuration class
 * @author Florencio Sarmiento
 */
public abstract class MultipleNamespaceCloudwatchMonitor<T> extends AWSCloudwatchMonitor<T> {

    public MultipleNamespaceCloudwatchMonitor(Class<T> clazz) {
        super(clazz);
    }

    @Override
    protected List<Metric> getStatsForUpload(T config) {
        List<Metric> allNamespacesStats =
                Collections.synchronizedList(new ArrayList<Metric>());

        ExecutorService threadPool = null;

        try {
            List<NamespaceMetricStatisticsCollector> namespaceMetricsCollectors =
                    getNamespaceMetricStatisticsCollectorList(config);

            threadPool = Executors.newFixedThreadPool(getNoOfNamespaceThreadsToUse(config));

            CompletionService<List<Metric>> tasks =
                    createConcurrentAccountTasks(threadPool, namespaceMetricsCollectors);

            collectMetrics(tasks,
                    namespaceMetricsCollectors.size(),
                    allNamespacesStats);

        } finally {
            if (threadPool != null && !threadPool.isShutdown()) {
                threadPool.shutdown();
            }
        }

        return allNamespacesStats;
    }

    private CompletionService<List<Metric>> createConcurrentAccountTasks(
            ExecutorService threadPool,
            List<NamespaceMetricStatisticsCollector> namespaceMetricsCollectors) {
        CompletionService<List<Metric>> namespaceCollectorTasks =
                new ExecutorCompletionService<List<Metric>>(threadPool);

        for (NamespaceMetricStatisticsCollector namespaceCollector : namespaceMetricsCollectors) {
            namespaceCollectorTasks.submit(namespaceCollector);
        }

        return namespaceCollectorTasks;
    }

    private void collectMetrics(CompletionService<List<Metric>> parallelTasks,
                                int taskSize, List<Metric> allNamespacesStats) {

        for (int index = 0; index < taskSize; index++) {
            try {
                List<Metric> namespaceStats =
                        parallelTasks.take().get(DEFAULT_THREAD_TIMEOUT, TimeUnit.SECONDS);

                allNamespacesStats.addAll(namespaceStats);

            } catch (InterruptedException e) {
                getLogger().error("Task interrupted. ", e);
            } catch (ExecutionException e) {
                getLogger().error("Task execution failed. ", e);
            } catch (TimeoutException e) {
                getLogger().error("Task timed out. ", e);
            }
        }
    }

    private int getNoOfNamespaceThreadsToUse(T config) {
        int noOfThreads = getNoOfNamespaceThreads(config);
        return noOfThreads > 0 ? noOfThreads : DEFAULT_NO_OF_THREADS;
    }

    protected abstract List<NamespaceMetricStatisticsCollector> getNamespaceMetricStatisticsCollectorList(T config);

    protected abstract int getNoOfNamespaceThreads(T config);
}
