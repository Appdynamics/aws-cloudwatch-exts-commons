/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.aws.exceptions;

/**
 * @author Florencio Sarmiento
 *
 */
public class AwsException extends RuntimeException {

	private static final long serialVersionUID = 8283646706856441405L;

	public AwsException() {
		super();
	}

	public AwsException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public AwsException(String arg0) {
		super(arg0);
	}

	public AwsException(Throwable arg0) {
		super(arg0);
	}

}
