package com.appdynamics.extensions.aws;

public class Constants {
	
	public static final int DEFAULT_START_TIME_IN_MINS_BEFORE_NOW = 5;
	
	public static final int DEFAULT_END_TIME_IN_MINS_BEFORE_NOW = 0;
	
	public static final int DEFAULT_NO_OF_THREADS = 3;
	
	public static final int DEFAULT_THREAD_TIMEOUT = 30;
	
	public static final int DEFAULT_METRIC_PERIOD_IN_SEC = 60;

	public static final int DEFAULT_MAX_ERROR_RETRY = 0;
	
	public static final String METRIC_PATH_SEPARATOR = "|";
	
	public static final String CONFIG_ARG = "config-file";
	
	public static final String CONFIG_REGION_ENDPOINTS_ARG = "region-endpoints-config";
	
	public static final String DEFAULT_REGION_ENDPOINTS_PATH = "conf/region-endpoints.yaml";
}
