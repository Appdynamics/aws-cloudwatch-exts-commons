package com.appdynamics.extensions.aws.config;

/**
 * @author Florencio Sarmiento
 *
 */
public class MetricsTimeRange {
	
	private int startTimeInMinsBeforeNow;
	
	private int endTimeInMinsBeforeNow;

	public int getStartTimeInMinsBeforeNow() {
		return startTimeInMinsBeforeNow;
	}

	public void setStartTimeInMinsBeforeNow(int startTimeInMinsBeforeNow) {
		this.startTimeInMinsBeforeNow = startTimeInMinsBeforeNow;
	}

	public int getEndTimeInMinsBeforeNow() {
		return endTimeInMinsBeforeNow;
	}

	public void setEndTimeInMinsBeforeNow(int endTimeInMinsBeforeNow) {
		this.endTimeInMinsBeforeNow = endTimeInMinsBeforeNow;
	}

}
