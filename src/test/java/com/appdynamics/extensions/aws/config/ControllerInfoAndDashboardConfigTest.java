/*
 *   Copyright 2018 . AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.aws.config;

import org.junit.Assert;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by bhuvnesh.kumar on 9/27/18.
 */
public class ControllerInfoAndDashboardConfigTest {

    // this test verifies that a valid Configuration object is created when using the config with sections of controllerInfo and customDashboard present

    @Test
    public void ConfigAndDashboardPresentTest() throws IOException {

        Yaml yaml = new Yaml();
        File file = new File("src/test/resources/conf/test-controllerInfo-dashboard-config.yml");
        Configuration config = yaml.loadAs(new FileInputStream(file), Configuration.class);

        Assert.assertTrue(config.getCustomDashboard().getEnabled() == true);
        Assert.assertTrue(config.getCustomDashboard().getDashboardName().equals("AWS ELB Monitor Dashboard"));
        Assert.assertTrue(config.getCustomDashboard().getPathToNormalDashboard().equals("monitors/AWSELBMonitor/normalDashboard.json"));
        Assert.assertTrue(config.getCustomDashboard().getPathToSIMDashboard().equals("monitors/AWSELBMonitor/simDashboard.json"));
        Assert.assertTrue(config.getCustomDashboard().getPeriodicDashboardCheckInSeconds().equals(300));

        Assert.assertTrue(config.getControllerInfo().getAccount().equals("account"));
        Assert.assertTrue(config.getControllerInfo().getUsername().equals("admin"));
        Assert.assertTrue(config.getControllerInfo().getPassword().equals("root"));
        Assert.assertTrue(config.getControllerInfo().getControllerHost().equals("host"));
        Assert.assertTrue(config.getControllerInfo().getControllerPort().equals(9000));

    }

}
