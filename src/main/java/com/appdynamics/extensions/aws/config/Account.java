/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.config;

import java.util.Set;

/**
 * @author Florencio Sarmiento
 *
 */
public class Account {
	
	private String awsAccessKey = null;
	
	private String awsSecretKey = null;

	private String awsSessionToken = null;
	
	private String displayAccountName;
	
	private Set<String> regions;
	
	public String getAwsAccessKey() {
		return awsAccessKey;
	}

	public void setAwsAccessKey(String awsAccessKey) {
		this.awsAccessKey = awsAccessKey;
	}

	public String getAwsSecretKey() {
		return awsSecretKey;
	}

	public void setAwsSecretKey(String awsSecretKey) {
		this.awsSecretKey = awsSecretKey;
	}

	public String getDisplayAccountName() {
		return displayAccountName;
	}

	public void setDisplayAccountName(String displayAccountName) {
		this.displayAccountName = displayAccountName;
	}

	public Set<String> getRegions() {
		return regions;
	}

	public void setRegions(Set<String> regions) {
		this.regions = regions;
	}

	public String getAwsSessionToken () {
		return awsSessionToken;
	}

	public void setAwsSessionToken (String awsSessionToken) {
		this.awsSessionToken = awsSessionToken;
	}
}
