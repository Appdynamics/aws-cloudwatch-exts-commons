package com.appdynamics.extensions.aws.metric;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

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
