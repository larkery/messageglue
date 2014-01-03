package uk.org.cse.messageglue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import javax.inject.Provider;

import uk.org.cse.messageglue.Model.Output;
import uk.org.cse.messageglue.export.IErrorHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import uk.org.cse.messageglue.export.IDirectAccess;

/**
 * This is the server which binds a method to the messaging system
 * 
 * @author hinton
 *
 * @param <T>
 */
class MethodServer<MT, IT> extends DefaultConsumer {
	private final IErrorHandler errorHandler;
	private final Model<MT> model;
	private final IT target;
	private final Method method;
	private final ObjectMapper mapper;
	private final Provider<Channel> channels;
	private final IDirectAccess direct;

	private String myTag;
	
	public MethodServer(final IErrorHandler errorHandler,
						final ObjectMapper mapper,
						final Provider<Channel> channels,
						final Method method,
						final IT target,
						final IDirectAccess direct) {
		super(channels.get());
		this.errorHandler = errorHandler;
		this.mapper = mapper;
		this.channels = channels;
		this.method = method;
		this.target = target;
		this.model = Model.of(method);
		this.direct = direct;
	}
	
	public void start() {
		try {
			final String privateQueue;
			if (model.isFromNamedQueue()) {
				privateQueue = model.getNamedQueue();
			} else if (model.isFromSharedAnonymousQueue()) {
				privateQueue = direct.getAnonymousQueue(model.getNamedQueue());
			} else {
				privateQueue = getChannel().queueDeclare().getQueue();
				
				getChannel().queueBind(
						privateQueue, 
						model.getSubmissionExchange(), 
						model.getSubmissionRoutingKey());
			}

			myTag = getChannel().basicConsume(privateQueue, 
											  true,
											  this);

		} catch (final IOException e) {
			
		}
	}
	
	public void stop() {
		if (myTag != null) {
			try {
				getChannel().basicCancel(myTag);
			} catch (final IOException ex) {}
			try {
				getChannel().close();
			} catch (final IOException ex) {}
			myTag = null;
		}
	}
	
	@Override
	public void handleDelivery(
			final String consumerTag, 
			final Envelope envelope,
			final BasicProperties properties, 
			final byte[] body) throws IOException {
		final MT message;
		
		try {
			message = mapper.readValue(body, model.getMessageType());
		} catch (final IOException ex) {
			// log error to logger; we have been sent the wrong thing.
			// possibly we should not ack it either? but then we will keep getting it.
			errorHandler.handleMessagingError(
					"Unable to deserialize payload: " + ex.getMessage(), 
					Optional.of(body), 
					Optional.<Throwable>of(ex));
			return;
		}
			
		try (final RefCountedChannel requestChannel = 
				new RefCountedChannel(channels.get(), 
						1 + model.getAsyncOutputs().size()))
		{
			final Object[] values = new Object[1 + model.getAsyncOutputs().size()];
			
			values[0] = message;
			
			final Sink<?> outputSink;
			if (model.getDirectOutput().isPresent()) {
				final Output<?> out = model.getDirectOutput().get();
				outputSink = createSink(mapper, requestChannel, out, properties.getReplyTo(), 0);
			} else {
				outputSink = null;
			}
			
			int i = 1;
			for (final Output<?> out : model.getAsyncOutputs()) {
				values[i] = createSink(mapper, requestChannel, out, properties.getReplyTo(), i);
				i++;
			}
			
			try {
				final Object result = method.invoke(target, values);
				if (outputSink != null) {
					try {
						if (model.isMultipleReturn()) {
							@SuppressWarnings("unchecked")
							final Collection<Object> col = (Collection<Object>) result;
							for (final Object o : col) {
								outputSink.castingWrite(o);
							}
						} else {
							outputSink.castingWrite(result);
						}
					} catch (final ClassCastException cce) {
						errorHandler.handleMessagingError(
								String.format("%s returned an output which could not be cast to the expected type",method), 
								Optional.of(body), 
								Optional.<Throwable>of(cce)
								);
						throw cce;
					}
				}
			} catch (final ClassCastException |
					IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				
				errorHandler.handleMessagingError(
						String.format("invoking %s produced a fatal error in the request",method), 
						Optional.of(body), 
						Optional.<Throwable>of(e)
						);
				
				outputSink.error("Error handling request", Optional.<Throwable>of(e));
				requestChannel.closeImmediately();
			}
		}
	}

	private static <T> Sink<T> createSink(final ObjectMapper mapper, final Channel requestChannel, final Output<T> out, final String destination, final int index) {
		return new Sink<>(requestChannel, mapper, out.getMessageType(), destination, index);
	}
}
