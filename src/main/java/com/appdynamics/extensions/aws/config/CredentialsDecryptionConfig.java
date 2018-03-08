/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.config;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author Florencio Sarmiento
 *
 */
public class CredentialsDecryptionConfig {

	private String enableDecryption;

	private String decryptionKey;

	public boolean isDecryptionEnabled() {
		return Boolean.valueOf(getEnableDecryption());
	}
	
	public String getEnableDecryption() {
		return enableDecryption;
	}

	public void setEnableDecryption(String enableDecryption) {
		this.enableDecryption = enableDecryption;
	}

	public String getDecryptionKey() {
		return decryptionKey;
	}

	public void setDecryptionKey(String decryptionKey) {
		this.decryptionKey = decryptionKey;
	}
	
	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
