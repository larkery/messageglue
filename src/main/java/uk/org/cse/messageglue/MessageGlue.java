package uk.org.cse.messageglue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

import uk.org.cse.messageglue.export.Declare;
import uk.org.cse.messageglue.export.Exchange;
import uk.org.cse.messageglue.export.IErrorHandler;
import uk.org.cse.messageglue.export.IMessageGlue;
import uk.org.cse.messageglue.export.Queue;
import uk.org.cse.messageglue.export.QueueBinding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.rabbitmq.client.Channel;

public class MessageGlue implements IMessageGlue {
	private final IErrorHandler errorHandler;
	private final Provider<Channel> channels;
	private final ObjectMapper mapper;

	private boolean started = false;
	private final Set<MethodServer<?, ?>> servers = new HashSet<>();
	private final Multimap<Class<?>, Object> boundObjects = LinkedHashMultimap.create();
	private final Set<Declare> declarations = new HashSet<>();

	public MessageGlue(final IErrorHandler errorHandler, Provider<Channel> channels, ObjectMapper mapper) {
		super();
		this.errorHandler = errorHandler;
		this.channels = channels;
		this.mapper = mapper;
	}

	@Override
	public <T> void bind(final Class<T> clazz, final T implementation) {
		boundObjects.put(clazz, implementation);
		if (started) {
			serve(clazz, implementation);
		}
	}

	private <T> void serve(final Class<T> clazz, final T impl) {
		final Set<Method> methods = Model.getModelMethods(clazz);
		
		ensureDeclared(clazz);
		
		for (final Method m : methods) {
			final MethodServer<?, T> server = new MethodServer<Object, T>(
					errorHandler, mapper, channels, m, impl);
			server.start();
			servers.add(server);
		}
	}
	
	private void declare(final Channel channel, final Declare decl) throws IOException {
		for (final Exchange e : decl.exchanges()) {
			try {
				channel.exchangeDeclare(e.value(), e.type().toString().toLowerCase());
			} catch (IOException ex) {
				errorHandler.handleMessagingError(
						"unable to declare exchange " + e.value(), 
						Optional.<byte[]>absent(), 
						Optional.<Throwable>of(ex));
				throw ex;
			}
		}
		
		for (final Queue q : decl.queues()) {
			try {
				channel.queueDeclare(q.value(), q.persistent(), q.exclusive(), q.autodelete(), null);
			} catch (IOException e) {
				errorHandler.handleMessagingError(
						"unable to declare queue " + q.value(), 
						Optional.<byte[]>absent(), 
						Optional.<Throwable>of(e));
				throw e;
			}
			for (final QueueBinding b : q.bindings()) {
				try {
					channel.queueBind(q.value(), b.value(), b.routingKey());
				} catch (IOException e) {
					errorHandler.handleMessagingError(
							"unable to bind queue " + q.value() + " to " + b.value(), 
							Optional.<byte[]>absent(), 
							Optional.<Throwable>of(e));
					throw e;
				}
			}
		}
	}
	
	private void ensureDeclared(final Class<?> clazz) {
		final Declare decl = clazz.getAnnotation(Declare.class);
		if (decl == null) return;
		if (declarations.contains(decl)) return;
		declarations.add(decl);
		if (started) {
			final Channel decls = channels.get();
			try {
				declare(decls, decl);
			} catch (IOException e) {
			} finally {
				try {
					decls.close();
				} catch (IOException e) {
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
		
		ensureDeclared(clazz);
		
		return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] {clazz}, 
				new ClientInvocationHandler(
						errorHandler,
						Model.getModelMethods(clazz),
						channels,
						mapper
						)
				);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void start() {
		if (started) return;
		started = true;
		
		final Channel decls = channels.get();

		try {
			for (final Declare decl : declarations) {
				declare(decls, decl);
			}
		} catch (IOException e) {
			
		} finally {
			try {
				decls.close();
			} catch (IOException e) {
			}
		}
		
		for (final Map.Entry<Class<?>, Object> object : boundObjects.entries()) {
			serve((Class) object.getKey(), object.getValue());
		}
	}
	
	public void stop() {
		started = false;
		for (final MethodServer<?, ?> s : servers) {
			s.stop();
		}
		servers.clear();
	}
}
