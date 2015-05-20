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
