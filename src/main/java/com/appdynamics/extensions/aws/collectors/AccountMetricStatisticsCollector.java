package com.appdynamics.extensions.aws.collectors;

import static com.appdynamics.extensions.aws.Constants.*;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.appdynamics.extensions.aws.config.Account;
import com.appdynamics.extensions.aws.config.MetricsTimeRange;
import com.appdynamics.extensions.aws.exceptions.AwsException;
import com.appdynamics.extensions.aws.metric.AccountMetricStatistics;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;

/**
 * Collects statistics (of specified regions) for specified account
 * 
 * @author Florencio Sarmiento
 *
 */
public class AccountMetricStatisticsCollector implements Callable<AccountMetricStatistics> {
	
	private static Logger LOGGER = Logger.getLogger("com.singularity.extensions.aws.AccountMetricStatisticsCollector");
	
	private Account account;
	
	private int noOfRegionThreadsPerAccount;
	
	private int noOfMetricThreadsPerRegion;
	
	private MetricsTimeRange metricsTimeRange;
	
	private MetricsProcessor metricsProcessor;
	
	private int maxErrorRetrySize;
	
	private AccountMetricStatisticsCollector(Account account,
			int noOfRegionThreadsPerAccount, 
			int noOfMetricThreadsPerRegion,
			MetricsTimeRange metricsTimeRange, 
			MetricsProcessor metricsProcessor,
			int maxErrorRetrySize) {
		this.account = account;
		this.noOfMetricThreadsPerRegion = noOfMetricThreadsPerRegion;
		this.metricsTimeRange = metricsTimeRange;
		this.metricsProcessor = metricsProcessor;
		
		setNoOfRegionThreadsPerAccount(noOfRegionThreadsPerAccount);
		setMaxErrorRetrySize(maxErrorRetrySize);
	}
	
	/**
	 * Loops through each region for specified account and hands
	 * off region metrics retrieval to {@link RegionMetricStatisticsCollector}
	 * 
	 * Returns the accumulated metrics statistics for specified account
	 */
	public AccountMetricStatistics call() throws Exception {
		AccountMetricStatistics accountStats = null;
		ExecutorService threadPool = null;
			
		try {
			validateAccount();
			
			LOGGER.info(String.format(
					"Collecting AccountMetricStatistics for Namespace [%s] Account [%s]",
					metricsProcessor.getNamespace(), account.getDisplayAccountName()));
			
			accountStats = new AccountMetricStatistics();
			accountStats.setAccountName(account.getDisplayAccountName());

			AWSCredentials awsCredentials = new BasicAWSCredentials(
					account.getAwsAccessKey(), account.getAwsSecretKey());

			ClientConfiguration awsClientConfig = new ClientConfiguration();
			awsClientConfig.setMaxErrorRetry(maxErrorRetrySize);

			threadPool = Executors.newFixedThreadPool(noOfRegionThreadsPerAccount);
			CompletionService<RegionMetricStatistics> tasks = createConcurrentRegionTasks(
					threadPool, account.getRegions(), awsCredentials, awsClientConfig);
			collectMetrics(tasks, account.getRegions().size(), accountStats);

		} catch (Exception e) {
			throw new AwsException(
					String.format(
							"Error getting AccountMetricStatistics for Namespace [%s] Account [%s]",
							metricsProcessor.getNamespace(),
							account.getDisplayAccountName()), e);

		} finally {
			if (threadPool != null && !threadPool.isShutdown()) {
				threadPool.shutdown();
			}
		}
		
		
		return accountStats;
	}
	
	private CompletionService<RegionMetricStatistics> createConcurrentRegionTasks(
			ExecutorService threadPool,
			Set<String> regions, 
			AWSCredentials awsCredentials, 
			ClientConfiguration awsClientConfig) {
		
		CompletionService<RegionMetricStatistics> regionTasks = 
				new ExecutorCompletionService<RegionMetricStatistics>(threadPool);

		for (String region : regions) {
			RegionMetricStatisticsCollector regionTask = 
					new RegionMetricStatisticsCollector.Builder()
						.withAccountName(account.getDisplayAccountName())
						.withAmazonCloudWatchConfig(awsCredentials, awsClientConfig)
						.withMetricsProcessor(metricsProcessor)
						.withMetricsTimeRange(metricsTimeRange)
						.withNoOfMetricThreadsPerRegion(noOfMetricThreadsPerRegion)
						.withRegion(region)
						.build();
			
			regionTasks.submit(regionTask);
		}
		
		return regionTasks;
	}
	
	private void collectMetrics(CompletionService<RegionMetricStatistics> parallelTasks,
			int taskSize, AccountMetricStatistics accountMetricStatistics) {
		
		for (int index=0; index<taskSize; index++) {
			try {
				RegionMetricStatistics regionStats = 
						parallelTasks.take().get(DEFAULT_THREAD_TIMEOUT, TimeUnit.SECONDS);
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
	
	private void validateAccount() {
		if (StringUtils.isBlank(account.getAwsAccessKey())) {
			throw new IllegalArgumentException("You must provide the aws access key");
		}
		
		if (StringUtils.isBlank(account.getAwsSecretKey())) {
			throw new IllegalArgumentException("You must provide the aws secret key");
		}
		
		if (StringUtils.isBlank(account.getDisplayAccountName())) {
			throw new IllegalArgumentException(
					String.format("You must provide a display name for account with aws access key [%s]",
							account.getAwsAccessKey()));
		}
		
		if (account.getRegions() == null || account.getRegions().isEmpty()) {
			throw new IllegalArgumentException(
					String.format("You must provide at least one region for Account [%s]",
							account.getDisplayAccountName()));
		}
	}
	
    /**
     * Builder class to maintain readability when 
     * building {@link AccountMetricStatisticsCollector} due to its params size
     */
	public static class Builder {
		
		private Account account;
		private int noOfRegionThreadsPerAccount;
		private int noOfMetricThreadsPerRegion;
		private MetricsTimeRange metricsTimeRange;
		private MetricsProcessor metricsProcessor;
		private int maxErrorRetrySize;
		
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
		
		public AccountMetricStatisticsCollector build() {
			return new AccountMetricStatisticsCollector(account, 
					noOfRegionThreadsPerAccount, 
					noOfMetricThreadsPerRegion, 
					metricsTimeRange, 
					metricsProcessor, 
					maxErrorRetrySize);
		}
		
	}

}
