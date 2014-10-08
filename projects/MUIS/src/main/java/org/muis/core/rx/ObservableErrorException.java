package org.muis.core.rx;

public class ObservableErrorException extends RuntimeException {
	public ObservableErrorException(Throwable cause) {
		super(cause);
	}
}
