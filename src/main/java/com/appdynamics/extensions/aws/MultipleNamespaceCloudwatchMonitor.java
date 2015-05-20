package com.appdynamics.extensions.aws;

import static com.appdynamics.extensions.aws.Constants.DEFAULT_NO_OF_THREADS;
import static com.appdynamics.extensions.aws.Constants.DEFAULT_THREAD_TIMEOUT;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.appdynamics.extensions.aws.collectors.NamespaceMetricStatisticsCollector;

/**
 * @author Florencio Sarmiento
 *
 * @param <T> Configuration class
 */
public abstract class MultipleNamespaceCloudwatchMonitor<T> extends AWSCloudwatchMonitor<T> {

	public MultipleNamespaceCloudwatchMonitor(Class<T> clazz) {
		super(clazz);
	}

	@Override
	protected Map<String, Double> getStatsForUpload(T config) {
		Map<String, Double> allNamespacesStats = new ConcurrentHashMap<String, Double>();
		
		ExecutorService threadPool = null;

		try {
			threadPool = Executors.newFixedThreadPool(getNoOfNamespaceThreadsToUse(config));
			CompletionService<Map<String, Double>> tasks = 
					createConcurrentAccountTasks(threadPool);
			collectMetrics(tasks,
					getNamespaceMetricStatisticsCollectorList().size(),
					allNamespacesStats);
			
		} finally {
			if (threadPool != null && !threadPool.isShutdown()) {
				threadPool.shutdown();
			}
		}

		return allNamespacesStats;
	}

	private CompletionService<Map<String, Double>> createConcurrentAccountTasks(
			ExecutorService threadPool) {
		CompletionService<Map<String, Double>> namespaceCollectorTasks = new ExecutorCompletionService<Map<String, Double>>(
				threadPool);

		for (NamespaceMetricStatisticsCollector namespaceCollector : getNamespaceMetricStatisticsCollectorList()) {
			namespaceCollectorTasks.submit(namespaceCollector);
		}

		return namespaceCollectorTasks;
	}
	
	private void collectMetrics(CompletionService<Map<String, Double>> parallelTasks,
			int taskSize, Map<String, Double> allNamespacesStats) {
		
		for (int index=0; index<taskSize; index++) {
			try {
				Map<String, Double> namespaceStats = 
						parallelTasks.take().get(DEFAULT_THREAD_TIMEOUT, TimeUnit.SECONDS);
				allNamespacesStats.putAll(namespaceStats);
				
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

	protected abstract List<NamespaceMetricStatisticsCollector> getNamespaceMetricStatisticsCollectorList();

	protected abstract int getNoOfNamespaceThreads(T config);
}
