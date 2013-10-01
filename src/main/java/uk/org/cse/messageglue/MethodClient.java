package uk.org.cse.messageglue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import javax.inject.Provider;

import uk.org.cse.messageglue.export.IAsyncOutput;
import uk.org.cse.messageglue.export.IErrorHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;


class MethodClient<MT> {
	private final ObjectMapper mapper;
	private final Model<MT> model;
	private Provider<Channel> channelProvider;
	private final IErrorHandler errorHandler;

	public MethodClient(
			final IErrorHandler errorHandler,
			final Provider<Channel> channelProvider, 
			final ObjectMapper mapper, final Method method) {
		this.errorHandler = errorHandler;
		this.channelProvider = channelProvider;
		this.mapper = mapper;
		this.model = Model.of(method);
	}
	
	public Object invoke(final MT message, final IAsyncOutput<?>[] asyncs) throws TimeoutException {
		final byte[] bytes;
		
		try {
			bytes = mapper.writeValueAsBytes(message);
		} catch (final JsonProcessingException e) {
			throw new RuntimeException("Attempted to send an unmappable object over the network", e);
		}
		
		try (final RefCountedChannel invocationChannel = 
				new RefCountedChannel(
						channelProvider.get(),
						model.getAsyncOutputs().size()+1)) {
			
			final BasicProperties.Builder properties = new BasicProperties.Builder();
	
			
			if (model.isReplyQueueNeeded()) {
				final DeclareOk declare;
				
				try {
					declare = invocationChannel.queueDeclare();
				} catch (IOException e) {
					// make error come out
					return null;
				}
				
				properties.replyTo(declare.getQueue());
				
				final IAsyncOutput<?>[] outputsAndResult = new IAsyncOutput[model.getAsyncOutputs().size()+1];
				
				int i = 1;
				for (final IAsyncOutput<?> out : asyncs) {
					outputsAndResult[i++] = out;
				}
				
				final MultipleWaitableOutput<Object> waitableOutput = 
						new MultipleWaitableOutput<Object>(model.getMultipleReturnLimit().or(1));
				outputsAndResult[0] = waitableOutput;
				
				final Correlator correlator = 
						new Correlator(
								errorHandler,
								invocationChannel,
								mapper,
								outputsAndResult
						);
				
				invocationChannel.basicConsume(declare.getQueue(), true, correlator);
				
				invocationChannel.basicPublish(
						model.getSubmissionExchange(), 
						model.getSubmissionRoutingKey(), 
						properties.build(), bytes);
				
				if (model.getDirectOutput().isPresent()) {
					try {
						synchronized (waitableOutput) {
							if (model.getTimeout().isPresent()) {
								waitableOutput.wait(model.getTimeout().get());
							} else {								
								waitableOutput.wait();
							}
						}
					} catch (InterruptedException e) {}
					
					if (model.isMultipleReturn()) {
						return ImmutableList.copyOf(waitableOutput.values);
					} else {
						if (waitableOutput.values.isEmpty()) {
							throw new TimeoutException("Timeout exceeded whilst waiting for synchronous output");
						} else {	
							return waitableOutput.values.get(0);
						}
					}
				}
			} else {
				invocationChannel.basicPublish(
						model.getSubmissionExchange(), 
						model.getSubmissionRoutingKey(), 
						properties.build(), bytes);
			}
		} catch (final IOException e) {
			errorHandler.handleMessagingError(
					"Exception closing channel: " + e.getMessage(),
					Optional.<byte[]>absent(),
					Optional.<Throwable>of(e));
		}
		
		return null;
	}
	
	static class MultipleWaitableOutput<T> implements IAsyncOutput<T> {
		private final ArrayList<T> values = new ArrayList<>();
		private final int maximumSize;
		
		public MultipleWaitableOutput(int maximumSize) {
			super();
			this.maximumSize = maximumSize;
		}

		@Override
		public synchronized void close() throws Exception {
			this.notifyAll();
		}

		@Override
		public synchronized void write(T value) {
			values.add(value);
			if (values.size() >= maximumSize) {
				this.notifyAll();
			}
		}

		@Override
		public synchronized void error(final String message, final Optional<Throwable> ex) {
			this.notifyAll();
		}

		@Override
		public boolean isOpen() {
			return false;
		}
		
	}
	
	static class Correlator extends DefaultConsumer {
		private final IAsyncOutput<?>[] outputs;
		private final ObjectMapper mapper;
		private final RefCountedChannel channel;
		private final IErrorHandler errorHandler;
		
		public Correlator(
				final IErrorHandler errorHandler, 
				RefCountedChannel channel, 
				final ObjectMapper mapper, 
				final IAsyncOutput<?>[] outputs) {
			super(channel);
			this.errorHandler = errorHandler;
			this.channel = channel;
			this.mapper = mapper;
			this.outputs = outputs;
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void handleDelivery(
				final String consumerTag, 
				final Envelope envelope,
				final BasicProperties properties, 
				final byte[] body) throws IOException {
			final String correlationID = properties.getCorrelationId();
			try {
				final Msg msg = mapper.readValue(body, Msg.class);
				try {
					final int i = Integer.parseInt(correlationID);
					if (i >= 0 && i< outputs.length) {
						if (msg instanceof Msg.Close) {
							channel.close();
							outputs[i].close();
						} else if (msg instanceof Msg.Error) {
							final Msg.Error e = (Msg.Error) msg;
							outputs[i].error(e.getMessage(), Optional.fromNullable(e.getCause()));
						} else if (msg instanceof Msg.Value) {
							((IAsyncOutput) outputs[i]).write(((Msg.Value) msg).getValue());
						}
					} else {
						throw new RuntimeException(String.format("Message had invalid correlation id %s", correlationID));
					}
				} catch (Exception e) {
					errorHandler.handleMessagingError(
							e.getMessage(),
							Optional.of(body),
							Optional.<Throwable>of(e));
				}
			} catch (final IOException ex) {
				errorHandler.handleMessagingError(
						"Exception unmapping message: " + ex.getMessage(),
						Optional.of(body),
						Optional.<Throwable>of(ex));
				return;
			}
		}
	}
}
