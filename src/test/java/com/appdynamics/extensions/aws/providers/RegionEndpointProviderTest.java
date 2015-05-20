package com.appdynamics.extensions.aws.providers;

import static org.junit.Assert.*;

import org.junit.Test;

import com.appdynamics.extensions.aws.providers.RegionEndpointProvider;

public class RegionEndpointProviderTest {

	private RegionEndpointProvider classUnderTest = RegionEndpointProvider.getInstance();
	
	@Test
	public void testLoadFromClasspath() {
		classUnderTest.initialise(null);
		
		String[] expectedRegions = {"ap-southeast-1",
				"ap-southeast-2",
				"ap-northeast-1",
				"eu-central-1",
				"eu-west-1",
				"us-east-1",
				"us-west-1",
				"us-west-2",
				"sa-east-1"};
		
		for (String region : expectedRegions) {
			assertNotNull(classUnderTest.getEndpoint(region));
		}
	}
	
	@Test
	public void testLoadFromSpecifiedPath() {
		classUnderTest.initialise("src/test/resources/conf/test-region-endpoints.yaml");
		
		String[] expectedRegions = {"region1",
				"region2",
				"region3"};
		
		for (String region : expectedRegions) {
			assertNotNull(classUnderTest.getEndpoint(region));
		}
	}
}
