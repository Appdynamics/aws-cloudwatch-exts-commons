package com.appdynamics.extensions.aws.config;

import java.util.List;

/**
 * @author Florencio Sarmiento
 *
 */
public class Configuration {
	
	private List<Account> accounts;
	
	private MetricsConfig metricsConfig;
	
	private ConcurrencyConfig concurrencyConfig;
	
	private String metricPrefix;
	
	public List<Account> getAccounts() {
		return accounts;
	}

	public void setAccounts(List<Account> accounts) {
		this.accounts = accounts;
	}

	public MetricsConfig getMetricsConfig() {
		return metricsConfig;
	}

	public void setMetricsConfig(MetricsConfig metricsConfig) {
		this.metricsConfig = metricsConfig;
	}

	public ConcurrencyConfig getConcurrencyConfig() {
		return concurrencyConfig;
	}

	public void setConcurrencyConfig(ConcurrencyConfig concurrencyConfig) {
		this.concurrencyConfig = concurrencyConfig;
	}

	public String getMetricPrefix() {
		return metricPrefix;
	}

	public void setMetricPrefix(String metricPrefix) {
		this.metricPrefix = metricPrefix;
	}
}
