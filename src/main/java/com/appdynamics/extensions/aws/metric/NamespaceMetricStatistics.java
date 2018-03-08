/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.metric;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Florencio Sarmiento
 *
 */
public class NamespaceMetricStatistics {
	
	private String namespace;
	
	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	private List<AccountMetricStatistics> accountMetricStatisticsList =
			new CopyOnWriteArrayList<AccountMetricStatistics>();

	public List<AccountMetricStatistics> getAccountMetricStatisticsList() {
		return this.accountMetricStatisticsList;
	}
	
	public void add(AccountMetricStatistics accountMetricStatistics) {
		this.accountMetricStatisticsList.add(accountMetricStatistics);
	}

}
