/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.config;

import java.util.List;
import java.util.Map;

/**
 * @author Florencio Sarmiento
 */
public class Configuration {

    private List<Account> accounts;

    private CredentialsDecryptionConfig credentialsDecryptionConfig;

    private ProxyConfig proxyConfig;

    private MetricsConfig metricsConfig;

    private ConcurrencyConfig concurrencyConfig;

    private Map<String, String> regionEndPoints;

    private String metricPrefix;

    private TaskSchedule taskSchedule;

    private String cloudWatchMonitoring;

    private int cloudWatchMonitoringInterval;

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    public CredentialsDecryptionConfig getCredentialsDecryptionConfig() {
        return credentialsDecryptionConfig;
    }

    public void setCredentialsDecryptionConfig(
            CredentialsDecryptionConfig credentialsDecryptionConfig) {
        this.credentialsDecryptionConfig = credentialsDecryptionConfig;
    }

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
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

    public Map<String, String> getRegionEndPoints() {
        return regionEndPoints;
    }

    public void setRegionEndPoints(Map<String, String> regionEndPoints) {
        this.regionEndPoints = regionEndPoints;
    }

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }

    public TaskSchedule getTaskSchedule() {
        return taskSchedule;
    }

    public void setTaskSchedule(TaskSchedule taskSchedule) {
        this.taskSchedule = taskSchedule;
    }

    public String getCloudWatchMonitoring() {
        return cloudWatchMonitoring;
    }

    public void setCloudWatchMonitoring(String cloudWatchMonitoring) {

        if (CloudWatchMonitoringLevel.BASIC.getLevel().equalsIgnoreCase(cloudWatchMonitoring) ||
                CloudWatchMonitoringLevel.DETAILED.getLevel().equalsIgnoreCase(cloudWatchMonitoring)) {
            this.cloudWatchMonitoring = cloudWatchMonitoring;
        } else {
            this.cloudWatchMonitoring = CloudWatchMonitoringLevel.BASIC.getLevel();
        }
    }

    public int getCloudWatchMonitoringInterval() {
        return cloudWatchMonitoringInterval;
    }

    public void setCloudWatchMonitoringInterval(int cloudWatchMonitoringInterval) {
        this.cloudWatchMonitoringInterval = cloudWatchMonitoringInterval;
    }

    public enum CloudWatchMonitoringLevel {
        BASIC("Basic"), DETAILED("Detailed");
        private String level;

        CloudWatchMonitoringLevel(String level) {
            this.level = level;
        }

        public String getLevel() {
            return level;
        }

    }
}
