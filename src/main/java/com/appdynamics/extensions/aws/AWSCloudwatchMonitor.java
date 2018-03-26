/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.aws.config.Configuration;
import com.appdynamics.extensions.aws.config.TaskSchedule;
import com.appdynamics.extensions.aws.providers.RegionEndpointProvider;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.conf.modules.JobScheduleModule;
import com.appdynamics.extensions.metrics.Metric;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @param <T> Configuration class
 * @author Florencio Sarmiento
 */
public abstract class AWSCloudwatchMonitor<T> extends ABaseMonitor {

    private Class<T> clazz;
    private T config;

    public AWSCloudwatchMonitor(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    protected void onConfigReload(File file) {
        Yaml yaml = new Yaml();
        try {
            config = yaml.loadAs(new FileInputStream(file), clazz);
        } catch (FileNotFoundException e) {
            getLogger().error("Error wile reading the config file", e);
        }

        initialize(config);
    }

    @Override
    protected void initializeMoreStuff(Map<String, String> args, MonitorConfiguration conf) {
        Configuration configuration = (Configuration) config;

        TaskSchedule taskSchedule = configuration.getTaskSchedule();

        //Make sure taskSchedule is not defined in the config file
        if (taskSchedule != null) {
            getLogger().error("Please do not define taskSchedule in config file. Extension creates taskSchedule based on the CloudWatch monitor level.");
            throw new IllegalArgumentException("Please do not define taskSchedule in config file. Extension creates taskSchedule based on the CloudWatch monitor level.");
        }

        String cloudWatchMonitoring = configuration.getCloudWatchMonitoring();
        int taskDelaySeconds = -1;

        if (Configuration.CloudWatchMonitoringLevel.BASIC.getLevel().equalsIgnoreCase(cloudWatchMonitoring)) {
            taskDelaySeconds = 300; //Every 5 minutes
        } else if (Configuration.CloudWatchMonitoringLevel.DETAILED.getLevel().equalsIgnoreCase(cloudWatchMonitoring)) {
            taskDelaySeconds = 60; //Every minute
        }

        if (configuration.getCloudWatchMonitoringInterval() > 0) {
            taskDelaySeconds = configuration.getCloudWatchMonitoringInterval() * 60;
        }

        Map<String, Map<String, Integer>> dynamicConfig = new HashMap<>();

        Map<String, Integer> taskScheduleMap = new HashMap<>();
        taskScheduleMap.put("numberOfThreads", 1);
        taskScheduleMap.put("taskDelaySeconds", taskDelaySeconds);

        dynamicConfig.put("taskSchedule", taskScheduleMap);

        JobScheduleModule jobScheduleModule = new JobScheduleModule();

        Field jobScheduleModuleField = null;
        try {
            jobScheduleModuleField = conf.getClass().getDeclaredField("jobScheduleModule");
            jobScheduleModuleField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            getLogger().error("Not able to get the jobScheduleModule using reflection");
        }

        if (jobScheduleModuleField != null) {
            try {
                jobScheduleModuleField.set(conf, jobScheduleModule);
                jobScheduleModule.initScheduledJob(dynamicConfig, monitorName, monitorJob);
            } catch (IllegalAccessException e) {
                getLogger().error("Not able to set the jobScheduleModule using reflection");
            }
        }

    }

    @Override
    protected void doRun(TasksExecutionServiceProvider serviceProvider) {
        getLogger().info("Starting AWS Cloudwatch Monitoring task");

        try {
            List<Metric> statsForUpload = getStatsForUpload(config);

            serviceProvider.getMetricWriteHelper().transformAndPrintMetrics(statsForUpload);

        } catch (Exception ex) {
            getLogger().error("Unfortunately an issue has occurred: ", ex);
        }
    }

    protected void initialiseRegionServiceProviders(T config) {
        Configuration thisConfig = (Configuration) config;
        RegionEndpointProvider regionEndpointProvider = RegionEndpointProvider.getInstance();
        regionEndpointProvider.initialise(thisConfig.getRegionEndPoints());
    }

    protected void initialize(T config) {
        initialiseRegionServiceProviders(config);
    }

    protected abstract List<Metric> getStatsForUpload(T config);

    protected abstract Logger getLogger();
}
