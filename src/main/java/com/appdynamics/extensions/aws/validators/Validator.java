package com.appdynamics.extensions.aws.validators;

import org.apache.commons.lang3.StringUtils;

import com.appdynamics.extensions.aws.config.Account;
import com.appdynamics.extensions.aws.providers.RegionEndpointProvider;

/**
 * @author Florencio Sarmiento
 *
 */
public class Validator {
	
	public static void validateNamespace(String namespace) {
		if (StringUtils.isBlank(namespace)) {
			throw new IllegalArgumentException("You must provide the namespace");
		}
	}

	public static void validateAccount(Account account) {
		/*if (StringUtils.isBlank(account.getAwsAccessKey())) {
			throw new IllegalArgumentException("You must provide the aws access key");
		}
		
		if (StringUtils.isBlank(account.getAwsSecretKey())) {
			throw new IllegalArgumentException("You must provide the aws secret key");
		}
		
		if (StringUtils.isBlank(account.getDisplayAccountName())) {
			throw new IllegalArgumentException(
					String.format("You must provide a display name for account with aws access key [%s]",
							account.getAwsAccessKey()));
		}
		
		if (account.getRegions() == null || account.getRegions().isEmpty()) {
			throw new IllegalArgumentException(
					String.format("You must provide at least one region for Account [%s]",
							account.getDisplayAccountName()));
		}*/
	}
	
	public static void validateRegion(String region, RegionEndpointProvider regionEndpointProvider) {
		if (StringUtils.isBlank(regionEndpointProvider.getEndpoint(region))) {
			throw new IllegalArgumentException(String.format(
					"Invalid region [%s]", region));
		}
	}
	
    public static void validateTimeRange(int startTimeInMinsBeforeNow, int endTimeInMinsBeforeNow) {
    	if (endTimeInMinsBeforeNow > startTimeInMinsBeforeNow) {
    		throw new IllegalArgumentException(String.format(
    				"endTimeInMinsBeforeNow [%s] must not be greater than startTimeInMinsBeforeNow [%s]",
    				endTimeInMinsBeforeNow, startTimeInMinsBeforeNow));
    	}
    }
	
}
