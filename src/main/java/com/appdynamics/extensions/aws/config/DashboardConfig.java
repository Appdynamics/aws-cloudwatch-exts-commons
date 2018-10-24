/*
 *   Copyright 2018 . AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.aws.config;

/**
 * Created by bhuvnesh.kumar on 8/16/18.
 */
public class DashboardConfig {

    private Boolean enabled;
    private String dashboardName;
    private Integer executionFrequencyMinutes;
    private String pathToSIMDashboard;
    private String pathToNormalDashboard;
    private Integer periodToCheckDashboardPresenceInSeconds;

    public Integer getPeriodToCheckDashboardPresenceInSeconds() {
        return periodToCheckDashboardPresenceInSeconds;
    }

    public void setPeriodToCheckDashboardPresenceInSeconds(Integer periodToCheckDashboardPresenceInSeconds) {
        this.periodToCheckDashboardPresenceInSeconds = periodToCheckDashboardPresenceInSeconds;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getDashboardName() {
        return dashboardName;
    }

    public void setDashboardName(String dashboardName) {
        this.dashboardName = dashboardName;
    }

    public Integer getExecutionFrequencyMinutes() {
        return executionFrequencyMinutes;
    }

    public void setExecutionFrequencyMinutes(Integer executionFrequencyMinutes) {
        this.executionFrequencyMinutes = executionFrequencyMinutes;
    }

    public String getPathToSIMDashboard() {
        return pathToSIMDashboard;
    }

    public void setPathToSIMDashboard(String pathToSIMDashboard) {
        this.pathToSIMDashboard = pathToSIMDashboard;
    }

    public String getPathToNormalDashboard() {
        return pathToNormalDashboard;
    }

    public void setPathToNormalDashboard(String pathToNormalDashboard) {
        this.pathToNormalDashboard = pathToNormalDashboard;
    }


}
