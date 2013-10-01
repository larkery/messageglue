package uk.org.cse.messageglue;

import java.io.IOException;

import uk.org.cse.messageglue.export.IAsyncOutput;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;

/**
 * A box you can put messages into, which will send them to a particular place.
 * 
 * @author hinton
 *
 * @param <T>
 */
public class Sink<T> implements IAsyncOutput<T> {
	private final Channel channel;
	private final ObjectMapper mapper;
	private final String routingKey;
	private final Class<T> clazz;
	private BasicProperties properties;
	
	public Sink(Channel channel, ObjectMapper mapper, final Class<T> clazz, String routingKey, final int correlationID) {
		this.channel = channel;
		this.mapper = mapper;
		this.clazz = clazz;
		this.routingKey = routingKey;
		
		properties = new BasicProperties.Builder().correlationId("" + correlationID).build();
	}

	@Override
	public void close() throws IOException {
		try {
			write(new Msg.Close());
		} catch (Exception e) {}
		try {
			channel.close();
		} catch (Exception e) {}
	}

	@Override
	public void write(T value) {
		try {
			write(new Msg.Value<T>(value));
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException(
					"Values to be transmitted over the messaging system must be serializable",
					e);
		} catch (IOException e) {
			throw new RuntimeException("IO error writing " + value, e);
		}
	}
	
	private void dieIfClosed() {
		if (!isOpen()) {
			throw new IllegalStateException("Attempted to write to a closed output");
		}
	}

	public void castingWrite(final Object value) {
		write(clazz.cast(value));
	}

	@Override
	public void error(final String message, final Optional<Throwable> throwable) {
		try {
			write(new Msg.Error(message, throwable.orNull()));
		} catch (JsonProcessingException e) {
			if (throwable.isPresent()) {
				error(message + " (exception could not be serialized)", Optional.<Throwable>absent());
			} else {
				throw new RuntimeException("Unexpectedly, the error symbol could not be serialized; this should not happen");
			}
		} catch (IOException e) {
			throw new RuntimeException("IO error writing error symbol", e);
		}
	}

	private void write(final Msg o) throws JsonProcessingException, IOException {
		dieIfClosed();
		
		channel.basicPublish("", routingKey, properties, mapper.writeValueAsBytes(o));
	}
	
	@Override
	public boolean isOpen() {
		return channel.isOpen();
	}
}
