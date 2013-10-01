package uk.org.cse.messageglue.export;

import com.google.common.base.Optional;

/**
 * An {@link IAsyncOutput} is a deferred output handle; it will be connected
 * over the network when used as a parameter to a {@link UseExchange} or {@link UseQueue} marked
 * method.
 * 
 * @author hinton
 *
 * @param <T>
 */
public interface IAsyncOutput<T> extends AutoCloseable {
	/**
	 * @param value transmit a reply back over the network
	 */
	public void write(T value);
	/**
	 * @param notify over the network (if it is still there) that an error has
	 * 		  occurred. This is more intended for system errors; user errors
	 * 		  could perhaps be handled by having a suitable subtype of T.
	 */
	public void error(final String message, final Optional<Throwable> throwable);
	/**
	 * Is the channel open?
	 */
	public boolean isOpen();
}
