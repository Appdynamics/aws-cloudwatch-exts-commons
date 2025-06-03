/*
 *   Copyright 2018 . AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.aws.predicate;

import static org.mockito.Mockito.when;

import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashSet;
import java.util.List;

/**
 * @author Satish Muddam
 */

@RunWith(PowerMockRunner.class)
public class MultiDimensionPredicateTest {
    @Mock
    private Metric metric;

    @Mock
    private Dimension awsDimension;

    @Test
    public void testNullIncludeDimensionNamesShouldReturnFalse() {
        com.appdynamics.extensions.aws.config.Dimension dimension = new com.appdynamics.extensions.aws.config.Dimension();
        dimension.setName("LoadBalancerName");
        dimension.setValues(null);

        List<com.appdynamics.extensions.aws.config.Dimension> dimensions = Lists.newArrayList(dimension);

        MultiDimensionPredicate classUnderTest = new MultiDimensionPredicate(dimensions);
        when(metric.dimensions()).thenReturn(Lists.newArrayList(awsDimension));
        when(awsDimension.name()).thenReturn("LoadBalancerName");
        when(awsDimension.value()).thenReturn("Dimension");
        Assert.assertFalse(classUnderTest.apply(metric));
    }

    @Test
    public void testEmptyIncludeDimensionNamesShouldReturnFalse() {

        com.appdynamics.extensions.aws.config.Dimension dimension = new com.appdynamics.extensions.aws.config.Dimension();
        dimension.setName("LoadBalancerName");
        dimension.setValues(new HashSet<>());

        List<com.appdynamics.extensions.aws.config.Dimension> dimensions = Lists.newArrayList(dimension);

        MultiDimensionPredicate classUnderTest = new MultiDimensionPredicate(dimensions);
        when(metric.dimensions()).thenReturn(Lists.newArrayList(awsDimension));
        when(awsDimension.name()).thenReturn("LoadBalancerName");
        when(awsDimension.value()).thenReturn("Dimension");
        Assert.assertFalse(classUnderTest.apply(metric));
    }

    @Test
    public void testWildCardIncludeDimensionNamesShouldReturnTrue() {

        com.appdynamics.extensions.aws.config.Dimension dimension = new com.appdynamics.extensions.aws.config.Dimension();
        dimension.setName("LoadBalancerName");
        dimension.setValues(Sets.newHashSet(".*"));

        List<com.appdynamics.extensions.aws.config.Dimension> dimensions = Lists.newArrayList(dimension);


        MultiDimensionPredicate classUnderTest = new MultiDimensionPredicate(dimensions);
        when(metric.dimensions()).thenReturn(Lists.newArrayList(awsDimension));
        when(awsDimension.name()).thenReturn("LoadBalancerName");
        when(awsDimension.value()).thenReturn("Dimension");
        Assert.assertTrue(classUnderTest.apply(metric));

        when(awsDimension.value()).thenReturn("AnotherDimention");
        Assert.assertTrue(classUnderTest.apply(metric));
    }

    @Test
    public void testIncludeDimensionNamesMatching1ShouldReturnTrue() {

        com.appdynamics.extensions.aws.config.Dimension dimension = new com.appdynamics.extensions.aws.config.Dimension();
        dimension.setName("LoadBalancerName");
        dimension.setValues(Sets.newHashSet("^Dimension$"));

        List<com.appdynamics.extensions.aws.config.Dimension> dimensions = Lists.newArrayList(dimension);

        MultiDimensionPredicate classUnderTest = new MultiDimensionPredicate(dimensions);
        when(metric.dimensions()).thenReturn(Lists.newArrayList(awsDimension));
        when(awsDimension.name()).thenReturn("LoadBalancerName");
        when(awsDimension.value()).thenReturn("Dimension");
        Assert.assertTrue(classUnderTest.apply(metric));

        when(awsDimension.value()).thenReturn("AnotherDimention");
        Assert.assertFalse(classUnderTest.apply(metric));
    }

    @Test
    public void testIncludeDimensionNamesMatching2ShouldReturnTrue() {

        com.appdynamics.extensions.aws.config.Dimension dimension = new com.appdynamics.extensions.aws.config.Dimension();
        dimension.setName("LoadBalancerName");
        dimension.setValues(Sets.newHashSet("Dimension", "test"));

        List<com.appdynamics.extensions.aws.config.Dimension> dimensions = Lists.newArrayList(dimension);

        MultiDimensionPredicate classUnderTest = new MultiDimensionPredicate(dimensions);
        when(metric.dimensions()).thenReturn(Lists.newArrayList(awsDimension));
        when(awsDimension.name()).thenReturn("LoadBalancerName");

        when(awsDimension.value()).thenReturn("Dimension");
        Assert.assertTrue(classUnderTest.apply(metric));

        when(awsDimension.value()).thenReturn("test");
        Assert.assertTrue(classUnderTest.apply(metric));
    }

    @Test
    public void testIncludeDimensionNamesContainsShouldReturnTrue() {

        com.appdynamics.extensions.aws.config.Dimension dimension = new com.appdynamics.extensions.aws.config.Dimension();
        dimension.setName("LoadBalancerName");
        dimension.setValues(Sets.newHashSet("Dim.*"));

        List<com.appdynamics.extensions.aws.config.Dimension> dimensions = Lists.newArrayList(dimension);

        MultiDimensionPredicate classUnderTest = new MultiDimensionPredicate(dimensions);
        when(metric.dimensions()).thenReturn(Lists.newArrayList(awsDimension));
        when(awsDimension.name()).thenReturn("LoadBalancerName");

        when(awsDimension.value()).thenReturn("Dimensions");
        Assert.assertTrue(classUnderTest.apply(metric));
    }


    @Test
    public void testIncludeTableNamesNotMatchingShouldReturnFalse() {

        com.appdynamics.extensions.aws.config.Dimension dimension = new com.appdynamics.extensions.aws.config.Dimension();
        dimension.setName("LoadBalancerName");
        dimension.setValues(Sets.newHashSet("Dimension", "test"));

        List<com.appdynamics.extensions.aws.config.Dimension> dimensions = Lists.newArrayList(dimension);

        MultiDimensionPredicate classUnderTest = new MultiDimensionPredicate(dimensions);
        when(metric.dimensions()).thenReturn(Lists.newArrayList(awsDimension));
        when(awsDimension.name()).thenReturn("LoadBalancerName");
        when(awsDimension.value()).thenReturn("Hammer");
        Assert.assertFalse(classUnderTest.apply(metric));
    }

    @Test
    public void testIncludeDimensionsMatchPattern() {

        com.appdynamics.extensions.aws.config.Dimension dimension1 = new com.appdynamics.extensions.aws.config.Dimension();
        dimension1.setName("LoadBalancerName");
        dimension1.setValues(Sets.newHashSet(".*"));

        com.appdynamics.extensions.aws.config.Dimension dimension2 = new com.appdynamics.extensions.aws.config.Dimension();
        dimension2.setName("TargetGroup");
        dimension2.setValues(Sets.newHashSet("targetgroup/toolsappdynamicscom.*"));

        List<com.appdynamics.extensions.aws.config.Dimension> dimensions = Lists.newArrayList(dimension1, dimension2);

        MultiDimensionPredicate classUnderTest = new MultiDimensionPredicate(dimensions);

        when(metric.dimensions()).thenReturn(Lists.newArrayList(awsDimension));
        when(awsDimension.name()).thenReturn("TargetGroup");
        when(awsDimension.value()).thenReturn("targetgroup/toolsappdynamicscom/abcd");
        Assert.assertTrue(classUnderTest.apply(metric));

        when(metric.dimensions()).thenReturn(Lists.newArrayList(awsDimension));
        when(awsDimension.name()).thenReturn("TargetGroup");
        when(awsDimension.value()).thenReturn("targetgroup/testappd/abcd");
        Assert.assertFalse(classUnderTest.apply(metric));
    }

}
