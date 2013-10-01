package uk.org.cse.messageglue.export;

import com.google.common.base.Optional;

public interface IErrorHandler {
	final IErrorHandler STANDARD_ERROR = new IErrorHandler() {
		
		@Override
		public void handleMessagingError(String message, Optional<byte[]> payload,
				Optional<Throwable> throwable) {
			System.err.println(message);
		}
	};

	public void handleMessagingError(
			final String message,
			final Optional<byte[]> payload,
			final Optional<Throwable> throwable);
}
