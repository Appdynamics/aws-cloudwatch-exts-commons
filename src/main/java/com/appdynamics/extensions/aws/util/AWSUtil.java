package com.appdynamics.extensions.aws.util;

import java.io.File;

import org.apache.commons.lang.StringUtils;

import com.appdynamics.extensions.PathResolver;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;

/**
 * @author Florencio Sarmiento
 *
 */
public class AWSUtil {
    
    public static Long convertToLong(Double metricValue) {
    	Long value = null;
    	
    	if (metricValue == null || metricValue < 0) {
    		value = 0L;
    		
    	} else {
    		value = Math.round(metricValue);
    	}
    	
    	return value;
    }
    
    public static String resolvePath(String filename) {
        if(StringUtils.isBlank(filename)){
            return "";
        }
        
        //for absolute paths
        if(new File(filename).exists()){
            return filename;
        }
        
        //for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = String.format("%s%s%s", jarPath, File.separator, filename);
        return configFileName;
    }

}
