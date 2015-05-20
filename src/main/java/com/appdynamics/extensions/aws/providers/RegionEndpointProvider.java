package com.appdynamics.extensions.aws.providers;

import static com.appdynamics.extensions.aws.util.AWSUtil.resolvePath;
import static com.appdynamics.extensions.yml.YmlReader.*;
import static com.appdynamics.extensions.aws.Constants.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author Florencio Sarmiento
 *
 */
public class RegionEndpointProvider {
	
	private static Logger LOGGER = Logger.getLogger("com.singularity.extensions.aws.RegionEndpointProvider");
	
	private final Map<String, String> regionEndpoints = new ConcurrentHashMap<String, String>();
	
	private static RegionEndpointProvider instance;
	
	private RegionEndpointProvider() {}
	
	public static RegionEndpointProvider getInstance() {
		if (instance == null) {
			instance = new RegionEndpointProvider();
		}
		
		return instance;
	}
	
	@SuppressWarnings("unchecked")
	public void initialise(String configPath) {
		Map<String, String> tmpRegionEndPoints = null;
		
		if (StringUtils.isBlank(configPath)) {
			LOGGER.info("No config path provided for region endpoints, attempting to load from classpath...");
			tmpRegionEndPoints = read(
					this.getClass().getClassLoader().getResourceAsStream(DEFAULT_REGION_ENDPOINTS_PATH), 
					Map.class);
			
		} else {
			String configFilename = resolvePath(configPath);
			
			if (LOGGER.isDebugEnabled()) {
				LOGGER.info("Loading region endpoints from " + configFilename);
			}
			
			tmpRegionEndPoints = readFromFile(configFilename, Map.class);
		}
		
		if (tmpRegionEndPoints != null) {
			synchronized (regionEndpoints) {
				regionEndpoints.clear();
				regionEndpoints.putAll(tmpRegionEndPoints);
			}
			
			LOGGER.info("Region endpoints successfully initialised!");
		}
	}
	
	public String getEndpoint(String region) {
		return regionEndpoints.get(region);
	}

}
