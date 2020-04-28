/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.metric;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @author Florencio Sarmiento
 *
 */
public class AccountMetricStatistics {

	private String accountName;

	private List<RegionMetricStatistics> regionMetricStatisticsList = 
			new CopyOnWriteArrayList<RegionMetricStatistics>();

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public List<RegionMetricStatistics> getRegionMetricStatisticsList() {
		return regionMetricStatisticsList;
	}

	public void add(RegionMetricStatistics regionMetricStatistics) {
		this.regionMetricStatisticsList.add(regionMetricStatistics);
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
