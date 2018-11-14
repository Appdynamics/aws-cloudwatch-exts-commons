package com.appdynamics.extensions.aws.config;

import java.util.Set;

/**
 * @author: {Vishaka Sekar} on {11/7/18}
 */
public class Tags {

    private String tagName;

    private Set<String> tagValue;




    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public Set<String> getTagValue() {
        return tagValue;
    }

    public void setTagValue(Set<String> tagValue) {
        this.tagValue = tagValue;
    }

}
