/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.config;

/**
 * @author Florencio Sarmiento
 */
public class ConcurrencyConfig {

    private int noOfAccountThreads;

    private int noOfRegionThreadsPerAccount;

    private int noOfMetricThreadsPerRegion;

    private int threadTimeOut = 30;

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

    public int getThreadTimeOut() {
        return threadTimeOut;
    }

    public void setThreadTimeOut(int threadTimeOut) {
        //Set only if greater than the default
        if (threadTimeOut > this.threadTimeOut) {
            this.threadTimeOut = threadTimeOut;
        }
    }
}
