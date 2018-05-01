/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.dto;

import com.amazonaws.services.cloudwatch.model.Metric;
import com.appdynamics.extensions.aws.config.IncludeMetric;

/**
 * @author Satish Muddam
 */
public class AWSMetric {

    private IncludeMetric includeMetric;
    private Metric metric;

    public IncludeMetric getIncludeMetric() {
        return includeMetric;
    }

    public void setIncludeMetric(IncludeMetric includeMetric) {
        this.includeMetric = includeMetric;
    }

    public Metric getMetric() {
        return metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }
}
