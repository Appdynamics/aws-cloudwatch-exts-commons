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
public class ControllerInformation {

    private String controllerHost;
    private Integer controllerPort;
    private String account;
    private String username;
    private String password;
    private String encryptedPassword;
    private String encryptionKey;
    private Boolean simEnabled;
    private String applicationName;
    private String tierName;
    private String nodeName;
    private Boolean controllerSslEnabled;
    protected Boolean enableOrchestration;
    protected String uniqueHostId;
    protected String accountAccessKey;
    protected String machinePath;

    public Boolean getEnableOrchestration() {
        return enableOrchestration;
    }

    public void setEnableOrchestration(Boolean enableOrchestration) {
        this.enableOrchestration = enableOrchestration;
    }

    public String getUniqueHostId() {
        return uniqueHostId;
    }

    public void setUniqueHostId(String uniqueHostId) {
        this.uniqueHostId = uniqueHostId;
    }

    public String getAccountAccessKey() {
        return accountAccessKey;
    }

    public void setAccountAccessKey(String accountAccessKey) {
        this.accountAccessKey = accountAccessKey;
    }

    public String getMachinePath() {
        return machinePath;
    }

    public void setMachinePath(String machinePath) {
        this.machinePath = machinePath;
    }


    public Boolean getControllerSslEnabled() {
        return controllerSslEnabled;
    }

    public void setControllerSslEnabled(Boolean controllerSslEnabled) {
        this.controllerSslEnabled = controllerSslEnabled;
    }

    public String getControllerHost() {
        return controllerHost;
    }

    public void setControllerHost(String controllerHost) {
        this.controllerHost = controllerHost;
    }

    public Integer getControllerPort() {
        return controllerPort;
    }

    public void setControllerPort(Integer controllerPort) {
        this.controllerPort = controllerPort;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public Boolean getSimEnabled() {
        return simEnabled;
    }

    public void setSimEnabled(Boolean simEnabled) {
        this.simEnabled = simEnabled;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

}
