package com.appdynamics.extensions.aws.config;

import java.util.Set;

/**
 * @author Florencio Sarmiento
 *
 */
public class Account {
	
	private String awsAccessKey;
	
	private String awsSecretKey;
	
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

}
