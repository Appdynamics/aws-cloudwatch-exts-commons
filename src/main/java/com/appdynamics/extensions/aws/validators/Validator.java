package com.appdynamics.extensions.aws.validators;

import org.apache.commons.lang.StringUtils;

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
