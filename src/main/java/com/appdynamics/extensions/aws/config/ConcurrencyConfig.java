package com.appdynamics.extensions.aws.config;

/**
 * @author Florencio Sarmiento
 *
 */
public class ConcurrencyConfig {
	
	private int noOfAccountThreads;

	private int noOfRegionThreadsPerAccount;

	private int noOfMetricThreadsPerRegion;

	public int getNoOfAccountThreads() {
		return noOfAccountThreads;
	}

	public void setNoOfAccountThreads(int noOfAccountThreads) {
		this.noOfAccountThreads = noOfAccountThreads;
	}

	public int getNoOfRegionThreadsPerAccount() {
		return noOfRegionThreadsPerAccount;
	}

	public void setNoOfRegionThreadsPerAccount(int noOfRegionThreadsPerAccount) {
		this.noOfRegionThreadsPerAccount = noOfRegionThreadsPerAccount;
	}

	public int getNoOfMetricThreadsPerRegion() {
		return noOfMetricThreadsPerRegion;
	}

	public void setNoOfMetricThreadsPerRegion(int noOfMetricThreadsPerRegion) {
		this.noOfMetricThreadsPerRegion = noOfMetricThreadsPerRegion;
	}

}
