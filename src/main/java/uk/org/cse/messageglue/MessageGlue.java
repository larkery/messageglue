package uk.org.cse.messageglue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

import uk.org.cse.messageglue.export.Declare;
import uk.org.cse.messageglue.export.Exchange;
import uk.org.cse.messageglue.export.IDirectAccess;
import uk.org.cse.messageglue.export.IErrorHandler;
import uk.org.cse.messageglue.export.IMessageGlue;
import uk.org.cse.messageglue.export.Queue;
import uk.org.cse.messageglue.export.QueueBinding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.Channel;
import java.util.NoSuchElementException;

public class MessageGlue implements IMessageGlue {
	private final IErrorHandler errorHandler;
	private final Provider<Channel> channels;
	private final ObjectMapper mapper;

	private boolean started = false;
	private final Set<MethodServer<?, ?>> servers = new HashSet<>();
	private final Multimap<Class<?>, Object> boundObjects = LinkedHashMultimap.create();
	private final Map<Object, DirectAccess> directAccesses = new HashMap<>();
	
	class DirectAccess implements IDirectAccess {
		private final Map<String, String> names = new HashMap<>();
		
		@Override
		public String getAnonymousQueue(final String identifier) {
			final String result = names.get(identifier);

			if (result == null) throw new NoSuchElementException("No queue defined with identifier " + identifier);

			return result;
		}

		@Override
		public void sendMessageToQueue(final String queue, final Object message) {
			sendMessagesToQueue(queue, ImmutableList.<Object>of(message));
		}
		
		@Override
		public void sendMessagesToQueue(final String queue, final Iterable<Object> message) {
			try {
				final Channel channel = channels.get();
				if (channel != null) {
					for (final Object object : message) {
						channel.basicPublish("", queue, null, mapper.writeValueAsBytes(object));
					}
				} else {
					errorHandler.handleMessagingError("Could not get channel",
													  Optional.<byte[]>absent(),
													  Optional.<Throwable>absent());
				}
			} catch (final Exception e) {
				errorHandler.handleMessagingError("Direct send failed", Optional.<byte[]>absent(), Optional.<Throwable>of(e));
			}
		}

		public void setAnonymousQueueName(final String value, final String queueName) {
			names.put(value, queueName);
		}
	}
	
	public MessageGlue(final IErrorHandler errorHandler, final Provider<Channel> channels, final ObjectMapper mapper) {
		super();
		this.errorHandler = errorHandler;
		this.channels = channels;
		this.mapper = mapper;
	}

	@Override
	public <T> void bind(final Class<T> clazz, final T implementation) {
		final DirectAccess da = new DirectAccess();

		boundObjects.put(clazz, implementation);
		directAccesses.put(implementation, da);
		
		for (final Method method : implementation.getClass().getMethods()) {
			if (method.getReturnType() == Void.TYPE && 
					method.getParameterTypes().length == 1 &&
					method.getParameterTypes()[0].isAssignableFrom(DirectAccess.class)) {
				try {
					method.invoke(implementation, da);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					errorHandler.handleMessagingError("Error invoking " + method, Optional.<byte[]>absent(), Optional.<Throwable>of(e));
					throw new RuntimeException(e);
				}
			}
		}
		
		if (started) {
			serve(clazz, implementation);
		}
	}

	private <T> void serve(final Class<T> clazz, final T implementation) {
		final Set<Method> methods = Model.getModelMethods(clazz);
		final DirectAccess da = directAccesses.get(implementation);

		ensureDeclared(clazz, da);
		
		if (implementation instanceof IStartStoppable) {
			((IStartStoppable) implementation).start();
		}

		for (final Method m : methods) {
			final MethodServer<?, T> server = 
				new MethodServer<Object, T>(errorHandler, mapper, channels, m, implementation, da);

			server.start();
			
			servers.add(server);
		}
	}
	
	private void declare(final Channel channel, final Declare decl, final DirectAccess da) throws IOException {
		for (final Exchange e : decl.exchanges()) {
			try {
				channel.exchangeDeclare(e.value(), e.type().toString().toLowerCase());
			} catch (final IOException ex) {
				errorHandler.handleMessagingError(
						"unable to declare exchange " + e.value(), 
						Optional.<byte[]>absent(), 
						Optional.<Throwable>of(ex));
				throw ex;
			}
		}
		
		for (final Queue q : decl.queues()) {
			final String queueName;
			if (q.anonymous() && da == null) continue;
			
			try {
				if (q.anonymous()) {
					final DeclareOk queueDeclare = channel.queueDeclare();
					queueName = queueDeclare.getQueue();
					da.setAnonymousQueueName(q.value(), queueName);
				} else {
					channel.queueDeclare(q.value(), q.persistent(), q.exclusive(), q.autodelete(), null);
					queueName = q.value();
				}
			} catch (final IOException e) {
				errorHandler.handleMessagingError(
						"unable to declare queue " + q.value(), 
						Optional.<byte[]>absent(), 
						Optional.<Throwable>of(e));
				throw e;
			}
			for (final QueueBinding b : q.bindings()) {
				try {
					channel.queueBind(queueName, b.value(), b.routingKey());
				} catch (final IOException e) {
					errorHandler.handleMessagingError(
							"unable to bind queue " + q.value() + " to " + b.value(), 
							Optional.<byte[]>absent(), 
							Optional.<Throwable>of(e));
					throw e;
				}
			}
		}
	}
	
	private void ensureDeclared(final Class<?> clazz, final DirectAccess da) {
		final Declare decl = clazz.getAnnotation(Declare.class);
		if (decl == null) return;
		
		if (started) {
			final Channel decls = channels.get();
			try {
				declare(decls, decl, da);
			} catch (final IOException e) {
			} finally {
				try {
					decls.close();
				} catch (final IOException e) {
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T implement(final Class<T> clazz) {
		if (!started) {
			throw new IllegalStateException("Please do not use when not started.");
		}
		
		ensureDeclared(clazz, null);
		
		return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] {clazz}, 
				new ClientInvocationHandler(
						errorHandler,
						Model.getModelMethods(clazz),
						channels,
						mapper
						)
				);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void start() {
		if (started) return;
		started = true;
		
		for (final Map.Entry<Class<?>, Object> object : boundObjects.entries()) {
			serve((Class) object.getKey(), object.getValue());
		}
	}
	
	@Override
	public void stop() {
		started = false;
		for (final MethodServer<?, ?> s : servers) {
			s.stop();
		}
		servers.clear();

		for (final Object o : boundObjects.entries()) {
			if (o instanceof IStartStoppable) {
				((IStartStoppable) o).stop();
			}
		}
	}
}
