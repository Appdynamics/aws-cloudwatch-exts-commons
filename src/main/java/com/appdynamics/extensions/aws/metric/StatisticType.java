/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.metric;

import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

import java.util.Collection;

/**
 * @author Florencio Sarmiento
 */
public enum StatisticType {

    AVE("Average"),
    MAX("Maximum"),
    MIN("Minimum"),
    SUM("Sum"),
    SAMPLE_COUNT("SampleCount");

    private static Logger LOGGER = ExtensionsLoggerFactory.getLogger(StatisticType.class);

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

    public Statistic asStatistic () {
        switch (typeName) {
            case "Average": return Statistic.AVERAGE;
            case "Maximum": return Statistic.MAXIMUM;
            case "Minimum": return Statistic.MINIMUM;
            case "Sum": return Statistic.SUM;
            case "SampleCount": return Statistic.SAMPLE_COUNT;
            default: return Statistic.UNKNOWN_TO_SDK_VERSION;
        }
    }
}
