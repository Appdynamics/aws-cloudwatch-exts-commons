package com.appdynamics.extensions.aws.metric;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author Florencio Sarmiento
 *
 */
public enum StatisticType {
	
	AVE("Average"),
	MAX("Maximum"),
	MIN("Minimum"),
	SUM("Sum"),
	SAMPLE_COUNT("SampleCount");
	
	private static Logger LOGGER = Logger.getLogger("com.singularity.extensions.aws.StatisticType");
	
	private String typeName;
	
	StatisticType(String typeName) {
		this.typeName = typeName;
	}
	
	public String getTypeName() {
		return typeName;
	}

	public static StatisticType fromString(String name) {
		if (StringUtils.isNotBlank(name)) {
			String trimmedName = name.trim();
			
			for (StatisticType type : StatisticType.values()) {
				if (type.name().equalsIgnoreCase(trimmedName) || 
						type.typeName.equalsIgnoreCase(trimmedName)) {
					return type;
				}
			}
		}
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Invalid aws statistic type provided [%s], defaulting to [%s]", 
					name, AVE.name()));
		}
		
		return AVE;
	}
}
