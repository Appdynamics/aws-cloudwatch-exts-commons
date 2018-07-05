package com.appdynamics.extensions.aws.config;

import java.util.Set;

/**
 * @author Satish Muddam
 */
public class Dimension {

    private String name;
    private String displayName;
    private Set<String> values;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Set<String> getValues() {
        return values;
    }

    public void setValues(Set<String> values) {
        this.values = values;
    }
}
