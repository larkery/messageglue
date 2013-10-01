package uk.org.cse.messageglue.export;

public interface IMessageGlue {
	/**
	 * Bind the given class to the provided implementation type;
	 * this will effectively serve methods from that class onto the message bus.
	 * 
	 * @param clazz
	 * @param implementation
	 */
	public <T> void bind(
			final Class<T> clazz,
			final T implementation);
	
	/**
	 * Implement the given class; this effectively provides access to instances
	 * bound elsewhere using {@link #bind(Class, Object)}.
	 * @param clazz
	 * @return
	 */
	public <T> T implement(final Class<T> clazz);

	/**
	 * Start serving all bound instances
	 */
	public void start();

	/**
	 * Stop serving anything
	 */
	public void stop();
}
