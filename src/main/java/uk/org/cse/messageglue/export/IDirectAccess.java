package uk.org.cse.messageglue.export;

public interface IDirectAccess {
	public String getAnonymousQueue(final String identifier);
	public void sendMessageToQueue(final String queue, final Object message);
	public void sendMessagesToQueue(final String queue, final Iterable<Object> message);
}
