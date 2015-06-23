package com.appdynamics.extensions.aws.collectors;

import static com.appdynamics.extensions.aws.Constants.*;
import static com.appdynamics.extensions.aws.validators.Validator.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.aws.config.Account;
import com.appdynamics.extensions.aws.config.ConcurrencyConfig;
import com.appdynamics.extensions.aws.config.CredentialsDecryptionConfig;
import com.appdynamics.extensions.aws.config.MetricsConfig;
import com.appdynamics.extensions.aws.config.ProxyConfig;
import com.appdynamics.extensions.aws.exceptions.AwsException;
import com.appdynamics.extensions.aws.metric.AccountMetricStatistics;
import com.appdynamics.extensions.aws.metric.NamespaceMetricStatistics;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;

/**
 * Collects statistics (of all specified accounts) for specified namespace
 * 
 * @author Florencio Sarmiento
 *
 */
public class NamespaceMetricStatisticsCollector implements Callable<Map<String, Double>>{
	
	private static Logger LOGGER = Logger.getLogger("com.singularity.extensions.aws.NamespaceMetricStatisticsCollector");
	
	private List<Account> accounts;
	
	private MetricsConfig metricsConfig;
	
	private ConcurrencyConfig concurrencyConfig;
	
	private MetricsProcessor metricsProcessor;
	
	private CredentialsDecryptionConfig credentialsDecryptionConfig;
	
	private ProxyConfig proxyConfig;

	private NamespaceMetricStatisticsCollector(Builder builder) {
		this.accounts = builder.accounts;
		this.concurrencyConfig = builder.concurrencyConfig;
		this.metricsConfig = builder.metricsConfig;
		this.metricsProcessor = builder.metricsProcessor;
		this.credentialsDecryptionConfig = builder.credentialsDecryptionConfig;
		this.proxyConfig = builder.proxyConfig;
	}
	
	/**
	 * Loops through each account for specified namespace and hands
	 * off account metrics retrieval to {@link AccountMetricStatisticsCollector}
	 * 
	 * Uses {@link MetricsProcessor} to convert all stats retrieved
	 * into a {@link Map<String, Double>} format
	 * 
	 * Returns the accumulated metrics statistics for specified namespace
	 */
	public Map<String, Double> call() {
		LOGGER.info(String.format("Collecting statistics for Namespace [%s]",
				 metricsProcessor.getNamespace()));
		
		if (accounts != null && !accounts.isEmpty()) {
			ExecutorService threadPool = null;
			
			try {
				validateNamespace(metricsProcessor.getNamespace());
				
				threadPool = Executors.newFixedThreadPool(getNoOfAccountThreads());
				CompletionService<AccountMetricStatistics> tasks = 
						createConcurrentAccountTasks(threadPool);
				
				NamespaceMetricStatistics namespaceMetrics = new NamespaceMetricStatistics();
				namespaceMetrics.setNamespace(metricsProcessor.getNamespace());
				
				collectMetrics(tasks, accounts.size(), namespaceMetrics);
				return metricsProcessor.createMetricStatsMapForUpload(namespaceMetrics);
				
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
		
		return new HashMap<String, Double>();
	}
	
	private CompletionService<AccountMetricStatistics> createConcurrentAccountTasks(
			ExecutorService threadPool) {
		CompletionService<AccountMetricStatistics> accountTasks =
				new ExecutorCompletionService<AccountMetricStatistics>(threadPool);
		
		for (Account account : accounts) {
			AccountMetricStatisticsCollector accountTask = 
					new AccountMetricStatisticsCollector.Builder()
						.withAccount(account)
						.withMaxErrorRetrySize(metricsConfig.getMaxErrorRetrySize())
						.withMetricsProcessor(metricsProcessor)
						.withMetricsTimeRange(metricsConfig.getMetricsTimeRange())
						.withNoOfMetricThreadsPerRegion(concurrencyConfig.getNoOfMetricThreadsPerRegion())
						.withNoOfRegionThreadsPerAccount(concurrencyConfig.getNoOfRegionThreadsPerAccount())
						.withCredentialsDecryptionConfig(credentialsDecryptionConfig)
						.withProxyConfig(proxyConfig)
						.build();
					
			accountTasks.submit(accountTask);
		}
		
		return accountTasks;
	}
	
	private void collectMetrics(CompletionService<AccountMetricStatistics> parallelTasks,
			int taskSize, NamespaceMetricStatistics namespaceMetricStatistics) {
		
		for (int index=0; index<taskSize; index++) {
			try {
				AccountMetricStatistics accountStats = 
						parallelTasks.take().get(DEFAULT_THREAD_TIMEOUT, TimeUnit.SECONDS);
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

		public Builder(List<Account> accounts,
				ConcurrencyConfig concurrencyConfig,
				MetricsConfig metricsConfig, 
				MetricsProcessor metricsProcessor) {
			this.accounts = accounts;
			this.concurrencyConfig = concurrencyConfig;
			this.metricsConfig = metricsConfig;
			this.metricsProcessor = metricsProcessor;
		}
		
		public Builder withCredentialsEncryptionConfig(CredentialsDecryptionConfig credentialsDecryptionConfig) {
			this.credentialsDecryptionConfig = credentialsDecryptionConfig;
			return this;
		}
		
		public Builder withProxyConfig(ProxyConfig proxyConfig) {
			this.proxyConfig = proxyConfig;
			return this;
		}
		
		public NamespaceMetricStatisticsCollector build() {
			return new NamespaceMetricStatisticsCollector(this);
		}
	}
}
