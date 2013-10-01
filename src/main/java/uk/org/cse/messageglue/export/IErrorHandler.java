package uk.org.cse.messageglue.export;

import com.google.common.base.Optional;

/**
 * A callback for errors; when an error happens off the main thread,
 * it should end up getting passed to one of these by your {@link IMessageGlue}
 *
 */
public interface IErrorHandler {
	final IErrorHandler STANDARD_ERROR = new IErrorHandler() {
		
		@Override
		public void handleMessagingError(String message, Optional<byte[]> payload,
				Optional<Throwable> throwable) {
			System.err.println(message);
		}
	};

	/**
	 * An error has occurred! Panic!
	 * 
	 * @param message a hopefully-useful message
	 * @param payload if this happened somewhere near a message receipt, here are the bytes of the message
	 * @param throwable if an exception caused this, here it is
	 */
	public void handleMessagingError(
			final String message,
			final Optional<byte[]> payload,
			final Optional<Throwable> throwable);
}
