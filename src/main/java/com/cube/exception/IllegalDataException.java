package com.cube.exception;

public class IllegalDataException extends Exception {

	private static final long serialVersionUID = -9222480292500296268L;

	public IllegalDataException() {
		super();
	}

	public IllegalDataException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public IllegalDataException(String message, Throwable cause) {
		super(message, cause);
	}

	public IllegalDataException(String message) {
		super(message);
	}

	public IllegalDataException(Throwable cause) {
		super(cause);
	}

	
	
}
